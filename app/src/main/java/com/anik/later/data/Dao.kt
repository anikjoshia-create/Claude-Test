package com.anik.later.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {
    @Query("SELECT * FROM scheduled_messages ORDER BY enabled DESC, scheduledAt ASC")
    fun observeAll(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun byId(id: Long): ScheduledMessage?

    @Query("SELECT * FROM scheduled_messages WHERE enabled = 1")
    suspend fun allEnabled(): List<ScheduledMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ScheduledMessage): Long

    @Update
    suspend fun update(message: ScheduledMessage)

    @Delete
    suspend fun delete(message: ScheduledMessage)
}

@Dao
interface SendAttemptDao {
    @Query("SELECT * FROM send_attempts ORDER BY attemptedAt DESC LIMIT 200")
    fun observeRecent(): Flow<List<SendAttempt>>

    @Insert
    suspend fun insert(attempt: SendAttempt)

    @Query("DELETE FROM send_attempts")
    suspend fun clear()
}
