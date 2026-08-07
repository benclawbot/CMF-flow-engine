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

@Database(
    entities = [SelfReportEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FlowDatabase : RoomDatabase() {
    abstract fun selfReportDao(): SelfReportDao
}
