package com.hippo.ehviewer.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_outbox")
data class SyncOutbox(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val api: String,
    val payload: String,
    val createdAt: Long,
)
