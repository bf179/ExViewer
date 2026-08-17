# 历史界面：隐藏已收藏画廊 + 长按移除同作者/同社团画廊 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为历史界面新增设置开关（隐藏已收藏画廊，仅显示过滤）和长按操作（移除同作者/同社团画廊，确认后批量删除并提示数量）。

**Architecture:** 数据层改动集中在 HistoryDao（新增过滤分页查询、全量 join 查询、批量删除）与 EhDB（新增委托与业务方法，沿用 `artist:`/`cosplayer:`/`group:` 标签前缀匹配）。UI 层：HistoryScreen 按设置开关切换 Paging 数据源；长按通过给通用 `doGalleryInfoAction` 增加可选 `extraAction` 参数追加菜单项，回调中执行"统计数量 → 确认弹窗 → 删除 → 提示"。

**Tech Stack:** Kotlin, Jetpack Compose, Room, Paging 3, DataStore Preferences

**设计文档:** `docs/superpowers/specs/2026-08-15-history-hide-favorites-remove-same-artist-group-design.md`

---

### Task 1: 新增设置项 hideFavInHistory

**Files:**
- Modify: `app/src/main/kotlin/com/hippo/ehviewer/Settings.kt:186`（hideFav 附近）
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 在 Settings.kt 中新增设置项**

在 `var hideFav by boolPref("hide_fav", true)` 下方新增：

```kotlin
var hideFavInHistory by boolPref("hide_fav_in_history", false)
```

- [ ] **Step 2: 在 strings.xml 新增字符串**

在 `values/strings.xml` 中新增：

```xml
<string name="settings_hide_fav_in_history">[Self] 隐藏历史中已收藏画廊</string>
<string name="settings_hide_fav_in_history_summary">在历史列表中隐藏已收藏（本地+云端）的画廊</string>
<string name="remove_same_artist_group">移除同作者/同社团画廊</string>
<string name="remove_same_artist_group_message">将移除历史中与该作者/社团相关的 %d 个画廊，是否继续？</string>
<string name="removed_n_galleries">已移除 %d 个画廊</string>
<string name="no_matching_gallery_found">未找到匹配的画廊</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/hippo/ehviewer/Settings.kt app/src/main/res/values/strings.xml
git commit -m "feat: add hideFavInHistory setting and related strings"
```

---

### Task 2: HistoryDao 新增查询与批量删除

**Files:**
- Modify: `app/src/main/kotlin/com/hippo/ehviewer/dao/HistoryDao.kt`

- [ ] **Step 1: 新增过滤分页查询、全量 join 查询、批量删除**

在 `joinListLazy(title)` 之后新增：

```kotlin
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
```

> 说明：`FAVORITE_SLOT = -2` 即 `= NOT_FAVORITED`，仅保留未收藏记录，从而隐藏已收藏（本地 -1 与云端 0..9）。`NOT_FAVORITED = -2` 定义于 `GalleryInfo.Companion`，DAO 中直接写字面量（Room 查询不支持引用 Kotlin 常量）。

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/hippo/ehviewer/dao/HistoryDao.kt
git commit -m "feat: add history dao queries for hide fav and range delete"
```

---

### Task 3: EhDB 新增委托与匹配/删除业务方法

**Files:**
- Modify: `app/src/main/kotlin/com/hippo/ehviewer/EhDB.kt`

- [ ] **Step 1: 新增过滤委托**

在 `val historyLazyList` / `fun searchHistory` 附近新增：

```kotlin
val historyLazyListExcludeFav
    get() = db.historyDao().joinListLazyExcludeFav()

fun searchHistoryExcludeFav(keyword: String) = db.historyDao().joinListLazyExcludeFav("*$keyword*")
```

- [ ] **Step 2: 新增匹配与删除业务方法**

在 EhDB object 内新增（放在 `deleteHistoryInfo` / `clearHistoryInfo` 附近）：

```kotlin
private fun BaseGalleryInfo.artistGroupKeys(): Set<String> = buildSet {
    simpleTags.orEmpty().forEach { tag ->
        when {
            tag.startsWith("artist:") || tag.startsWith("cosplayer:") || tag.startsWith("group:") -> add(tag)
        }
    }
}

suspend fun countHistoryBySameArtistOrGroup(target: BaseGalleryInfo): Int {
    val keys = target.artistGroupKeys()
    if (keys.isEmpty()) return 0
    return db.historyDao().joinList().count {
        it.gid != target.gid && it.simpleTags.orEmpty().any(keys::contains)
    }
}

suspend fun removeHistoryBySameArtistOrGroup(target: BaseGalleryInfo): Int {
    val keys = target.artistGroupKeys()
    if (keys.isEmpty()) return 0
    val toDelete = db.historyDao().joinList()
        .filter { it.gid != target.gid && it.simpleTags.orEmpty().any(keys::contains) }
        .map { it.gid }
    if (toDelete.isNotEmpty()) {
        db.historyDao().deleteByKeyRange(toDelete)
    }
    return toDelete.size
}
```

- [ ] **Step 3: 确认 import**

确保 `BaseGalleryInfo` 已 import（EhDB.kt 已有 `import com.hippo.ehviewer.client.data.BaseGalleryInfo`）。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/hippo/ehviewer/EhDB.kt
git commit -m "feat: add history hide fav delegates and same artist/group removal"
```

---

### Task 4: doGalleryInfoAction 增加 extraAction 参数

**Files:**
- Modify: `app/src/main/kotlin/com/hippo/ehviewer/ui/CommonOperations.kt:293-351`

- [ ] **Step 1: 修改函数签名与菜单逻辑**

将 `doGalleryInfoAction` 修改为：

```kotlin
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

            extraAction != null && selected == baseCount -> withIOContext { onExtraAction() }
        }
        true
    }
}
```

> 说明：`baseCount` 即原有菜单项数（未下载 3 项、已下载 4 项），新增项索引恒等于 `baseCount`。原有 when 分支语义完全不变。

- [ ] **Step 2: 确认 import**

在文件头部 import 区新增（若已存在则跳过）：

```kotlin
import androidx.compose.ui.graphics.vector.ImageVector
```

> 说明：`awaitSelectItemWithIcon(items, ...)` 中 items 类型为 `List<Pair<ImageVector, Int>>`，函数签名显式引用 `ImageVector` 需要该 import。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/hippo/ehviewer/ui/CommonOperations.kt
git commit -m "feat: support extra action in gallery info action menu"
```

---

### Task 5: 新增确认删除函数

**Files:**
- Modify: `app/src/main/kotlin/com/hippo/ehviewer/ui/CommonOperations.kt`（新增函数，放在 `doGalleryInfoAction` 之后）

- [ ] **Step 1: 新增函数**

```kotlin
context(DialogState, Context, DestinationsNavigator)
suspend fun removeHistoryBySameArtistOrGroupWithConfirm(info: BaseGalleryInfo) {
    val count = withIOContext { EhDB.countHistoryBySameArtistOrGroup(info) }
    if (count == 0) {
        showTip(R.string.no_matching_gallery_found)
        return
    }
    awaitConfirmationOrCancel(
        confirmText = R.string.remove,
        text = { Text(stringResource(R.string.remove_same_artist_group_message, count)) },
    )
    val removed = withIOContext { EhDB.removeHistoryBySameArtistOrGroup(info) }
    if (removed > 0) {
        showTip(appCtx.getString(R.string.removed_n_galleries, removed))
    } else {
        showTip(R.string.no_matching_gallery_found)
    }
}
```

> 说明：用户点"取消"时 `awaitConfirmationOrCancel` 内部 `cont.cancel()` 会抛 CancellationException 中断后续代码，无需判断返回值。`stringResource(id, count)` 使用 Android 资源占位符格式化（`%d`）；`showTip` 无格式化重载，故带参数提示用 `appCtx.getString(R.string.removed_n_galleries, removed)`（`appCtx` 来自 `splitties.init.appCtx`，CommonOperations.kt 已 import）。

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/hippo/ehviewer/ui/CommonOperations.kt
git commit -m "feat: add remove same artist/group history confirm flow"
```

---

### Task 6: HistoryScreen 接入设置开关与长按菜单

**Files:**
- Modify: `app/src/main/kotlin/com/hippo/ehviewer/ui/screen/HistoryScreen.kt`

- [ ] **Step 1: 按设置切换 Paging 数据源**

将 `historyData` 定义修改为（注意 `hideFavInHistory` 通过 `collectAsState` 读取并驱动 Paging 重建）：

```kotlin
val hideFavInHistory by Settings.hideFavInHistory.collectAsState()
val historyData = rememberInVM {
    Pager(config = PagingConfig(pageSize = 20, jumpThreshold = 40)) {
        when {
            keyword.isNotEmpty() ->
                if (hideFavInHistory) EhDB.searchHistoryExcludeFav(keyword)
                else EhDB.searchHistory(keyword)

            hideFavInHistory -> EhDB.historyLazyListExcludeFav
            else -> EhDB.historyLazyList
        }
    }.flow.map { data ->
        val favCat = Settings.favCat
        data.map {
            it.apply { favoriteName = favCat.getOrNull(favoriteSlot) }
        }
    }.cachedIn(viewModelScope)
}.collectAsLazyPagingItems()
```

> 说明：`hideFavInHistory` 在 `rememberInVM` 之外读取，开关变化会触发重组与 Paging 重建；搜索时同样生效。`FavouriteStatusRouter.Observe(historyData)` 保持不变。

- [ ] **Step 2: 修改长按回调**

将 `onLongClick` 修改为：

```kotlin
onLongClick = {
    launch {
        doGalleryInfoAction(
            info,
            extraAction = Icons.Default.DeleteSweep to R.string.remove_same_artist_group,
        ) {
            removeHistoryBySameArtistOrGroupWithConfirm(info)
        }
    }
},
```

- [ ] **Step 3: 新增 import**

在文件头部 import 区新增：

```kotlin
import androidx.compose.material.icons.filled.DeleteSweep
import com.hippo.ehviewer.ui.removeHistoryBySameArtistOrGroupWithConfirm
```

> 说明：`doGalleryInfoAction` 已通过 `com.hippo.ehviewer.ui.doGalleryInfoAction` import（现有第 56 行）。`Icons.Default.DeleteSweep` 若需确认图标存在（`androidx.compose.material.icons.filled.DeleteSweep` 为标准图标）。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/hippo/ehviewer/ui/screen/HistoryScreen.kt
git commit -m "feat: wire hide fav switch and long-press removal in history screen"
```

---

### Task 7: AdvancedScreen 新增设置开关

**Files:**
- Modify: `app/src/main/kotlin/com/hippo/ehviewer/ui/settings/AdvancedScreen.kt:139-143`（hideFav 开关后）

- [ ] **Step 1: 新增 SwitchPreference**

在 `[Self] 隐藏已收藏画廊` 开关之后新增：

```kotlin
SwitchPreference(
    title = stringResource(id = R.string.settings_hide_fav_in_history),
    summary = stringResource(id = R.string.settings_hide_fav_in_history_summary),
    value = Settings::hideFavInHistory,
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/hippo/ehviewer/ui/settings/AdvancedScreen.kt
git commit -m "feat: add hide fav in history switch in advanced settings"
```

---

### Task 8: 编译验证

**Files:**
- 无（仅验证）

- [ ] **Step 1: 编译调试版本**

Run: `./gradlew :app:compileDebugKotlin -x lint`
Expected: `BUILD SUCCESSFUL`

> 若沙盒无 Android SDK 无法编译，退化为人工代码审查：确认 6 个任务的所有引用（函数名、import、资源 id）一致。

- [ ] **Step 2: 手工功能验证清单**

1. 设置 → 高级 → 开启「隐藏历史中已收藏画廊」→ 历史列表中已收藏（本地收藏一个 + 云端收藏一个）的画廊消失；关闭后恢复
2. 长按某画廊 → 菜单出现「移除同作者/同社团画廊」→ 选择后弹出「将移除历史中与该作者/社团相关的 N 个画廊」→ 确认 → 提示「已移除 N 个画廊」，被长按画廊保留，列表刷新
3. 长按无 artist/cosplayer/group 标签的画廊 → 提示「未找到匹配的画廊」
4. 搜索关键词状态下长按删除 → 删除基于全量历史，提示数量正确，列表刷新
5. 通用画廊列表（非历史）长按 → 菜单不含新增项，行为不变
