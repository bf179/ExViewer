package com.hippo.ehviewer.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

// 批量收藏任务进度（app 级后台执行，进度持久化以支持恢复/重续）
@Entity(tableName = "batch_fav_tasks")
data class BatchFavTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // 运行中/完成/失败/中断
    val status: String,
    val total: Int,
    val done: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
