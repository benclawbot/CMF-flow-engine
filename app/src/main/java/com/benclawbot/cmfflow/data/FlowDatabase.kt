package com.benclawbot.cmfflow.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface SelfReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: SelfReportEntity): Long

    @Query("SELECT * FROM self_reports ORDER BY capturedAtEpochMs DESC LIMIT 100")
    fun observeRecent(): Flow<List<SelfReportEntity>>
}

@Dao
interface ContextSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: ContextSnapshotEntity): Long

    @Query("SELECT * FROM context_snapshots WHERE selfReportId = :selfReportId LIMIT 1")
    suspend fun forReport(selfReportId: Long): ContextSnapshotEntity?

    @Query("SELECT * FROM context_snapshots ORDER BY capturedAtEpochMs DESC LIMIT 100")
    fun observeRecent(): Flow<List<ContextSnapshotEntity>>
}

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Query("SELECT * FROM tasks WHERE status = 'open' ORDER BY createdAtEpochMs DESC")
    fun observeOpen(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET status = 'done' WHERE id = :taskId")
    suspend fun markDone(taskId: Long)
}

@Dao
interface RecommendationEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: RecommendationEventEntity): Long

    @Query("UPDATE recommendation_events SET response = :response, respondedAtEpochMs = :respondedAtEpochMs WHERE id = :eventId")
    suspend fun recordResponse(eventId: Long, response: String, respondedAtEpochMs: Long)

    @Query("""
        UPDATE recommendation_events
        SET outcomeSelfReportId = :selfReportId
        WHERE id = (
            SELECT id FROM recommendation_events
            WHERE response IS NOT NULL AND outcomeSelfReportId IS NULL
            ORDER BY respondedAtEpochMs DESC
            LIMIT 1
        )
    """)
    suspend fun attachOutcomeToLatestResponded(selfReportId: Long)

    @Query("SELECT * FROM recommendation_events ORDER BY presentedAtEpochMs DESC LIMIT 100")
    fun observeRecent(): Flow<List<RecommendationEventEntity>>
}

@Dao
interface InterventionEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: InterventionEventEntity): Long

    @Query("UPDATE intervention_events SET response = :response, respondedAtEpochMs = :respondedAtEpochMs WHERE id = :eventId")
    suspend fun recordResponse(eventId: Long, response: String, respondedAtEpochMs: Long)

    @Query("""
        UPDATE intervention_events
        SET outcomeSelfReportId = :selfReportId
        WHERE id = (
            SELECT id FROM intervention_events
            WHERE response IS NOT NULL AND outcomeSelfReportId IS NULL
            ORDER BY respondedAtEpochMs DESC
            LIMIT 1
        )
    """)
    suspend fun attachOutcomeToLatestResponded(selfReportId: Long)

    @Query("SELECT * FROM intervention_events ORDER BY presentedAtEpochMs DESC LIMIT 100")
    fun observeRecent(): Flow<List<InterventionEventEntity>>
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions WHERE status = 'active' ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeActive(): Flow<SessionEntity?>

    @Query("UPDATE sessions SET struggleCount = struggleCount + 1 WHERE id = :sessionId")
    suspend fun recordStruggle(sessionId: Long)

    @Query("UPDATE sessions SET endedAtEpochMs = :endedAtEpochMs, status = 'ended' WHERE id = :sessionId")
    suspend fun end(sessionId: Long, endedAtEpochMs: Long)
}

@Database(
    entities = [SelfReportEntity::class, ContextSnapshotEntity::class, TaskEntity::class, RecommendationEventEntity::class, InterventionEventEntity::class, SessionEntity::class],
    version = 9,
    exportSchema = true,
)
abstract class FlowDatabase : RoomDatabase() {
    abstract fun selfReportDao(): SelfReportDao
    abstract fun contextSnapshotDao(): ContextSnapshotDao
    abstract fun taskDao(): TaskDao
    abstract fun recommendationEventDao(): RecommendationEventDao
    abstract fun interventionEventDao(): InterventionEventDao
    abstract fun sessionDao(): SessionDao
}
