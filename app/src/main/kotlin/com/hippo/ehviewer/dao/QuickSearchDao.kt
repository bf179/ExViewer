package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface QuickSearchDao {
    @Query("SELECT * FROM QUICK_SEARCH WHERE NAME NOT LIKE 'lastSearch%' ORDER BY POSITION")
    suspend fun list(): List<QuickSearch>

    @Query("SELECT * FROM QUICK_SEARCH ORDER BY POSITION LIMIT :limit OFFSET :offset")
    suspend fun list(offset: Int, limit: Int): List<QuickSearch>

    @Query("UPDATE QUICK_SEARCH SET POSITION = POSITION - 1 WHERE POSITION > :position")
    suspend fun fill(position: Int)

    @Update
    suspend fun update(quickSearchList: List<QuickSearch>)

    @Update
    suspend fun update(quickSearch: QuickSearch)

    @Query("SELECT * FROM QUICK_SEARCH WHERE NAME LIKE :name || '%' LIMIT 1")
    suspend fun getByNamePrefix(name: String): QuickSearch?

    @Insert
    suspend fun insert(quickSearch: QuickSearch): Long

    @Insert
    suspend fun insert(quickSearchList: List<QuickSearch>)

    @Delete
    suspend fun delete(quickSearch: QuickSearch)
}
