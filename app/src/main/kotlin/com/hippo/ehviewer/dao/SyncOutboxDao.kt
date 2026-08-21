package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SyncOutboxDao {
    @Insert
    suspend fun insert(item: SyncOutbox): Long

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM sync_outbox ORDER BY id ASC LIMIT 50")
    suspend fun pending(): List<SyncOutbox>
}
