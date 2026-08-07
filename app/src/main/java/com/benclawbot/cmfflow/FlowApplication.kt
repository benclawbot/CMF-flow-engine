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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN localHour INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN localDayOfWeek INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN batteryPercent INTEGER")
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN isCharging INTEGER")
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN isPhoneInteractive INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        domain TEXT NOT NULL,
                        valueScore INTEGER NOT NULL,
                        urgencyScore INTEGER NOT NULL,
                        difficultyScore INTEGER NOT NULL,
                        estimatedMinutes INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        dueAtEpochMs INTEGER,
                        createdAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
