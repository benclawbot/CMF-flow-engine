package com.benclawbot.cmfflow

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benclawbot.cmfflow.data.FlowDatabase

class FlowApplication : Application() {
    val database: FlowDatabase by lazy {
        Room.databaseBuilder(applicationContext, FlowDatabase::class.java, "cmf-flow.db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
            )
            .build()
    }

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
                """.trimIndent())
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
                db.execSQL("""
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
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recommendation_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        taskTitle TEXT NOT NULL,
                        presentedAtEpochMs INTEGER NOT NULL,
                        score REAL NOT NULL,
                        reasonsSnapshot TEXT NOT NULL,
                        response TEXT,
                        respondedAtEpochMs INTEGER,
                        outcomeSelfReportId INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_events_taskId ON recommendation_events(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_events_outcomeSelfReportId ON recommendation_events(outcomeSelfReportId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recommendation_events ADD COLUMN taskDomain TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS intervention_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        action TEXT NOT NULL,
                        presentedAtEpochMs INTEGER NOT NULL,
                        reasonsSnapshot TEXT NOT NULL,
                        response TEXT,
                        respondedAtEpochMs INTEGER,
                        outcomeSelfReportId INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intervention_events_outcomeSelfReportId ON intervention_events(outcomeSelfReportId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER,
                        taskTitle TEXT,
                        taskDomain TEXT,
                        startedAtEpochMs INTEGER NOT NULL,
                        endedAtEpochMs INTEGER,
                        struggleCount INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_taskId ON sessions(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_startedAtEpochMs ON sessions(startedAtEpochMs)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN usageAccessGranted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN appSwitchCount INTEGER")
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN unlockCount INTEGER")
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN screenInteractiveTransitions INTEGER")
                db.execSQL("ALTER TABLE context_snapshots ADD COLUMN notificationCount INTEGER")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS experiments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hypothesis TEXT NOT NULL,
                        conditionA TEXT NOT NULL,
                        conditionB TEXT NOT NULL,
                        primaryOutcome TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS experiment_assignments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        experimentId INTEGER NOT NULL,
                        assignedCondition TEXT NOT NULL,
                        assignedAtEpochMs INTEGER NOT NULL,
                        completedAtEpochMs INTEGER,
                        outcomeSelfReportId INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_experiment_assignments_experimentId ON experiment_assignments(experimentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_experiment_assignments_outcomeSelfReportId ON experiment_assignments(outcomeSelfReportId)")
            }
        }
    }
}
