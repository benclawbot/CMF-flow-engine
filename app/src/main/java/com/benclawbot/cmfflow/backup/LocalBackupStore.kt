package com.benclawbot.cmfflow.backup

import androidx.sqlite.db.SupportSQLiteDatabase
import com.benclawbot.cmfflow.data.FlowDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Callable

data class BackupPreview(
    val createdAtEpochMs: Long,
    val recordCounts: Map<String, Int>,
    internal val data: JSONObject,
)

/**
 * Explicit, local-only export of all Flow Room tables. Imported snapshots are
 * validated before any write. Room's transaction makes an interrupted import
 * roll back instead of leaving partially restored data.
 */
class LocalBackupStore(private val database: FlowDatabase) {
    private val tables = listOf(
        "self_reports",
        "context_snapshots",
        "tasks",
        "recommendation_events",
        "intervention_events",
        "sessions",
        "experiments",
        "experiment_assignments",
    )
    private val schema = 10
    private val maxRecords = 30_000

    fun createEncryptedArchive(password: CharArray): ByteArray {
        val payload: ByteArray = database.runInTransaction(Callable {
            val sql = database.openHelper.writableDatabase
            require(sql.version == schema) { "Unsupported local database schema" }
            val allTables = JSONObject()
            var recordCount = 0
            for (table in tables) {
                val columns = columnNames(sql, table)
                val columnList = JSONArray()
                columns.forEach { columnList.put(it) }
                val rows = JSONArray()
                sql.query("SELECT * FROM " + table).use { cursor ->
                    while (cursor.moveToNext()) {
                        recordCount++
                        require(recordCount <= maxRecords) { "Backup exceeds record limit" }
                        val row = JSONArray()
                        for (i in columns.indices) {
                            val value: Any = when (cursor.getType(i)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                                android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                                android.database.Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                                else -> throw IllegalArgumentException("Unsupported database value type")
                            }
                            row.put(value)
                        }
                        rows.put(row)
                    }
                }
                allTables.put(table, JSONObject().put("columns", columnList).put("rows", rows))
            }
            JSONObject()
                .put("archiveVersion", 1)
                .put("schemaVersion", schema)
                .put("createdAtEpochMs", System.currentTimeMillis())
                .put("tables", allTables)
                .toString()
                .toByteArray(Charsets.UTF_8)
        })
        return EncryptedBackupCodec.encrypt(payload, password)
    }

    fun previewEncryptedArchive(bytes: ByteArray, password: CharArray): BackupPreview {
        val plaintext = EncryptedBackupCodec.decrypt(bytes, password)
        val json = try {
            JSONObject(String(plaintext, Charsets.UTF_8))
        } finally {
            plaintext.fill(0)
        }
        require(json.getInt("archiveVersion") == 1) { "Unsupported backup format" }
        require(json.getInt("schemaVersion") == schema) {
            "This backup requires a different database schema"
        }
        val allTables = json.getJSONObject("tables")
        require(allTables.length() == tables.size) { "Backup table set is incomplete or unexpected" }
        val counts = LinkedHashMap<String, Int>()
        val sql = database.openHelper.writableDatabase
        require(sql.version == schema) { "Local database schema differs from the backup" }
        var totalRecords = 0
        for (table in tables) {
            require(allTables.has(table)) { "Missing backup table" }
            val objectTable = allTables.getJSONObject(table)
            val suppliedColumns = objectTable.getJSONArray("columns")
            val actualColumns = columnNames(sql, table)
            require(suppliedColumns.length() == actualColumns.size) { "Unexpected backup columns" }
            for (i in actualColumns.indices) {
                require(suppliedColumns.getString(i) == actualColumns[i]) { "Unsupported backup columns" }
            }
            val rows = objectTable.getJSONArray("rows")
            totalRecords += rows.length()
            require(totalRecords <= maxRecords) { "Backup exceeds record limit" }
            for (i in 0 until rows.length()) {
                val row = rows.getJSONArray(i)
                require(row.length() == actualColumns.size) { "Malformed backup row" }
                for (column in 0 until row.length()) {
                    val v = row.get(column)
                    require(v === JSONObject.NULL || v is String || v is Number) {
                        "Invalid backup cell type"
                    }
                }
            }
            counts[table] = rows.length()
        }
        return BackupPreview(json.getLong("createdAtEpochMs"), counts, json)
    }

    fun restoreFromPreview(preview: BackupPreview) {
        // The entire replacement is transactional. Foreign keys remain enabled.
        database.runInTransaction {
            val sql = database.openHelper.writableDatabase
            require(sql.version == schema) { "Local database schema changed" }
            val archiveTables = preview.data.getJSONObject("tables")
            // Validate again at commit time and use only DB-derived column names.
            for (table in tables) {
                val actualColumns = columnNames(sql, table)
                val saved = archiveTables.getJSONObject(table)
                val supplied = saved.getJSONArray("columns")
                require(supplied.length() == actualColumns.size)
                for (i in actualColumns.indices) require(supplied.getString(i) == actualColumns[i])
            }
            for (table in tables.asReversed()) sql.execSQL("DELETE FROM " + table)
            for (table in tables) {
                val columns = columnNames(sql, table)
                val rows = archiveTables.getJSONObject(table).getJSONArray("rows")
                val statement = "INSERT INTO " + table + " (" + columns.joinToString(",") +
                    ") VALUES (" + columns.joinToString(",") { "?" } + ")"
                for (rowIndex in 0 until rows.length()) {
                    val row = rows.getJSONArray(rowIndex)
                    require(row.length() == columns.size) { "Malformed backup row" }
                    val values = Array<Any?>(columns.size) { columnIndex ->
                        row.get(columnIndex).let { if (it === JSONObject.NULL) null else it }
                    }
                    sql.execSQL(statement, values)
                }
            }
            // On a constraint violation the transaction rolls back automatically.
        }
    }

    private fun columnNames(sql: SupportSQLiteDatabase, table: String): List<String> =
        sql.query("SELECT * FROM " + table + " LIMIT 0").use { cursor ->
            cursor.columnNames.toList().also { columns ->
                require(columns.isNotEmpty()) { "Missing database table" }
                require(columns.all { it.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")) }) {
                    "Unsupported database column name"
                }
            }
        }
}
