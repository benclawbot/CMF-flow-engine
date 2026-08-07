package com.benclawbot.cmfflow

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benclawbot.cmfflow.data.FlowDatabase

class FlowApplication : Application() {
    val database: FlowDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            FlowDatabase::class.java,
            "cmf-flow.db",
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS context_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        selfReportId INTEGER NOT NULL,
                        capturedAtEpochMs INTEGER NOT NULL,
                        windowStartEpochMs INTEGER NOT NULL,
                        windowEndEpochMs INTEGER NOT NULL,
                        heartRateRecordCount INTEGER NOT NULL,
                        heartRateSampleCount INTEGER NOT NULL,
                        heartRateMinBpm REAL,
                        heartRateMaxBpm REAL,
                        heartRateMeanBpm REAL,
                        stepCount INTEGER,
                        sleepMinutesPrevious24h INTEGER,
                        healthDataOrigins TEXT NOT NULL,
                        collectionError TEXT,
                        FOREIGN KEY(selfReportId) REFERENCES self_reports(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_context_snapshots_selfReportId ON context_snapshots(selfReportId)")
            }
        }
    }
}
