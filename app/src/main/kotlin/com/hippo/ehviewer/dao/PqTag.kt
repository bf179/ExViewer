package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

// 优先队列标签缓存（GET /pq_tags 拉取，供隐藏优先队列标签画廊判定）
@Entity(tableName = "pq_tag")
data class PqTag(
    @PrimaryKey
    @ColumnInfo(name = "tag")
    val tag: String,
)

@Dao
interface PqTagDao {
    @Query("SELECT tag FROM pq_tag")
    suspend fun getAll(): List<String>

    @Query("DELETE FROM pq_tag")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<PqTag>)
}
