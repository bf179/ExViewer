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
package com.hippo.ehviewer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.ExlApiRequest
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.dao.BatchFavTask
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.downloadDir
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.download.tempDownloadDir
import com.hippo.ehviewer.sendExlApiRequest
import com.hippo.ehviewer.showToastOnMainThread
import com.hippo.ehviewer.ui.destinations.ReaderScreenDestination
import com.hippo.ehviewer.ui.reader.ReaderScreenArgs
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LabeledCheckbox
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.isAtLeastT
import com.hippo.ehviewer.util.mapToLongArray
import com.hippo.ehviewer.util.requestPermission
import com.hippo.ehviewer.util.restartApplication
import com.hippo.ehviewer.util.toEpochMillis
import com.hippo.ehviewer.util.toLocalDateTime
import com.hippo.files.delete
import com.hippo.files.exists
import com.hippo.files.isDirectory
import com.hippo.files.write
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path
import splitties.init.appCtx

private fun removeNoMediaFile(downloadDir: Path) {
    (downloadDir / ".nomedia").delete()
}

private fun ensureNoMediaFile(downloadDir: Path) {
    (downloadDir / ".nomedia").apply { if (!exists()) write {} }
}

private val lck = Mutex()

suspend fun keepNoMediaFileStatus(downloadDir: Path = downloadLocation) {
    if (downloadDir.isDirectory) {
        lck.withLock {
            if (Settings.mediaScan) {
                removeNoMediaFile(downloadDir)
            } else {
                ensureNoMediaFile(downloadDir)
            }
        }
    }
}

fun getFavoriteIcon(favorited: Boolean) = if (favorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder

suspend fun DialogState.startDownload(
    context: Context,
    forceDefault: Boolean,
    vararg galleryInfos: BaseGalleryInfo,
) = with(context) {
    if (isAtLeastT) {
        requestPermission(Manifest.permission.POST_NOTIFICATIONS)
    }
    val (toStart, toAdd) = galleryInfos.partition { DownloadManager.containDownloadInfo(it.gid) }
    if (toStart.isNotEmpty()) {
        val intent = Intent(context, DownloadService::class.java)
        intent.action = DownloadService.ACTION_START_RANGE
        val list = toStart.mapToLongArray(GalleryInfo::gid)
        intent.putExtra(DownloadService.KEY_GID_LIST, list)
        ContextCompat.startForegroundService(context, intent)
    }
    if (toAdd.isEmpty()) {
        return with(findActivity<MainActivity>()) {
            showTip(R.string.added_to_download_list)
        }
    }
    var justStart = forceDefault
    var label: String? = null
    // Get default download label
    if (!justStart && Settings.hasDefaultDownloadLabel) {
        label = Settings.defaultDownloadLabel
        justStart = label == null || DownloadManager.containLabel(label)
    }
    // If there is no other label, just use null label
    if (!justStart && DownloadManager.labelList.isEmpty()) {
        justStart = true
        label = null
    }
    if (justStart) {
        // Got default label
        for (gi in toAdd) {
            val intent = Intent(context, DownloadService::class.java)
            intent.action = DownloadService.ACTION_START
            intent.putExtra(DownloadService.KEY_LABEL, label)
            intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi)
            ContextCompat.startForegroundService(context, intent)
        }
        // Notify
        with(findActivity<MainActivity>()) {
            showTip(R.string.added_to_download_list)
        }
    } else {
        // Let use chose label
        val list = DownloadManager.labelList
        val items = buildList {
            add(getString(R.string.default_download_label_name))
            list.forEach {
                add(it.label)
            }
        }
        val (selected, checked) = awaitSelectItemWithCheckBox(
            items,
            title = R.string.download,
            checkBoxText = R.string.remember_download_label,
        )
        val label1 = if (selected == 0) null else items[selected].takeIf { DownloadManager.containLabel(it) }
        // Start download
        for (gi in toAdd) {
            val intent = Intent(context, DownloadService::class.java)
            intent.action = DownloadService.ACTION_START
            intent.putExtra(DownloadService.KEY_LABEL, label1)
            intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi)
            ContextCompat.startForegroundService(context, intent)
        }
        // Save settings
        if (checked) {
            Settings.hasDefaultDownloadLabel = true
            Settings.defaultDownloadLabel = label1
        } else {
            Settings.hasDefaultDownloadLabel = false
        }
        with(context.findActivity<MainActivity>()) {
            showTip(R.string.added_to_download_list)
        }
    }
}

suspend fun DialogState.modifyFavorites(galleryInfo: BaseGalleryInfo, showFavslotList: Boolean = false, showSuccessToast: Boolean = true): Boolean {
    val localFavorited = EhDB.containLocalFavorites(galleryInfo.gid)
    if (Settings.hasSignedIn.value) {
        val isFavorited = galleryInfo.favoriteSlot != NOT_FAVORITED
        val defaultFavSlot = Settings.defaultFavSlot
        if (showFavslotList || (defaultFavSlot == -2)) {
            val localFav = getFavoriteIcon(localFavorited) to appCtx.getString(R.string.local_favorites)
            val cloudFav = Settings.favCat.mapIndexed { index, name ->
                getFavoriteIcon(galleryInfo.favoriteSlot == index) to name
            }
            val items = buildList {
                if (isFavorited) {
                    val remove = Icons.Default.HeartBroken to appCtx.getString(R.string.remove_from_favourites)
                    add(remove)
                }
                add(localFav)
                addAll(cloudFav)
            }
            if (galleryInfo.favoriteSlot >= 0 && galleryInfo.favoriteNote == null) {
                galleryInfo.favoriteNote = bgWork { EhEngine.getFavoriteNote(galleryInfo.gid, galleryInfo.token) }
            }
            val (slot, note) = awaitSelectItemWithIconAndTextField(
                items,
                title = R.string.add_favorites_dialog_title,
                hint = R.string.favorite_note,
                initialNote = galleryInfo.favoriteNote.orEmpty(),
                maxChar = MAX_FAVNOTE_CHAR,
            )
            return doModifyFavorites(galleryInfo, if (isFavorited) slot - 2 else slot - 1, localFavorited, note)
        } else {
            return doModifyFavorites(galleryInfo, if (isFavorited) NOT_FAVORITED else defaultFavSlot, localFavorited, showSuccessToast = showSuccessToast)
        }
    } else {
        return doModifyFavorites(galleryInfo, LOCAL_FAVORITED, localFavorited)
    }
}

private suspend fun doModifyFavorites(
    galleryInfo: BaseGalleryInfo,
    slot: Int = NOT_FAVORITED,
    localFavorited: Boolean = true,
    note: String = "",
    showSuccessToast: Boolean = true,
) = with(galleryInfo) {
    // 收藏原子化（远端先行）：syncFav 开启且 sapi 已配置时，先向远端 /exl 同步对应操作，
    // 远端成功（201）后才执行网站变更与本地数据库操作；远端失败则本次操作整体不生效
    if (!syncRemoteFirst(slot, localFavorited)) return@with false
    val add = when (slot) {
        NOT_FAVORITED -> { // Remove from cloud favorites first
            if (favoriteSlot > LOCAL_FAVORITED) {
                EhEngine.modifyFavorites(gid, token)
                favoriteSlot = if (localFavorited) LOCAL_FAVORITED else NOT_FAVORITED
                favoriteName = null
                favoriteNote = null
            } else {
                EhDB.removeLocalFavorites(galleryInfo)
                favoriteSlot = NOT_FAVORITED
            }
            false
        }

        LOCAL_FAVORITED -> {
            if (localFavorited) {
                EhDB.removeLocalFavorites(galleryInfo)
            } else {
                EhDB.putLocalFavorites(galleryInfo, showSuccessToast)
            }
            // Keep cloud favorite slot
            if (favoriteSlot == NOT_FAVORITED) {
                favoriteSlot = LOCAL_FAVORITED
            } else if (favoriteSlot == LOCAL_FAVORITED) {
                favoriteSlot = NOT_FAVORITED
            }
            !localFavorited
        }

        in 0..9 -> {
            EhEngine.modifyFavorites(gid, token, slot, note, showSuccessToast)
            favoriteSlot = slot
            favoriteName = Settings.favCat[slot]
            favoriteNote = note
            true
        }

        else -> throw EhException("Invalid favorite slot!")
    }
    FavouriteStatusRouter.notify(galleryInfo)
    add
}

// 收藏远端先行：开启 syncFav 时先调用 sendExlApiRequest 直接发送（带 token）；
// 返回 true 表示远端已确认（或无需远端同步），可继续本地/网站变更；false 表示远端失败应整体中止
private suspend fun BaseGalleryInfo.syncRemoteFirst(slot: Int, localFavorited: Boolean): Boolean {
    if (!Settings.syncFav) return true
    val sapi = Settings.sapiUrl
    if (sapi.isNullOrBlank()) return true
    val (op, favSlot) = when (slot) {
        NOT_FAVORITED -> if (favoriteSlot > LOCAL_FAVORITED) "favdel" to -1 else "del" to favoriteSlot
        LOCAL_FAVORITED -> if (localFavorited) "del" to LOCAL_FAVORITED else "add" to LOCAL_FAVORITED
        in 0..9 -> slot.toString() to slot
        else -> return true
    }
    val exlar = ExlApiRequest(
        user = "loliwant",
        gid = gid,
        token = token,
        favoriteslot = favSlot,
        op = op,
    )
    // 直接发送（不带 outbox 入队语义），失败则不继续本地/网站操作
    return sendExlApiRequest(exlar, sapi, showSuccessToast = false)
}

suspend fun removeFromFavorites(galleryInfo: BaseGalleryInfo) = doModifyFavorites(
    galleryInfo = galleryInfo,
    localFavorited = EhDB.containLocalFavorites(galleryInfo.gid),
)

// ===== 批量收藏（app 级后台执行 + Room 进度持久化）=====

// 批量收藏任务状态
const val BATCH_STATUS_RUNNING = "运行中"
const val BATCH_STATUS_DONE = "完成"
const val BATCH_STATUS_FAILED = "失败"
const val BATCH_STATUS_INTERRUPTED = "中断"

// 全局批量收藏任务句柄：app 级作用域执行，离开页面/点进画廊不中断
object BatchFavController {
    @Volatile
    var job: Job? = null
}

// 批量收藏逐项处理：仅执行"添加"语义（已收藏判定在调用方完成）
suspend fun batchModifyFavorite(galleryInfo: BaseGalleryInfo, showSuccessToast: Boolean = false) {
    val localFavorited = EhDB.containLocalFavorites(galleryInfo.gid)
    val slot = if (!Settings.hasSignedIn.value) {
        LOCAL_FAVORITED
    } else {
        val defaultFavSlot = Settings.defaultFavSlot
        // 批量场景不弹"每次询问"对话框，回退到本地收藏
        if (defaultFavSlot == -2) LOCAL_FAVORITED else defaultFavSlot
    }
    doModifyFavorites(galleryInfo, slot, localFavorited, showSuccessToast = showSuccessToast)
}

// 启动批量收藏任务（app 级作用域，不随 UI 取消）
fun startBatchFav(items: List<BaseGalleryInfo>) {
    val job = EhApplication.appScope.launch {
        val dao = EhDB.batchFavTaskDao()
        val now = System.currentTimeMillis()
        val task = BatchFavTask(status = BATCH_STATUS_RUNNING, total = items.size, done = 0, createdAt = now, updatedAt = now)
        val id = dao.insert(task)
        runBatchFavLoop(items, id, items.size, task.createdAt)
    }
    BatchFavController.job = job
}

// 续跑已中断的批量任务：重置进度后重新执行（已收藏者跳过并计入成功）
fun resumeBatchFav(items: List<BaseGalleryInfo>, task: BatchFavTask) {
    val job = EhApplication.appScope.launch {
        val dao = EhDB.batchFavTaskDao()
        dao.update(task.copy(status = BATCH_STATUS_RUNNING, done = 0, updatedAt = System.currentTimeMillis()))
        runBatchFavLoop(items, task.id, task.total, task.createdAt)
    }
    BatchFavController.job = job
}

// 手动终止：仅取消剩余任务，已处理进度保留（下次可续跑）
fun cancelBatchFav() {
    BatchFavController.job?.cancel(CancellationException("手动中止"))
}

private suspend fun runBatchFavLoop(items: List<BaseGalleryInfo>, taskId: Long, total: Int, createdAt: Long) {
    val dao = EhDB.batchFavTaskDao()
    var successCount = 0
    val defaultFavSlot = Settings.defaultFavSlot
    val slowfav = defaultFavSlot != -1
    suspend fun updateTask(status: String, done: Int) {
        dao.update(BatchFavTask(id = taskId, status = status, total = total, done = done, createdAt = createdAt, updatedAt = System.currentTimeMillis()))
    }
    try {
        items.forEach { galleryInfo ->
            currentCoroutineContext().ensureActive()
            // 已收藏判定：实时 favoriteSlot + 本地双查，已收藏跳过并计入成功，避免误删
            val isFavorited = galleryInfo.favoriteSlot != NOT_FAVORITED || EhDB.containLocalFavorites(galleryInfo.gid)
            if (!isFavorited) {
                runSuspendCatching {
                    batchModifyFavorite(galleryInfo)
                }.onSuccess { successCount++ }
                if (slowfav) {
                    delay(4000)
                }
                delay(100) // 每个画廊处理完延迟100毫秒
            } else {
                successCount++
            }
            updateTask(BATCH_STATUS_RUNNING, successCount)
        }
        updateTask(BATCH_STATUS_DONE, successCount)
        showToastOnMainThread("成功收藏 $successCount/$total 个画廊")
    } catch (e: CancellationException) {
        // 手动终止：保留已处理进度，供下次续跑（NonCancellable 确保取消后仍能写入 Room）
        withContext(NonCancellable) {
            updateTask(BATCH_STATUS_INTERRUPTED, successCount)
        }
        showToastOnMainThread("任务中止，已处理收藏 $successCount 个画廊")
        throw e
    } catch (e: Exception) {
        updateTask(BATCH_STATUS_FAILED, successCount)
        showToastOnMainThread("批量收藏失败: ${e.message}")
    }
}

fun DestinationsNavigator.navToReader(info: BaseGalleryInfo, page: Int = -1) = navToReader(ReaderScreenArgs.Gallery(info, page))

fun DestinationsNavigator.navToReader(uri: Uri) = navToReader(ReaderScreenArgs.Archive(uri))

private fun DestinationsNavigator.navToReader(args: ReaderScreenArgs) = navigate(ReaderScreenDestination(args)) { launchSingleTop = true }

context(DialogState, Context, DestinationsNavigator)
suspend fun doGalleryInfoAction(
    info: BaseGalleryInfo,
    extraAction: Pair<ImageVector, Int>? = null,
    onExtraAction: suspend () -> Unit = {},
) {
    val downloaded = DownloadManager.getDownloadState(info.gid) != DownloadInfo.STATE_INVALID
    val favorited = info.favoriteSlot != NOT_FAVORITED
    val baseCount = if (downloaded) 4 else 3
    val items = buildList {
        add(Icons.AutoMirrored.Default.MenuBook to R.string.read)
        val download = if (downloaded) {
            Icons.Default.Delete to R.string.delete_downloads
        } else {
            Icons.Default.Download to R.string.download
        }
        add(download)
        val favorite = if (favorited) {
            Icons.Default.HeartBroken to R.string.remove_from_favourites
        } else {
            Icons.Default.Favorite to R.string.add_to_favourites
        }
        add(favorite)
        if (downloaded) {
            add(Icons.AutoMirrored.Default.DriveFileMove to R.string.download_move_dialog_title)
        }
        extraAction?.let { add(it) }
    }
    val selected = awaitSelectItemWithIcon(items, EhUtils.getSuitableTitle(info))
    with(findActivity<MainActivity>()) {
        when (selected) {
            in 0 until baseCount -> when (selected) {
                0 -> {
                    EhDB.putHistoryInfo(info)
                    navToReader(info)
                }

                1 -> withUIContext {
                    if (downloaded) {
                        confirmRemoveDownload(info)
                    } else {
                        startDownload(this@with, false, info)
                    }
                }

                2 -> if (favorited) {
                    runSuspendCatching {
                        removeFromFavorites(info)
                        showTip(R.string.remove_from_favorite_success)
                    }.onFailure {
                        showTip(R.string.remove_from_favorite_failure)
                    }
                } else {
                    runSuspendCatching {
                        modifyFavorites(info)
                        showTip(R.string.add_to_favorite_success)
                    }.onFailure {
                        showTip(R.string.add_to_favorite_failure)
                    }
                }

                3 -> showMoveDownloadLabel(info)
            }

            baseCount -> if (extraAction != null) {
                withIOContext { onExtraAction() }
            }
        }
        true
    }
}

context(DialogState, Context, DestinationsNavigator)
suspend fun removeHistoryBySameArtistOrGroupWithConfirm(info: BaseGalleryInfo) {
    val activity = findActivity<MainActivity>()
    val nsTags = runSuspendCatching {
        withIOContext {
            EhEngine.getGalleryNamespacedTags(info.gid, info.token).takeIf { it.isNotEmpty() }
        }
    }.getOrNull()
    val summary = runSuspendCatching {
        withIOContext {
            if (nsTags != null) {
                EhDB.countHistoryBySameArtistOrGroup(info, nsTags)
            } else {
                EhDB.countHistoryBySameArtistOrGroup(info)
            }
        }
    }.getOrNull()
    if (summary == null) {
        activity.showTip(R.string.remove_history_failed)
        return
    }
    if (summary.totalMatching == 0) {
        activity.showTip(R.string.no_matching_gallery_found)
        return
    }
    if (summary.matchingFavorited == 0) {
        activity.showTip(appCtx.getString(R.string.no_favorited_matching_gallery, summary.totalMatching))
        return
    }
    awaitConfirmationOrCancel(
        confirmText = R.string.remove,
        text = {
            Text(
                stringResource(
                    id = R.string.remove_same_artist_group_message,
                    summary.totalMatching,
                    summary.matchingFavorited,
                    summary.matchingUnfavorited,
                ),
            )
        },
    )
    val result = runSuspendCatching {
        withIOContext {
            if (nsTags != null) {
                EhDB.removeHistoryBySameArtistOrGroup(info, nsTags)
            } else {
                EhDB.removeHistoryBySameArtistOrGroup(info)
            }
        }
    }.getOrNull()
    if (result == null) {
        activity.showTip(R.string.remove_history_failed)
    } else if (result.removed > 0) {
        activity.showTip(appCtx.getString(R.string.removed_n_of_matched_galleries, result.removed, result.totalMatching))
    } else if (result.totalMatching > 0) {
        activity.showTip(appCtx.getString(R.string.no_favorited_matching_gallery, result.totalMatching))
    } else {
        activity.showTip(R.string.no_matching_gallery_found)
    }
}

private const val MAX_FAVNOTE_CHAR = 200

private suspend fun DialogState.confirmRemoveDownload(text: String): Boolean {
    val checked = awaitResult(
        initial = Settings.removeImageFiles,
        title = R.string.download_remove_dialog_title,
    ) {
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LabeledCheckbox(
                modifier = Modifier.fillMaxWidth(),
                checked = expectedValue,
                onCheckedChange = { expectedValue = it },
                label = stringResource(id = R.string.download_remove_dialog_check_text),
                indication = null,
            )
        }
    }
    Settings.removeImageFiles = checked
    return checked
}

suspend fun DialogState.confirmRemoveDownload(info: GalleryInfo) {
    val text = appCtx.getString(R.string.download_remove_dialog_message, EhUtils.getSuitableTitle(info))
    val checked = confirmRemoveDownload(text)
    withIOContext {
        DownloadManager.deleteDownload(info.gid, checked)
    }
}

suspend fun DialogState.confirmRemoveDownloadRange(list: Collection<DownloadInfo>) {
    val text = appCtx.getString(R.string.download_remove_dialog_message_2, list.size)
    val checked = confirmRemoveDownload(text)
    withIOContext {
        // Delete
        DownloadManager.deleteRangeDownload(list.mapToLongArray(DownloadInfo::gid))
        // Delete image files
        if (checked) {
            list.forEach { info ->
                // Delete file
                info.downloadDir?.delete()
                info.tempDownloadDir?.delete()
                // Remove download path
                EhDB.removeDownloadDirname(info.gid)
            }
        }
    }
}

suspend fun DialogState.showMoveDownloadLabel(info: GalleryInfo) {
    val defaultLabel = appCtx.getString(R.string.default_download_label_name)
    val labels = buildList {
        add(defaultLabel)
        DownloadManager.labelList.forEach {
            add(it.label)
        }
    }
    val selected = awaitSelectItem(labels, R.string.download_move_dialog_title)
    val downloadInfo = DownloadManager.getDownloadInfo(info.gid) ?: return
    val label = if (selected == 0) null else labels[selected]
    DownloadManager.changeLabel(listOf(downloadInfo), label)
}

suspend fun DialogState.showMoveDownloadLabelList(list: Collection<DownloadInfo>): String? {
    val defaultLabel = appCtx.getString(R.string.default_download_label_name)
    val labels = buildList {
        add(defaultLabel)
        DownloadManager.labelList.forEach {
            add(it.label)
        }
    }
    val selected = awaitSelectItem(labels, R.string.download_move_dialog_title)
    val label = if (selected == 0) null else labels[selected]
    DownloadManager.changeLabel(list, label)
    return label
}

suspend fun DialogState.awaitSelectDate(): String? {
    val initial = LocalDate(2007, 3, 21)
    val yesterday = Clock.System.todayIn(TimeZone.UTC).minus(1, DateTimeUnit.DAY)
    val initialMillis = initial.toEpochMillis()
    val yesterdayMillis = yesterday.toEpochMillis()
    val dateRange = initialMillis..yesterdayMillis
    val dateMillis = awaitSelectDate(
        title = R.string.go_to,
        yearRange = initial.year..yesterday.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in dateRange
        },
    )
    val date = dateMillis?.run { toLocalDateTime().date.toString() }
    return date
}

context(Context)
suspend fun DialogState.showRestartDialog() {
    awaitConfirmationOrCancel {
        Text(stringResource(R.string.settings_restart))
    }
    restartApplication()
}
