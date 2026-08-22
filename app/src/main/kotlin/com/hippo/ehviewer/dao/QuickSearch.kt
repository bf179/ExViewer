package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "QUICK_SEARCH")
data class QuickSearch(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,

    @ColumnInfo(name = "NAME")
    var name: String,

    // 隐藏类型：0=快速搜索；1=隐藏标题；2=隐藏上传者；3=隐藏标签（v26 新增列）
    @ColumnInfo(name = "HIDE_TYPE", defaultValue = "0")
    var hideType: Int = 0,

    @ColumnInfo(name = "MODE")
    var mode: Int = 0,

    @ColumnInfo(name = "CATEGORY")
    var category: Int = 0,

    @ColumnInfo(name = "KEYWORD")
    var keyword: String? = null,

    @ColumnInfo(name = "ADVANCE_SEARCH")
    var advanceSearch: Int = 0,

    @ColumnInfo(name = "MIN_RATING")
    var minRating: Int = 0,

    @ColumnInfo(name = "PAGE_FROM")
    var pageFrom: Int = 0,

    @ColumnInfo(name = "PAGE_TO")
    var pageTo: Int = 0,

    @ColumnInfo(name = "POSITION")
    var position: Int = 0,
)
