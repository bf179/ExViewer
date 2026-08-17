# 历史界面：隐藏已收藏画廊 + 长按移除同作者/同社团画廊 设计文档

日期：2026-08-15
项目：bf179/ExViewer（EHViewer fork）

## 1. 需求概述

为阅读历史界面（HistoryScreen）新增两个功能：

1. **设置开关**：在设置中配置"隐藏历史列表中已收藏的画廊"（包含本地收藏和云端收藏），仅影响显示，不删除数据。
2. **长按操作**：长按历史列表中的某个画廊，在弹出的操作菜单中选择"移除同作者/同社团画廊"，将历史记录中与该画廊具有相同 `artist:` / `cosplayer:` / `group:` 标签的其他画廊从 HISTORY 表中永久删除（保留被长按的自身），操作完成后提示删除的数量。

### 已确认的决策

| 决策点 | 结论 |
|---|---|
| "移除"语义 | 永久删除 HISTORY 记录（数据库级） |
| 匹配范围 | 整个历史列表（不分页限制） |
| 设置中"隐藏"语义 | 仅显示时过滤，数据保留 |
| 自身画廊 | 保留，不删除 |
| 删除提示 | 显示删除了多少个画廊 |
| cosplayer 标签 | 与 artist 归为同类，参与匹配 |
| 实现方案 | 方案 A：SQL 层过滤 + DAO 批量删除 |

## 2. 现状分析

### 2.1 历史数据流

- [HistoryScreen.kt](app/src/main/kotlin/com/hippo/ehviewer/ui/screen/HistoryScreen.kt) 使用 `Pager` + `collectAsLazyPagingItems` 展示。
- 数据源来自 [HistoryDao.kt](app/src/main/kotlin/com/hippo/ehviewer/dao/HistoryDao.kt)：
  - `joinListLazy()`：`SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC`，返回 `PagingSource<Int, BaseGalleryInfo>`
  - `joinListLazy(title)`：同上，带 FTS 标题搜索
- 每个条目 `favoriteSlot` 字段来自 GALLERIES 表，已同步本地/云端收藏状态。

### 2.2 收藏状态表示（GalleryInfo.kt）

- `NOT_FAVORITED = -2`：未收藏
- `LOCAL_FAVORITED = -1`：本地收藏
- `0..9`：云端收藏夹

GALLERIES 表的 `FAVORITE_SLOT` 列保存上述值；本地收藏另有 `LOCAL_FAVORITES` 表，但 `favoriteSlot` 会在 `putLocalFavorites` / `FavouriteStatusRouter` 中同步。因此**判断已收藏只需 `FAVORITE_SLOT != -2`**。

### 2.3 标签格式（GalleryGroup.kt）

`simpleTags: List<String>`，元素形如 `"artist:xxx"`、`"cosplayer:xxx"`、`"group:xxx"`。
现有工具函数（`extractClusterKey`）已将 `artist:`/`cosplayer:` 归为 artist 类、`group:` 归为 group 类。

### 2.4 现有设置项模式

[Settings.kt](app/src/main/kotlin/com/hippo/ehviewer/Settings.kt) 使用 `boolPref(key, default)` 定义开关，如 `var hideFav by boolPref("hide_fav", true)`。
[AdvancedScreen.kt](app/src/main/kotlin/com/hippo/ehviewer/ui/settings/AdvancedScreen.kt) 中以 `SwitchPreference(title, summary, value = Settings::xxx)` 展示。

### 2.5 现有长按菜单

[HistoryScreen.kt](app/src/main/kotlin/com/hippo/ehviewer/ui/screen/HistoryScreen.kt#L166) 中 `onLongClick = { launch { doGalleryInfoAction(info) } }`。
[doGalleryInfoAction](app/src/main/kotlin/com/hippo/ehviewer/ui/CommonOperations.kt#L294) 是通用长按菜单（阅读/下载/收藏/移动），通过 `awaitSelectItemWithIcon` 展示图标列表。

## 3. 设计

### 3.1 设置项

**文件**：[Settings.kt](app/src/main/kotlin/com/hippo/ehviewer/Settings.kt)

新增：

```kotlin
var hideFavInHistory by boolPref("hide_fav_in_history", false)
```

默认关闭（false），与既有 `hideFav`（搜索列表，默认 true）行为独立。

**文件**：[AdvancedScreen.kt](app/src/main/kotlin/com/hippo/ehviewer/ui/settings/AdvancedScreen.kt)

在 `[Self]` 区域（`hideFav` 开关附近）新增：

```kotlin
SwitchPreference(
    title = "[Self] 隐藏历史中已收藏画廊",
    summary = "在历史列表中隐藏已收藏（本地+云端）的画廊",
    value = Settings::hideFavInHistory,
)
```

**文件**：[strings.xml](app/src/main/res/values/strings.xml)

新增字符串 `settings_hide_fav_in_history` / `settings_hide_fav_in_history_summary`（放入正式资源，非硬编码）。

### 3.2 历史查询：显示过滤

**文件**：[HistoryDao.kt](app/src/main/kotlin/com/hippo/ehviewer/dao/HistoryDao.kt)

新增两个过滤版本查询（保留原方法供他处使用）：

```kotlin
@Query("SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) WHERE FAVORITE_SLOT != -2 ORDER BY TIME DESC")
fun joinListLazyExcludeFav(): PagingSource<Int, BaseGalleryInfo>

@Query(
    """SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID)
    JOIN GALLERIES_FTS ON GALLERIES.rowid = docid WHERE GALLERIES_FTS MATCH :title AND FAVORITE_SLOT != -2 ORDER BY TIME DESC""",
)
fun joinListLazyExcludeFav(title: String): PagingSource<Int, BaseGalleryInfo>

// 全量历史（含 GALLERIES 的 simpleTags），供"同作者/同社团"匹配使用
@Query("SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
suspend fun joinList(): List<BaseGalleryInfo>
```

**文件**：[HistoryScreen.kt](app/src/main/kotlin/com/hippo/ehviewer/ui/screen/HistoryScreen.kt)

`Pager` 数据源按设置切换：

```kotlin
val hideFavInHistory by Settings.hideFavInHistory.collectAsState()
val historyData = rememberInVM {
    Pager(PagingConfig(pageSize = 20, jumpThreshold = 40)) {
        when {
            keyword.isNotEmpty() ->
                if (hideFavInHistory) EhDB.searchHistoryExcludeFav(keyword)
                else EhDB.searchHistory(keyword)
            hideFavInHistory -> EhDB.historyLazyListExcludeFav
            else -> EhDB.historyLazyList
        }
    }.flow.map { ... }
}
```

注意：切换开关时需 `historyData.refresh()` 使 Paging 重新加载（通过 `LaunchedEffect(hideFavInHistory)`）。

**文件**：[EhDB.kt](app/src/main/kotlin/com/hippo/ehviewer/EhDB.kt)

新增对应委托：

```kotlin
val historyLazyListExcludeFav get() = db.historyDao().joinListLazyExcludeFav()
fun searchHistoryExcludeFav(keyword: String) = db.historyDao().joinListLazyExcludeFav("*$keyword*")
```

### 3.3 长按移除同作者/同社团画廊

**文件**：[HistoryDao.kt](app/src/main/kotlin/com/hippo/ehviewer/dao/HistoryDao.kt)

新增批量删除：

```kotlin
@Query("DELETE FROM HISTORY WHERE GID IN (:gids)")
suspend fun deleteByKeyRange(gids: List<Long>)
```

**文件**：[EhDB.kt](app/src/main/kotlin/com/hippo/ehviewer/EhDB.kt)

新增业务方法：提取目标画廊的匹配标签键，遍历全部历史画廊，统计匹配数量 / 收集匹配的 gid 批量删除并返回删除数。

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
    return db.historyDao().joinList().count { it.gid != target.gid && it.simpleTags.orEmpty().any(keys::contains) }
}

suspend fun removeHistoryBySameArtistOrGroup(target: BaseGalleryInfo): Int {
    val keys = target.artistGroupKeys()
    if (keys.isEmpty()) return 0
    val toDelete = db.historyDao().joinList()
        .filter { it.gid != target.gid && it.simpleTags.orEmpty().any(keys::contains) }
        .map { it.gid }
    if (toDelete.isNotEmpty()) db.historyDao().deleteByKeyRange(toDelete)
    return toDelete.size
}
```

> 说明：`HistoryDao.joinList()` 为新增查询 `SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC` 返回 `List<BaseGalleryInfo>`（含 simpleTags），供上述方法使用。

**文件**：[CommonOperations.kt](app/src/main/kotlin/com/hippo/ehviewer/ui/CommonOperations.kt)

给通用 `doGalleryInfoAction` 增加可选参数，在菜单末尾追加一项扩展操作（默认无，不影响现有调用方）：

```kotlin
suspend fun doGalleryInfoAction(
    info: BaseGalleryInfo,
    extraAction: Pair<ImageVector, Int>? = null,  // 图标 + 菜单文案资源 id
    onExtraAction: suspend () -> Unit = {},
) {
    ...
    val baseCount = if (downloaded) 4 else 3  // 原有项数
    val items = buildList {
        add(read); add(download); add(favorite)
        if (downloaded) add(move)
        extraAction?.let { add(it) }
    }
    val selected = awaitSelectItemWithIcon(items, EhUtils.getSuitableTitle(info))
    ...
    when {
        selected in 0 until baseCount -> { ...原有逻辑不变... }
        extraAction != null && selected == baseCount -> withIOContext { onExtraAction() }
    }
}
```

**文件**：[HistoryScreen.kt](app/src/main/kotlin/com/hippo/ehviewer/ui/screen/HistoryScreen.kt)

长按传入扩展项，回调中执行"确认 → 删除 → 提示"：

```kotlin
onLongClick = { launch {
    doGalleryInfoAction(
        info,
        extraAction = Icons.Default.DeleteSweep to R.string.remove_same_artist_group,
    ) {
        removeHistoryBySameArtistOrGroupWithConfirm(info)
    }
} }
```

**文件**：[CommonOperations.kt](app/src/main/kotlin/com/hippo/ehviewer/ui/CommonOperations.kt)（新增独立函数）

```kotlin
context(DialogState, Context, DestinationsNavigator)
suspend fun removeHistoryBySameArtistOrGroupWithConfirm(info: BaseGalleryInfo) {
    val count = withIOContext { EhDB.countHistoryBySameArtistOrGroup(info) }
    if (count == 0) {
        showTip(R.string.no_matching_gallery_found)
        return
    }
    // 用户点"取消"时 awaitConfirmationOrCancel 通过 cont.cancel() 抛 CancellationException，
    // 会中断后续代码，因此无需判断返回值；走到这里即代表用户已确认
    awaitConfirmationOrCancel(
        confirmText = R.string.remove,
        text = { Text(stringResource(R.string.remove_same_artist_group_message, count)) },
    )
    val removed = withIOContext { EhDB.removeHistoryBySameArtistOrGroup(info) }
    if (removed > 0) showTip(R.string.removed_n_galleries, removed) else showTip(R.string.no_matching_gallery_found)
}
```

> 说明：先查询匹配数量（`countHistoryBySameArtistOrGroup`），确认弹窗直接显示"N 个匹配画廊"，确认后执行删除并提示实际删除数。若已存在更合适的通用函数可替换 `showTip`（HistoryScreen 现有 `launch { ... }` + Snackbar 模式亦可）。

## 4. 错误处理与边界

| 场景 | 处理 |
|---|---|
| 目标画廊无 artist/cosplayer/group 标签 | 提示"未找到匹配的画廊"，不执行删除 |
| 匹配数量为 0 | 提示"未找到匹配的画廊" |
| 删除中途失败（异常） | `runSuspendCatching` 包裹，失败提示，列表不刷新异常 |
| 搜索模式（keyword 非空）下长按 | 同样可用，删除基于全量历史而非搜索结果 |
| 切换隐藏开关 | `historyData.refresh()` 触发 Paging 重载 |

## 5. 测试计划

- 手动验证：
  1. 开启/关闭"隐藏已收藏"开关，历史列表相应过滤/恢复，分页正常无空白
  2. 收藏一个画廊后（本地+云端分别验证）历史中消失；取消收藏后重新出现
  3. 长按有 artist 标签的画廊 → 确认弹窗 → 删除同 artist 画廊，提示数量正确，被长按画廊保留
  4. 长按有 group 标签的画廊 → 同上
  5. 长按无标签画廊 → 提示无匹配
  6. 搜索状态下长按删除 → 列表刷新正常
- 无单测框架强制要求；若项目已有测试目录可按需补充 DAO 层测试。

## 6. 不做的事（YAGNI）

- 不修改通用 `doGalleryInfoAction` 菜单（避免影响画廊列表/收藏列表）
- 不做"撤销删除"（与现有左滑删除行为一致）
- 不为本地收藏单独建查询（FAVORITE_SLOT 已足够判定）
