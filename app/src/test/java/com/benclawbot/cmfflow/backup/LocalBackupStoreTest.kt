package com.benclawbot.cmfflow.backup

import androidx.room.Room
import com.benclawbot.cmfflow.data.FlowDatabase
import com.benclawbot.cmfflow.data.TaskEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalBackupStoreTest {
    private lateinit var database: FlowDatabase
    private lateinit var store: LocalBackupStore
    private val password = "an unusually long recovery phrase".toCharArray()

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FlowDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = LocalBackupStore(database)
    }

    @After fun cleanup() {
        database.close()
    }

    private fun insertTask(title: String) = runBlocking {
        database.taskDao().insert(
            TaskEntity(
                title = title,
                domain = "work",
                valueScore = 3,
                urgencyScore = 3,
                difficultyScore = 2,
                estimatedMinutes = 25,
            ),
        )
    }

    private fun taskTitles(): List<String> {
        val titles = mutableListOf<String>()
        database.openHelper.writableDatabase.query("SELECT title FROM tasks ORDER BY id").use { cursor ->
            while (cursor.moveToNext()) titles.add(cursor.getString(0))
        }
        return titles
    }

    @Test fun exportPreviewAndRestoreRoundTripPreservesData() {
        insertTask("Saved task")
        val encrypted = store.createEncryptedArchive(password)
        assertFalse(String(encrypted, Charsets.UTF_8).contains("Saved task"))
        val preview = store.previewEncryptedArchive(encrypted, password)
        assertEquals(1, preview.recordCounts["tasks"])
        insertTask("Unsaved task")
        assertEquals(listOf("Saved task", "Unsaved task"), taskTitles())
        store.restoreFromPreview(preview)
        assertEquals(listOf("Saved task"), taskTitles())
    }

    @Test fun failedRestoreDoesNotEraseExistingDatabase() {
        insertTask("Archived task")
        val preview = store.previewEncryptedArchive(store.createEncryptedArchive(password), password)
        insertTask("Live task")
        val task = preview.data.getJSONObject("tables").getJSONObject("tasks")
        val columns = task.getJSONArray("columns")
        val titleIndex = (0 until columns.length()).first { columns.getString(it) == "title" }
        task.getJSONArray("rows").getJSONArray(0).put(titleIndex, JSONObject.NULL)
        assertThrows(Exception::class.java) { store.restoreFromPreview(preview) }
        assertEquals(listOf("Archived task", "Live task"), taskTitles())
    }

    @Test fun incompatibleSchemaIsRejectedBeforeAnyMutation() {
        insertTask("Protected data")
        val archive = store.createEncryptedArchive(password)
        val plaintext = EncryptedBackupCodec.decrypt(archive, password)
        val modified = JSONObject(String(plaintext, Charsets.UTF_8)).put("schemaVersion", 9)
            .toString().toByteArray()
        val encrypted = EncryptedBackupCodec.encrypt(modified, password)
        assertThrows(IllegalArgumentException::class.java) {
            store.previewEncryptedArchive(encrypted, password)
        }
        assertEquals(listOf("Protected data"), taskTitles())
    }
}
