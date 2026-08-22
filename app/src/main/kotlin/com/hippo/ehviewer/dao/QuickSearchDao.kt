package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface QuickSearchDao {
    // 快速搜索仅作用于 HIDE_TYPE=0 的条目（v26 起）
    @Query("SELECT * FROM QUICK_SEARCH WHERE HIDE_TYPE = 0 AND NAME NOT LIKE 'lastSearch%' ORDER BY POSITION")
    suspend fun list(): List<QuickSearch>

    @Query("SELECT * FROM QUICK_SEARCH WHERE HIDE_TYPE = 0 ORDER BY POSITION LIMIT :limit OFFSET :offset")
    suspend fun list(offset: Int, limit: Int): List<QuickSearch>

    // 一次性迁移用：返回全部条目（含隐藏列表）
    @Query("SELECT * FROM QUICK_SEARCH")
    suspend fun listAll(): List<QuickSearch>

    // 隐藏列表：HIDE_TYPE ∈ 1..3（1=标题，2=上传者，3=标签）
    @Query("SELECT * FROM QUICK_SEARCH WHERE HIDE_TYPE IN (1, 2, 3) ORDER BY POSITION")
    suspend fun getHideList(): List<QuickSearch>

    @Query("UPDATE QUICK_SEARCH SET POSITION = POSITION - 1 WHERE POSITION > :position")
    suspend fun fill(position: Int)

    @Update
    suspend fun update(quickSearchList: List<QuickSearch>)

    @Update
    suspend fun update(quickSearch: QuickSearch)

    @Query("SELECT * FROM QUICK_SEARCH WHERE NAME LIKE :name || '%' AND HIDE_TYPE = 0 LIMIT 1")
    suspend fun getByNamePrefix(name: String): QuickSearch?

    @Insert
    suspend fun insert(quickSearch: QuickSearch): Long

    @Insert
    suspend fun insert(quickSearchList: List<QuickSearch>)

    @Delete
    suspend fun delete(quickSearch: QuickSearch)
}
