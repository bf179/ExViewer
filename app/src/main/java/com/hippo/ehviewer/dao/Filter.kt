package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FILTER")
data class Filter(
    @ColumnInfo(name = "MODE")
    val mode: Int,

    @ColumnInfo(name = "TEXT")
    var text: String,

    @ColumnInfo(name = "ENABLE")
    var enable: Boolean = true,

    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,
)
