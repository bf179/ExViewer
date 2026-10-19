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
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.dao.DownloadArtist
import com.hippo.ehviewer.dao.DownloadDirname
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.DownloadLabel
import com.hippo.ehviewer.dao.EhDatabase
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.HistoryInfo
import com.hippo.ehviewer.dao.LocalFavoriteInfo
import com.hippo.ehviewer.dao.PqTag
import com.hippo.ehviewer.dao.ProgressInfo
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.dao.Schema17to18
import com.hippo.ehviewer.dao.SyncOutbox
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.util.sendTo
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
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

// 优先队列单画廊条目（GET /pq_galleries?section=single / group&group=...）
@Serializable
data class PqGalleryItem(
    val gid: Long = 0,
    val token: String = "",
    val title: String? = null,
    val thumb: String? = null,
    val fav_status: Int = 0,
)

// 优先队列标签组条目（GET /pq_galleries?section=group）
@Serializable
data class PqGroupItem(
    val group_content: String = "",
    val count: Int = 0,
    val new_count: Int = 0,
)

// 兼容 {items: [...]} 信封结构的分页响应
@Serializable
data class PqPage<T>(
    val items: List<T> = emptyList(),
)

// 服务器隐藏列表条目（GET /hide_list 返回 [{hide_type, content}]）
@Serializable
data class HideListServerItem(
    val hide_type: Int,
    val content: String,
)

// 隐藏列表上传请求信封（POST /hide_list 接收 {items: [...]}）
@Serializable
data class HideListUploadRequest(
    val items: List<HideListServerItem>,
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

suspend fun sendExlApiRequest(exlapirequest: ExlApiRequest, sapi: String, showSuccessToast: Boolean = true): Boolean {
    // API Token 为空时不发送（服务器开启鉴权后会 401），提示后直接失败
    val apiToken = Settings.apiToken
    if (apiToken.isNullOrBlank()) {
        showToastOnMainThread("未配置 API Token，请求已取消")
        return false
    }
    var retryCount = 0
    val maxRetries = 3
    val retryDelay = 5000L // 5秒

    while (retryCount < maxRetries) {
        try {
            val response = ktorClient.post(sapi) {
                method = HttpMethod.Post
                header("Authorization", "Bearer $apiToken")
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
                    return true // 成功则直接返回
                }

                400 -> { // 客户端错误
                    jsonResponse["error"]?.let { error ->
                        showToastOnMainThread("Bad request: $error")
                    } ?: showToastOnMainThread("Invalid request format")
                    return false // 也直接返回
                }

                500 -> { // 服务器错误
                    jsonResponse["error"]?.let { error ->
                        showToastOnMainThread("Server error: $error")
                    } ?: showToastOnMainThread("Internal server error")
                    return false // 也直接返回
                }

                else -> { // 其他状态码
                    showToastOnMainThread("Unexpected response: ${response.status}")
                    throw RuntimeException("Unexpected status: ${response.status}")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retryCount++
            showToastOnMainThread("(${retryCount + 1}/$maxRetries)-${retryDelay / 1000}s Failed to call SAPI: ${e.message}")
            if (retryCount >= maxRetries) {
                showToastOnMainThread("Failed after $maxRetries attempts: ${e.message}")
                return false
            }
            // 延迟后重试
            delay(retryDelay)
        }
    }
    return false
}

suspend fun sendPqApiRequest(pqapirequest: PqApiRequest, papi: String): Boolean {
    // API Token 为空时不发送（服务器开启鉴权后会 401），提示后直接失败
    val apiToken = Settings.apiToken
    if (apiToken.isNullOrBlank()) {
        showToastOnMainThread("未配置 API Token，请求已取消")
        return false
    }
    var retryCount = 0
    val maxRetries = 3
    val retryDelay = 5000L // 5秒

    while (retryCount < maxRetries) {
        try {
            val response = ktorClient.post(papi) {
                method = HttpMethod.Post
                header("Authorization", "Bearer $apiToken")
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
                    return true // 成功则直接返回
                }

                400 -> { // 客户端错误
                    jsonResponse["error"]?.let { error ->
                        showToastOnMainThread("Bad request: $error")
                    } ?: showToastOnMainThread("Invalid request format")
                    return false // 也直接返回
                }

                500 -> { // 服务器错误
                    jsonResponse["error"]?.let { error ->
                        showToastOnMainThread("Server error: $error")
                    } ?: showToastOnMainThread("Internal server error")
                    return false // 也直接返回
                }

                else -> { // 其他状态码
                    showToastOnMainThread("Unexpected response: ${response.status}")
                    throw RuntimeException("Unexpected status: ${response.status}")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retryCount++
            showToastOnMainThread("(${retryCount + 1}/$maxRetries)-${retryDelay / 1000}s Failed to call PAPI: ${e.message}")
            if (retryCount >= maxRetries) {
                showToastOnMainThread("Failed after $maxRetries attempts: ${e.message}")
                return false
            }
            // 延迟后重试
            delay(retryDelay)
        }
    }
    return false
}

// ===== 优先队列 API（GET /pq_galleries，Bearer 鉴权）=====

// 优先队列响应解析用的 Json（容忍未知字段与缺省字段）
private val pqJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

// 兼容纯数组与 {items: [...]} 信封两种响应结构
private inline fun <reified T> parsePqItems(body: String): List<T> = try {
    pqJson.decodeFromString<List<T>>(body)
} catch (_: Exception) {
    pqJson.decodeFromString<PqPage<T>>(body).items
}

// 拼接优先队列分页请求 URL
private fun buildPqUrl(base: String, section: String, page: Int, pageSize: Int, hideFav: Boolean, group: String?): String {
    val builder = base.toHttpUrl().newBuilder()
        .addQueryParameter("section", section)
        .addQueryParameter("page", page.toString())
        .addQueryParameter("page_size", pageSize.toString())
        .addQueryParameter("hide_fav", if (hideFav) "1" else "0")
    if (!group.isNullOrBlank()) {
        builder.addQueryParameter("group", group)
    }
    return builder.build().toString()
}

// 优先队列 GET 请求（带 Bearer 鉴权），失败返回 null（已 toast 提示）
private suspend fun <T> pqGet(url: String, decode: (String) -> T): T? {
    // API Token 为空时不发送（服务器开启鉴权后会 401），提示后返回 null
    val apiToken = Settings.apiToken
    if (apiToken.isNullOrBlank()) {
        showToastOnMainThread("未配置 API Token，无法请求优先队列")
        return null
    }
    return try {
        val response = ktorClient.get(url) {
            header("Authorization", "Bearer $apiToken")
        }
        if (response.status.value in 200..299) {
            decode(response.body())
        } else {
            showToastOnMainThread("优先队列请求失败: ${response.status}")
            null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        showToastOnMainThread("优先队列请求异常: ${e.message}")
        null
    }
}

// 单画廊段（section=single），按 updated_at 倒序分页
suspend fun fetchPqSingles(pqUrl: String, page: Int, pageSize: Int, hideFav: Boolean): List<PqGalleryItem>? = pqGet(buildPqUrl(pqUrl, "single", page, pageSize, hideFav, null)) { body -> parsePqItems<PqGalleryItem>(body) }

// 标签组列表（section=group），组列表分页
suspend fun fetchPqGroups(pqUrl: String, page: Int, pageSize: Int, hideFav: Boolean): List<PqGroupItem>? = pqGet(buildPqUrl(pqUrl, "group", page, pageSize, hideFav, null)) { body -> parsePqItems<PqGroupItem>(body) }

// 组内画廊（section=group & group=<content>），组内分页
suspend fun fetchPqGroupGalleries(pqUrl: String, group: String, page: Int, pageSize: Int, hideFav: Boolean): List<PqGalleryItem>? = pqGet(buildPqUrl(pqUrl, "group", page, pageSize, hideFav, group)) { body -> parsePqItems<PqGalleryItem>(body) }

object EhDB {
    private const val DB_NAME = "eh.db"
    private val db = roomDb<EhDatabase>(DB_NAME) {
        addMigrations(Schema17to18())
    }

    fun syncOutboxDao() = db.syncOutboxDao()

    fun batchFavTaskDao() = db.batchFavTaskDao()

    suspend fun enqueueSyncOutbox(api: String, payload: String) {
        db.syncOutboxDao().insert(SyncOutbox(api = api, payload = payload, createdAt = System.currentTimeMillis()))
    }

    suspend fun flushSyncOutbox() {
        val dao = db.syncOutboxDao()
        dao.pending().forEach { item ->
            val success = try {
                when (item.api) {
                    "sapi" -> {
                        val sapi = Settings.sapiUrl
                        if (sapi.isNullOrBlank()) false else sendExlApiRequest(Json.decodeFromString<ExlApiRequest>(item.payload), sapi, false)
                    }

                    "papi" -> {
                        val papi = Settings.papiUrl
                        if (papi.isNullOrBlank()) false else sendPqApiRequest(Json.decodeFromString<PqApiRequest>(item.payload), papi)
                    }

                    else -> false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (success) {
                dao.deleteById(item.id)
            }
        }
    }

    suspend fun syncExlApiRequest(req: ExlApiRequest, url: String, showSuccessToast: Boolean = true) {
        // 整体在 NonCancellable 中执行：flush/send/失败入队都不会被 UI 协程取消打断
        withContext(NonCancellable) {
            flushSyncOutbox()
            val success = sendExlApiRequest(req, url, showSuccessToast)
            if (!success) {
                enqueueSyncOutbox("sapi", Json.encodeToString(req))
            }
        }
    }

    suspend fun syncPqApiRequest(req: PqApiRequest, url: String) {
        withContext(NonCancellable) {
            flushSyncOutbox()
            val success = sendPqApiRequest(req, url)
            if (!success) {
                enqueueSyncOutbox("papi", Json.encodeToString(req))
            }
        }
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
                syncExlApiRequest(exlar, sapi)
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
                syncExlApiRequest(exlar, sapi, showSuccessToast)
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

    // ===== 快速搜索与隐藏列表（QUICK_SEARCH 同表，HIDE_TYPE 区分）=====

    // 进程内一次性迁移标志：避免重复查库（持久化标志见 Settings.quickSearchHideTypeMigrated）
    private var quickSearchClassified = false

    // v26 一次性数据分类：艺术家/团队/角色/Coser/临时 → 标签隐藏(3)；MODE=1 → 上传者隐藏(2)；
    // 引号包裹标题 → 标题隐藏(1) 且 KEYWORD 去引号；其余保持快速搜索(0)。只执行一次。
    suspend fun ensureQuickSearchClassified() {
        if (quickSearchClassified || Settings.quickSearchHideTypeMigrated) return
        val dao = db.quickSearchDao()
        dao.listAll().forEach { q ->
            val name = q.name
            val newHideType = when {
                name.startsWith("艺术家：") ||
                    name.startsWith("团队：") ||
                    name.startsWith("角色：") ||
                    name.startsWith("Coser：") ||
                    name.startsWith("临时：") -> 3

                q.mode == 1 -> 2

                name.length > 1 && name.startsWith("\"") && name.endsWith("\"") -> 1

                else -> 0
            }
            if (newHideType != 0) {
                if (newHideType == 1) {
                    // 引号标题隐藏条目：KEYWORD 去掉两端引号
                    q.keyword = q.keyword?.trim('"') ?: name.trim('"')
                }
                q.hideType = newHideType
                dao.update(q)
            }
        }
        Settings.quickSearchHideTypeMigrated = true
        quickSearchClassified = true
    }

    suspend fun getAllQuickSearch(): List<QuickSearch> {
        ensureQuickSearchClassified()
        return db.quickSearchDao().list()
    }

    // 隐藏列表全部条目（HIDE_TYPE ∈ 1..3）
    suspend fun getHideList(): List<QuickSearch> {
        ensureQuickSearchClassified()
        return db.quickSearchDao().getHideList()
    }

    // 新增隐藏条目：hideType 1=标题 2=上传者 3=标签，text 为内容
    suspend fun addHideEntry(text: String, hideType: Int) {
        ensureQuickSearchClassified()
        val dao = db.quickSearchDao()
        val position = dao.getHideList().size
        val entry = QuickSearch(
            name = text,
            hideType = hideType,
            mode = 0,
            keyword = text,
            position = position,
        )
        entry.id = dao.insert(entry)
    }

    suspend fun removeHideEntry(entry: QuickSearch) {
        val dao = db.quickSearchDao()
        dao.delete(entry)
        dao.fill(entry.position)
    }

    // ===== 隐藏列表同步（GET/POST /hide_list，Bearer 鉴权）=====

    // 拼接 {pqUrl 基址}/hide_list（兼容 /pq_galleries 结尾与裸基址）
    private fun buildHideListUrl(pqUrl: String): String = buildString {
        append(if (pqUrl.endsWith("/pq_galleries")) pqUrl.removeSuffix("/pq_galleries") else pqUrl.trimEnd('/'))
        append("/hide_list")
    }

    // 拉取服务器隐藏列表（GET /hide_list，Bearer 鉴权；失败返回 null 并 toast 提示）
    suspend fun fetchHideListFromServer(): List<HideListServerItem>? {
        val pqUrl = Settings.pqUrl ?: run {
            showToastOnMainThread("未配置优先队列地址(PQ URL)，无法同步隐藏列表")
            return null
        }
        val apiToken = Settings.apiToken
        if (apiToken.isNullOrBlank()) {
            showToastOnMainThread("未配置 API Token，无法同步隐藏列表")
            return null
        }
        return try {
            val response = ktorClient.get(buildHideListUrl(pqUrl)) {
                header("Authorization", "Bearer $apiToken")
            }
            if (response.status.value in 200..299) {
                parsePqItems<HideListServerItem>(response.body())
            } else {
                showToastOnMainThread("拉取隐藏列表失败: ${response.status}")
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showToastOnMainThread("拉取隐藏列表异常: ${e.message}")
            null
        }
    }

    // 服务器条目按 (hideType, content) 去重合并进本地 QUICK_SEARCH 隐藏条目，返回合并后的本地全量
    suspend fun mergeHideListFromServer(serverItems: List<HideListServerItem>): List<QuickSearch> {
        ensureQuickSearchClassified()
        val dao = db.quickSearchDao()
        val local = dao.getHideList()
        val localKeys = local.mapTo(HashSet()) { it.hideType to (it.keyword ?: it.name) }
        var nextPosition = (local.maxOfOrNull { it.position } ?: -1) + 1
        serverItems.forEach { item ->
            if ((item.hide_type to item.content) !in localKeys) {
                val entry = QuickSearch(
                    name = item.content,
                    hideType = item.hide_type,
                    mode = 0,
                    keyword = item.content,
                    position = nextPosition++,
                )
                entry.id = dao.insert(entry)
            }
        }
        return dao.getHideList()
    }

    // 上传本地隐藏列表全量（POST /hide_list，Bearer 鉴权；失败返回 false 并 toast 提示）
    suspend fun uploadHideList(): Boolean {
        val pqUrl = Settings.pqUrl ?: run {
            showToastOnMainThread("未配置优先队列地址(PQ URL)，无法上传隐藏列表")
            return false
        }
        val apiToken = Settings.apiToken
        if (apiToken.isNullOrBlank()) {
            showToastOnMainThread("未配置 API Token，无法上传隐藏列表")
            return false
        }
        val items = getHideList().map { HideListServerItem(it.hideType, it.keyword ?: it.name) }
        return try {
            val body = pqJson.encodeToString(HideListUploadRequest(items))
            val response = ktorClient.post(buildHideListUrl(pqUrl)) {
                method = HttpMethod.Post
                header("Authorization", "Bearer $apiToken")
                setBody(TextContent(text = body, contentType = ContentType.Application.Json))
            }
            if (response.status.value in 200..299) {
                true
            } else {
                showToastOnMainThread("上传隐藏列表失败: ${response.status}")
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showToastOnMainThread("上传隐藏列表异常: ${e.message}")
            false
        }
    }

    // 一键同步：先拉取服务器条目按 (hideType, content) 去重合并进本地，再上传本地全量（任一失败返回 false）
    suspend fun syncHideList(): Boolean {
        val serverItems = fetchHideListFromServer() ?: return false
        mergeHideListFromServer(serverItems)
        EhFilter.refreshHideList()
        return uploadHideList()
    }

    // ===== 优先队列标签缓存（pq_tag）=====

    suspend fun getAllPqTags(): List<String> = db.pqTagDao().getAll()

    // 全量替换优先队列标签缓存（拉取成功后调用）
    suspend fun replaceAllPqTags(tags: List<String>) {
        val dao = db.pqTagDao()
        dao.clear()
        dao.insertAll(tags.distinct().map { PqTag(it) })
    }

    // 拉取优先队列标签（GET {pqUrl base}/pq_tags，Bearer 鉴权；失败静默保留旧缓存）
    suspend fun fetchPqTags() {
        val pqUrl = Settings.pqUrl ?: return
        val apiToken = Settings.apiToken
        if (apiToken.isNullOrBlank()) return
        val tagsUrl = buildString {
            append(if (pqUrl.endsWith("/pq_galleries")) pqUrl.removeSuffix("/pq_galleries") else pqUrl.trimEnd('/'))
            append("/pq_tags")
        }
        try {
            val response = ktorClient.get(tagsUrl) {
                header("Authorization", "Bearer $apiToken")
            }
            if (response.status.value in 200..299) {
                val tags = pqJson.decodeFromString<List<String>>(response.body())
                replaceAllPqTags(tags)
                EhFilter.refreshPqTags()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 静默失败，保留旧缓存
        }
    }

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

    val historyLazyListExcludeFav
        get() = db.historyDao().joinListLazyExcludeFav()

    fun searchHistoryExcludeFav(keyword: String) = db.historyDao().joinListLazyExcludeFav("*$keyword*")

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

    private data class IdentityBare(val artists: Set<String>, val groups: Set<String>) {
        val required: Set<String> get() = artists + groups
        fun isEmpty() = required.isEmpty()
    }

    private fun String.bareValue() = removePrefix("_").let { if (':' in it) substringAfterLast(':') else it }

    private fun extractIdentityBareFromTags(tags: List<String>): IdentityBare {
        val artists = mutableSetOf<String>()
        val groups = mutableSetOf<String>()
        tags.forEach { raw ->
            val tag = raw.removePrefix("_")
            val sep = tag.indexOf(':')
            if (sep < 0) return@forEach
            val ns = tag.substring(0, sep)
            val value = tag.substring(sep + 1)
            when (ns) {
                "artist", "cosplayer" -> artists.add(value)
                "group" -> groups.add(value)
            }
        }
        return IdentityBare(artists, groups)
    }

    private fun BaseGalleryInfo.extractIdentityBare(): IdentityBare = extractIdentityBareFromTags(simpleTags.orEmpty())

    private fun List<String>.candidateBareSet() = buildSet {
        this@candidateBareSet.forEach { add(it.bareValue()) }
    }

    private fun BaseGalleryInfo.isFavorited() = favoriteSlot != NOT_FAVORITED

    data class MatchSummary(val totalMatching: Int, val matchingFavorited: Int) {
        val matchingUnfavorited: Int get() = totalMatching - matchingFavorited
    }

    private suspend fun computeMatchSummary(target: BaseGalleryInfo, overrideTargetTags: List<String>? = null): MatchSummary {
        val identity = overrideTargetTags
            ?.takeIf { it.isNotEmpty() }
            ?.let { extractIdentityBareFromTags(it) }
            ?: target.extractIdentityBare()
        if (identity.isEmpty()) return MatchSummary(0, 0)
        val required = identity.required
        var total = 0
        var favorited = 0
        db.historyDao().joinList().forEach {
            if (it.gid == target.gid) return@forEach
            if (it.simpleTags.orEmpty().candidateBareSet().containsAll(required)) {
                total++
                if (it.isFavorited()) favorited++
            }
        }
        return MatchSummary(total, favorited)
    }

    suspend fun countHistoryBySameArtistOrGroup(target: BaseGalleryInfo): MatchSummary = computeMatchSummary(target)

    suspend fun countHistoryBySameArtistOrGroup(target: BaseGalleryInfo, targetTagsOverride: List<String>): MatchSummary = computeMatchSummary(target, targetTagsOverride)

    data class RemovalResult(val removed: Int, val totalMatching: Int) {
        val untouchedMatching: Int get() = totalMatching - removed
    }

    private suspend fun computeRemoval(target: BaseGalleryInfo, overrideTargetTags: List<String>? = null): RemovalResult {
        val identity = overrideTargetTags
            ?.takeIf { it.isNotEmpty() }
            ?.let { extractIdentityBareFromTags(it) }
            ?: target.extractIdentityBare()
        if (identity.isEmpty()) return RemovalResult(0, 0)
        val required = identity.required
        var totalMatching = 0
        val toDelete = mutableListOf<Long>()
        db.historyDao().joinList().forEach {
            if (it.gid == target.gid) return@forEach
            if (it.simpleTags.orEmpty().candidateBareSet().containsAll(required)) {
                totalMatching++
                if (it.isFavorited()) toDelete.add(it.gid)
            }
        }
        toDelete.chunked(500).forEach { db.historyDao().deleteByKeyRange(it) }
        return RemovalResult(removed = toDelete.size, totalMatching = totalMatching)
    }

    suspend fun removeHistoryBySameArtistOrGroup(target: BaseGalleryInfo): RemovalResult = computeRemoval(target)

    suspend fun removeHistoryBySameArtistOrGroup(target: BaseGalleryInfo, targetTagsOverride: List<String>): RemovalResult = computeRemoval(target, targetTagsOverride)

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
