/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import arrow.fx.coroutines.resource
import arrow.fx.coroutines.resourceScope
import com.hippo.ehviewer.EhApplication.Companion.ktorClient
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.dao.DownloadArtist
import com.hippo.ehviewer.dao.DownloadDirname
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.DownloadLabel
import com.hippo.ehviewer.dao.EhDatabase
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.HistoryInfo
import com.hippo.ehviewer.dao.LocalFavoriteInfo
import com.hippo.ehviewer.dao.ProgressInfo
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.dao.Schema17to18
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.util.sendTo
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path
import okio.Path.Companion.toOkioPath
import splitties.arch.room.roomDb
import splitties.init.appCtx

@Serializable
data class ExlApiRequest(
    val user: String,
    val gid: Long,
    val token: String,
    var favoriteslot: Int,
    val op: String,
)

@Serializable
data class PqApiRequest(
    val user: String,
    val itype: String,
    val icontent: String,
)

// 保持一个全局的 Toast 引用
private var currentToast: Toast? = null

fun showToastOnMainThread(message: String) {
    Handler(Looper.getMainLooper()).post {
        // 取消前一个 Toast
        currentToast?.cancel()
        // 创建并显示新的 Toast
        currentToast = Toast.makeText(appCtx, message, Toast.LENGTH_SHORT).apply {
            show()
        }
    }
}

// 全局 Toast 工具函数
// fun showToastOnMainThread(message: String) {
//     Handler(Looper.getMainLooper()).post {
//         Toast.makeText(appCtx, message, Toast.LENGTH_SHORT).show()
//     }
// }

suspend fun sendExlApiRequest(exlapirequest: ExlApiRequest, sapi: String, showSuccessToast: Boolean = true) {
    var retryCount = 0
    val maxRetries = 3
    val retryDelay = 5000L // 5秒

    while (retryCount < maxRetries) {
        try {
            val response = ktorClient.post(sapi) {
                method = HttpMethod.Post
                val request = exlapirequest
                val json = Json.encodeToString(request)
                setBody(TextContent(text = json, contentType = ContentType.Application.Json))
            }
            // 解析响应内容
            val responseBody: String = response.body()
            val jsonResponse = Json.decodeFromString<Map<String, String>>(responseBody)

            when (response.status.value) {
                201 -> { // 成功
                    if (showSuccessToast) {
                        jsonResponse["message"]?.let { message ->
                            showToastOnMainThread(message)
                        } ?: showToastOnMainThread("Operation completed successfully")
                    }
                    return // 成功则直接返回
                }

                400 -> { // 客户端错误
                    jsonResponse["error"]?.let { error ->
                        showToastOnMainThread("Bad request: $error")
                    } ?: showToastOnMainThread("Invalid request format")
                    return // 也直接返回
                }

                500 -> { // 服务器错误
                    jsonResponse["error"]?.let { error ->
                        showToastOnMainThread("Server error: $error")
                    } ?: showToastOnMainThread("Internal server error")
                    return // 也直接返回
                }

                else -> { // 其他状态码
                    showToastOnMainThread("Unexpected response: ${response.status}")
                    throw RuntimeException("Unexpected status: ${response.status}")
                }
            }
        } catch (e: Exception) {
            retryCount++
            showToastOnMainThread("(${retryCount + 1}/$maxRetries)-${retryDelay / 1000}s Failed to call SAPI: ${e.message}")
            if (retryCount >= maxRetries) {
                showToastOnMainThread("Failed after $maxRetries attempts: ${e.message}")
                return
            }
            // 延迟后重试
            delay(retryDelay)
        }
    }
}

suspend fun sendPqApiRequest(pqapirequest: PqApiRequest, papi: String) {
    var retryCount = 0
    val maxRetries = 3
    val retryDelay = 5000L // 5秒

    while (retryCount < maxRetries) {
        try {
            val response = ktorClient.post(papi) {
                method = HttpMethod.Post
                val request = pqapirequest
                val json = Json.encodeToString(request)
                setBody(TextContent(text = json, contentType = ContentType.Application.Json))
            }
            // 解析响应内容
            val responseBody: String = response.body()
            val jsonResponse = Json.decodeFromString<Map<String, String>>(responseBody)

            when (response.status.value) {
                201 -> { // 成功
                    jsonResponse["message"]?.let { message ->
                        showToastOnMainThread(message)
                    } ?: showToastOnMainThread("Operation completed successfully")
                    return // 成功则直接返回
                }

                400 -> { // 客户端错误
                    jsonResponse["error"]?.let { error ->
                        showToastOnMainThread("Bad request: $error")
                    } ?: showToastOnMainThread("Invalid request format")
                    return // 也直接返回
                }

                500 -> { // 服务器错误
                    jsonResponse["error"]?.let { error ->
                        showToastOnMainThread("Server error: $error")
                    } ?: showToastOnMainThread("Internal server error")
                    return // 也直接返回
                }

                else -> { // 其他状态码
                    showToastOnMainThread("Unexpected response: ${response.status}")
                    throw RuntimeException("Unexpected status: ${response.status}")
                }
            }
        } catch (e: Exception) {
            retryCount++
            showToastOnMainThread("(${retryCount + 1}/$maxRetries)-${retryDelay / 1000}s Failed to call PAPI: ${e.message}")
            if (retryCount >= maxRetries) {
                showToastOnMainThread("Failed after $maxRetries attempts: ${e.message}")
                return
            }
            // 延迟后重试
            delay(retryDelay)
        }
    }
}

object EhDB {
    private const val DB_NAME = "eh.db"
    private val db = roomDb<EhDatabase>(DB_NAME) {
        addMigrations(Schema17to18())
    }

    suspend fun putGalleryInfo(galleryInfo: BaseGalleryInfo) {
        db.galleryDao().upsert(galleryInfo)
    }

    private suspend fun deleteGalleryInfo(galleryInfo: BaseGalleryInfo) {
        runCatching { db.galleryDao().delete(galleryInfo) }
    }

    suspend fun updateGalleryInfo(galleryInfoList: List<BaseGalleryInfo>) {
        db.galleryDao().update(galleryInfoList)
    }

    // 全局 Toast 工具函数
    // private fun showToastOnMainThread(message: String) {
    //     Handler(Looper.getMainLooper()).post {
    //         Toast.makeText(appCtx, message, Toast.LENGTH_LONG).show()
    //     }
    // }

    fun getReadProgressFlow(gid: Long) = db.progressDao().getPageFlow(gid)
    suspend fun getReadProgress(gid: Long) = db.progressDao().getPage(gid)
    suspend fun putReadProgress(gid: Long, page: Int) = db.progressDao().upsert(ProgressInfo(gid, page))
    suspend fun clearProgressInfo() = db.progressDao().deleteAll()

    suspend fun getAllDownloadInfo() = db.downloadsDao().joinList().onEach {
        if (it.state == DownloadInfo.STATE_WAIT || it.state == DownloadInfo.STATE_DOWNLOAD) {
            it.state = DownloadInfo.STATE_NONE
        }
    }

    suspend fun updateDownloadInfo(downloadInfo: Collection<DownloadInfo>) {
        val dao = db.downloadsDao()
        dao.update(downloadInfo.map(DownloadInfo::downloadInfo))
    }

    suspend fun putDownloadInfo(downloadInfo: DownloadInfo) {
        putGalleryInfo(downloadInfo.galleryInfo)
        db.downloadsDao().upsert(downloadInfo.downloadInfo)
    }

    suspend fun removeDownloadInfo(downloadInfo: DownloadInfo) {
        val dao = db.downloadsDao()
        dao.delete(downloadInfo.downloadInfo)
        deleteGalleryInfo(downloadInfo.galleryInfo)
    }

    suspend fun randomLocalFav() = db.localFavoritesDao().random()

    suspend fun removeDownloadInfo(downloadInfo: List<DownloadInfo>) {
        val dao = db.downloadsDao()
        downloadInfo.forEach {
            dao.delete(it.downloadInfo)
            deleteGalleryInfo(it.galleryInfo)
        }
    }

    suspend fun getDownloadDirname(gid: Long): String? {
        val dao = db.downloadDirnameDao()
        val raw = dao.load(gid)
        return raw?.dirname
    }

    suspend fun putDownloadDirname(gid: Long, dirname: String) {
        val dao = db.downloadDirnameDao()
        dao.upsert(DownloadDirname(gid, dirname))
    }

    suspend fun removeDownloadDirname(gid: Long) {
        val dao = db.downloadDirnameDao()
        dao.deleteByKey(gid)
    }

    private suspend fun importDownloadDirname(downloadDirnameList: List<DownloadDirname>) {
        val dao = db.downloadDirnameDao()
        dao.insertOrIgnore(downloadDirnameList)
    }

    val downloadsCountByLabel
        get() = db.downloadsDao().countByLabel()

    val downloadsCountByArtist
        get() = db.downloadsDao().countByArtist()

    suspend fun getAllDownloadLabelList() = db.downloadLabelDao().list()

    suspend fun addDownloadLabel(raw: DownloadLabel): DownloadLabel {
        // Reset id
        raw.id = null
        val dao = db.downloadLabelDao()
        raw.id = dao.insert(raw)
        return raw
    }

    suspend fun updateDownloadLabel(raw: DownloadLabel) {
        val dao = db.downloadLabelDao()
        dao.update(raw)
    }

    suspend fun updateDownloadLabel(downloadLabels: List<DownloadLabel>) {
        val dao = db.downloadLabelDao()
        dao.update(downloadLabels)
    }

    suspend fun removeDownloadLabel(raw: DownloadLabel) {
        val dao = db.downloadLabelDao()
        dao.delete(raw)
        dao.fill(raw.position)
    }

    suspend fun putDownloadArtist(gid: Long, artists: List<DownloadArtist>) {
        if (artists.isNotEmpty()) {
            val dao = db.downloadArtistDao()
            dao.deleteByGid(gid)
            dao.insertOrIgnore(artists)
        }
    }

    suspend fun removeLocalFavorites(galleryInfo: BaseGalleryInfo) {
        db.localFavoritesDao().deleteByKey(galleryInfo.gid)
        deleteGalleryInfo(galleryInfo)
        val syncfav = Settings.syncFav
        if (syncfav) {
            val sapi = Settings.sapiUrl
            if (!sapi.isNullOrBlank()) {
                // 向 API 发送 POST 请求
                val exlar = ExlApiRequest(
                    user = "loliwant",
                    gid = galleryInfo.gid,
                    token = galleryInfo.token,
                    favoriteslot = galleryInfo.favoriteSlot,
                    op = "del",
                )
                sendExlApiRequest(exlar, sapi)
            }
        }
    }

    suspend fun removeLocalFavorites(galleryInfoList: Collection<BaseGalleryInfo>) {
        galleryInfoList.forEach {
            removeLocalFavorites(it)
        }
    }

    suspend fun containLocalFavorites(gid: Long): Boolean {
        val dao = db.localFavoritesDao()
        return dao.contains(gid)
    }

    suspend fun putLocalFavorites(galleryInfo: BaseGalleryInfo, showSuccessToast: Boolean = true) {
        putGalleryInfo(galleryInfo)
        db.localFavoritesDao().upsert(LocalFavoriteInfo(galleryInfo.gid))
        val syncfav = Settings.syncFav
        if (syncfav) {
            val sapi = Settings.sapiUrl
            if (!sapi.isNullOrBlank()) {
                // 向 API 发送 POST 请求
                val exlar = ExlApiRequest(
                    user = "loliwant",
                    gid = galleryInfo.gid,
                    token = galleryInfo.token,
                    favoriteslot = galleryInfo.favoriteSlot,
                    op = "add",
                )
                sendExlApiRequest(exlar, sapi, showSuccessToast)
            }
        }
    }

    suspend fun putLocalFavorites(galleryInfoList: Collection<BaseGalleryInfo>) {
        galleryInfoList.forEach {
            putLocalFavorites(it)
        }
    }

    private suspend fun importLocalFavorites(localFavorites: List<LocalFavoriteInfo>) {
        db.localFavoritesDao().insertOrIgnore(localFavorites)
    }

    suspend fun getAllQuickSearch() = db.quickSearchDao().list()

    suspend fun insertQuickSearch(quickSearch: QuickSearch) {
        val dao = db.quickSearchDao()
        quickSearch.id = dao.insert(quickSearch)
    }

    private suspend fun importQuickSearch(quickSearchList: List<QuickSearch>) {
        val dao = db.quickSearchDao()
        dao.insert(quickSearchList)
    }

    suspend fun deleteQuickSearch(quickSearch: QuickSearch) {
        val dao = db.quickSearchDao()
        dao.delete(quickSearch)
        dao.fill(quickSearch.position)
    }

    suspend fun updateQuickSearch(quickSearchList: List<QuickSearch>) {
        val dao = db.quickSearchDao()
        dao.update(quickSearchList)
    }

    suspend fun getLastSearch(): QuickSearch? {
        val dao = db.quickSearchDao()
        return dao.getByNamePrefix("lastSearch")
    }

    suspend fun updateLastSearch(newLastSearch: QuickSearch) {
        val dao = db.quickSearchDao()

        // 1. 查找 NAME 以 "lastSearch" 开头的现有记录
        val existing = dao.getByNamePrefix("lastSearch")

        if (existing != null) {
            // 2. 如果存在则更新（保留原ID）
            newLastSearch.id = existing.id
            dao.update(newLastSearch)
        } else {
            // 3. 如果不存在则插入新记录
            dao.insert(newLastSearch)
        }
    }

    val historyLazyList
        get() = db.historyDao().joinListLazy()

    fun searchHistory(keyword: String) = db.historyDao().joinListLazy("*$keyword*")

    val localFavLazyList
        get() = db.localFavoritesDao().joinListLazy()

    val localFavCount: Flow<Int>
        get() = db.localFavoritesDao().count()

    fun searchLocalFav(keyword: String) = db.localFavoritesDao().joinListLazy("*$keyword*")

    suspend fun putHistoryInfo(galleryInfo: BaseGalleryInfo) {
        putGalleryInfo(galleryInfo)
        db.historyDao().upsert(HistoryInfo(galleryInfo.gid))
    }

    suspend fun updateFavoriteSlot(gid: Long, slot: Int) {
        val dao = db.galleryDao()
        dao.load(gid)?.let {
            it.favoriteSlot = slot
            dao.update(it)
        }
    }

    private suspend fun importHistoryInfo(historyInfoList: List<HistoryInfo>) {
        val dao = db.historyDao()
        dao.insertOrIgnore(historyInfoList)
    }

    suspend fun deleteHistoryInfo(galleryInfo: BaseGalleryInfo) {
        val dao = db.historyDao()
        dao.deleteByKey(galleryInfo.gid)
        deleteGalleryInfo(galleryInfo)
    }

    suspend fun clearHistoryInfo() {
        val dao = db.historyDao()
        val historyList = dao.list()
        dao.deleteAll()
        historyList.forEach { runCatching { db.galleryDao().deleteByKey(it.gid) } }
    }

    suspend fun getAllFilter() = db.filterDao().list()

    suspend fun addFilter(filter: Filter): Boolean {
        val existFilter = runCatching { db.filterDao().load(filter.text, filter.mode.field) }.getOrNull()
        return if (existFilter == null) {
            filter.id = null
            filter.id = db.filterDao().insert(filter)
            true
        } else {
            false
        }
    }

    suspend fun deleteFilter(filter: Filter) {
        db.filterDao().delete(filter)
    }

    suspend fun updateFilter(filter: Filter) {
        db.filterDao().update(filter)
    }

    fun exportDB(context: Context, file: Path) {
        db.query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToNext() }
        val dbFile = context.getDatabasePath(DB_NAME)
        dbFile.toOkioPath() sendTo file
    }

    suspend fun importDB(context: Context, uri: Uri) = resourceScope {
        val tempDBName = "tmp.db"
        val oldDB = resource {
            context.deleteDatabase(tempDBName)
            roomDb<EhDatabase>(tempDBName) {
                createFromInputStream { context.contentResolver.openInputStream(uri) }
                addMigrations(Schema17to18())
            }
        } release { db ->
            db.close()
            context.deleteDatabase(tempDBName)
        }

        db.galleryDao().insertOrIgnore(oldDB.galleryDao().list())
        db.progressDao().insertOrIgnore(oldDB.progressDao().list())

        val downloadLabelList = oldDB.downloadLabelDao().list()
        DownloadManager.addDownloadLabel(downloadLabelList)

        oldDB.downloadDirnameDao().list().let {
            importDownloadDirname(it)
        }

        val downloadInfoList = oldDB.downloadsDao().joinList().asReversed()
        DownloadManager.addDownload(downloadInfoList)

        val historyInfoList = oldDB.historyDao().list()
        importHistoryInfo(historyInfoList)

        val quickSearchList = oldDB.quickSearchDao().list()
        val currentQuickSearchList = db.quickSearchDao().list()
        val offset = currentQuickSearchList.size
        val importList = quickSearchList.filter { newQS ->
            currentQuickSearchList.none { it.name == newQS.name }
        }.onEachIndexed { index, q ->
            q.id = null
            q.position = index + offset
        }
        importQuickSearch(importList)

        oldDB.localFavoritesDao().list().let {
            importLocalFavorites(it)
        }

        oldDB.filterDao().list().forEach {
            addFilter(it)
        }
    }
}
