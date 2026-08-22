package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchFavTaskDao {
    @Insert
    suspend fun insert(task: BatchFavTask): Long

    @Update
    suspend fun update(task: BatchFavTask)

    @Query("DELETE FROM batch_fav_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM batch_fav_tasks ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): BatchFavTask?

    @Query("SELECT * FROM batch_fav_tasks ORDER BY id DESC LIMIT 1")
    fun observeLatest(): Flow<BatchFavTask?>
}
