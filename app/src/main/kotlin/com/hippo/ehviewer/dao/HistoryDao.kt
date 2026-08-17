package com.hippo.ehviewer.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hippo.ehviewer.client.data.BaseGalleryInfo

@Dao
interface HistoryDao {
    @Query("SELECT HISTORY.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME")
    suspend fun list(): List<HistoryInfo>

    @Query("SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
    fun joinListLazy(): PagingSource<Int, BaseGalleryInfo>

    @Query(
        """SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID)
        JOIN GALLERIES_FTS ON GALLERIES.rowid = docid WHERE GALLERIES_FTS MATCH :title ORDER BY TIME DESC""",
    )
    fun joinListLazy(title: String): PagingSource<Int, BaseGalleryInfo>

    @Query("SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) WHERE FAVORITE_SLOT = -2 ORDER BY TIME DESC")
    fun joinListLazyExcludeFav(): PagingSource<Int, BaseGalleryInfo>

    @Query(
        """SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID)
        JOIN GALLERIES_FTS ON GALLERIES.rowid = docid WHERE GALLERIES_FTS MATCH :title AND FAVORITE_SLOT = -2 ORDER BY TIME DESC""",
    )
    fun joinListLazyExcludeFav(title: String): PagingSource<Int, BaseGalleryInfo>

    @Query("SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
    suspend fun joinList(): List<BaseGalleryInfo>

    @Query("DELETE FROM HISTORY WHERE GID IN (:gids)")
    suspend fun deleteByKeyRange(gids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(historyInfoList: List<HistoryInfo>)

    @Upsert
    suspend fun upsert(historyInfo: HistoryInfo)

    @Query("DELETE FROM HISTORY WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)

    @Query("DELETE FROM HISTORY")
    suspend fun deleteAll()
}
