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

@Database(
    entities = [SelfReportEntity::class, ContextSnapshotEntity::class, TaskEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class FlowDatabase : RoomDatabase() {
    abstract fun selfReportDao(): SelfReportDao
    abstract fun contextSnapshotDao(): ContextSnapshotDao
    abstract fun taskDao(): TaskDao
}
