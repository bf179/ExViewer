package com.hippo.ehviewer.ui.screen

import android.content.Context
import android.view.ViewConfiguration
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.fork.SwipeToDismissBox
import androidx.compose.material3.fork.SwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.ExlApiRequest
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhCookieStore.KEY_IGNEOUS
import com.hippo.ehviewer.client.EhCookieStore.KEY_IPB_MEMBER_ID
import com.hippo.ehviewer.client.EhCookieStore.KEY_IPB_PASS_HASH
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_IMAGE_SEARCH
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_NORMAL
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_SUBSCRIPTION
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TAG
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_UPLOADER
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_WHATS_HOT
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.GoTo
import com.hippo.ehviewer.sendExlApiRequest
import com.hippo.ehviewer.ui.DrawerHandle
import com.hippo.ehviewer.ui.LocalSideSheetState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.awaitSelectDate
import com.hippo.ehviewer.ui.destinations.ProgressScreenDestination
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.main.AdvancedSearchOption
import com.hippo.ehviewer.ui.main.AvatarIcon
import com.hippo.ehviewer.ui.main.FAB_ANIMATE_TIME
import com.hippo.ehviewer.ui.main.FabLayout
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.main.GalleryList
import com.hippo.ehviewer.ui.main.SearchFilter
import com.hippo.ehviewer.ui.modifyFavorites
import com.hippo.ehviewer.ui.tools.Await
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.HapticFeedbackType
import com.hippo.ehviewer.ui.tools.asyncState
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import com.hippo.ehviewer.ui.tools.rememberHapticFeedback
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.ui.tools.rememberMutableStateInDataStore
import com.hippo.ehviewer.ui.tools.thenIf
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.text.toLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.tarsin.coroutines.onEachLatest
import moe.tarsin.coroutines.runSuspendCatching
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.HomePageScreen(navigator: DestinationsNavigator) = GalleryListScreen(ListUrlBuilder(), navigator)

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.SubscriptionScreen(navigator: DestinationsNavigator) = GalleryListScreen(ListUrlBuilder(MODE_SUBSCRIPTION), navigator)

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.WhatshotScreen(navigator: DestinationsNavigator) = GalleryListScreen(ListUrlBuilder(MODE_WHATS_HOT), navigator)

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.ToplistScreen(navigator: DestinationsNavigator) = GalleryListScreen(ListUrlBuilder(MODE_TOPLIST, mKeyword = Settings.recentToplist), navigator)

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.GalleryListScreen(lub: ListUrlBuilder, navigator: DestinationsNavigator) = Screen(navigator) {
    val searchFieldState = rememberTextFieldState()
    var urlBuilder by rememberSaveable(lub) { mutableStateOf(lub) }
    var searchBarExpanded by rememberSaveable { mutableStateOf(false) }
    var searchBarOffsetY by remember { mutableIntStateOf(0) }
    val animateItems by Settings.animateItems.collectAsState()

    var category by rememberMutableStateInDataStore("SearchCategory") { EhUtils.ALL_CATEGORY }
    var advancedSearchOption by rememberMutableStateInDataStore("AdvancedSearchOption") { AdvancedSearchOption() }

    DrawerHandle(!searchBarExpanded)

    LaunchedEffect(urlBuilder) {
        if (urlBuilder.category != EhUtils.NONE) category = urlBuilder.category
        if (urlBuilder.mode != MODE_TOPLIST) {
            var keyword = urlBuilder.keyword.orEmpty()
            if (urlBuilder.mode == MODE_TAG) {
                keyword = wrapTagKeyword(keyword)
            }
            if (keyword.isNotBlank()) {
                searchFieldState.setTextAndPlaceCursorAtEnd(keyword)
            }
        }
    }

    val density = LocalDensity.current
    val listState = rememberLazyGridState()
    val gridState = rememberLazyStaggeredGridState()
    val isTopList = remember(urlBuilder) { urlBuilder.mode == MODE_TOPLIST }
    val ehHint = stringResource(R.string.gallery_list_search_bar_hint_e_hentai)
    val exHint = stringResource(R.string.gallery_list_search_bar_hint_exhentai)
    val searchBarHint by rememberUpdatedState(if (EhUtils.isExHentai) exHint else ehHint)
    val suitableTitle = getSuitableTitleForUrlBuilder(urlBuilder)
    val data = rememberInVM {
        Pager(PagingConfig(25)) {
            object : PagingSource<String, BaseGalleryInfo>() {
                // 记录最后一次加载时间（每次创建PagingSource时初始化）
                var lastLoadTime = 0L
                val slowload = Settings.hideFav
                override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
                override suspend fun load(params: LoadParams<String>) = withIOContext {
                    if (urlBuilder.mode == MODE_TOPLIST) {
                        // TODO: Since we know total pages, let pager support jump
                        val key = (params.key ?: urlBuilder.jumpTo)?.toInt() ?: 0
                        val prev = (key - 1).takeIf { it > 0 }
                        val next = (key + 1).takeIf { it < TOPLIST_PAGES }
                        runSuspendCatching {
                            urlBuilder.setJumpTo(key)
                            EhEngine.getGalleryList(urlBuilder.build())
                        }.foldToLoadResult { result ->
                            LoadResult.Page(result.galleryInfoList, prev?.toString(), next?.toString())
                        }
                    } else {
                        if (slowload) {
                            // 仅对分页加载（Append）限流
                            if (params is LoadParams.Append) {
                                val currentTime = System.currentTimeMillis()
                                val elapsed = currentTime - lastLoadTime
                                if (elapsed < 3000) { // 3秒间隔
//                                    showSnackbar("限流 ${elapsed/1000} s")
                                    delay(3000 - elapsed) // 动态补足剩余时间
                                }
                                lastLoadTime = currentTime // 更新最后加载时间
                            }
                        }
                        when (params) {
                            is LoadParams.Prepend -> urlBuilder.setIndex(params.key, isNext = false)
                            is LoadParams.Append -> urlBuilder.setIndex(params.key, isNext = true)
                            is LoadParams.Refresh -> {
                                val key = params.key
                                if (key.isNullOrBlank()) {
                                    if (urlBuilder.jumpTo != null) {
                                        urlBuilder.next ?: urlBuilder.setIndex("2", true)
                                    }
                                } else {
                                    urlBuilder.setIndex(key, false)
                                }
                            }
                        }
                        runSuspendCatching {
                            val url = urlBuilder.build()
                            EhEngine.getGalleryList(url)
                        }.foldToLoadResult { result ->
                            urlBuilder.jumpTo = null
                            LoadResult.Page(result.galleryInfoList, result.prev, result.next)
                        }
                    }
                }
            }
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()
    ReportDrawnWhen { data.loadState.refresh !is LoadState.Loading }
    FavouriteStatusRouter.Observe(data)
    val listMode by Settings.listMode.collectAsState()

    val entries = stringArrayResource(id = R.array.toplist_entries)
    val values = stringArrayResource(id = R.array.toplist_values)
    val toplists = remember { entries zip values }
    val quickSearchName = getSuitableTitleForUrlBuilder(urlBuilder, false)
    var saveProgress by Settings.qSSaveProgress.asMutableState()
    var languageFilter by Settings.languageFilter.asMutableState()

    fun getFirstVisibleItemIndex() = if (listMode == 0) {
        listState.firstVisibleItemIndex
    } else {
        gridState.firstVisibleItemIndex
    }

    if (isTopList) {
        ProvideSideSheetContent { sheetState ->
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.toplist)) },
                windowInsets = WindowInsets(),
                colors = topBarOnDrawerColor(),
            )
            toplists.forEach { (name, keyword) ->
                ListItem(
                    modifier = Modifier.padding(horizontal = 4.dp).clip(CardDefaults.shape).clickable {
                        Settings.recentToplist = keyword
                        urlBuilder = ListUrlBuilder(MODE_TOPLIST, mKeyword = keyword)
                        data.refresh()
                        launch { sheetState.close() }
                    },
                    headlineContent = {
                        Text(text = name)
                    },
                    colors = listItemOnDrawerColor(urlBuilder.keyword == keyword),
                )
            }
        }
    } else {
        ProvideSideSheetContent { sheetState ->
            val quickSearchList = remember { mutableStateListOf<QuickSearch>() }
            LaunchedEffect(Unit) {
                val list = EhDB.getAllQuickSearch()
                quickSearchList.addAll(list)
            }
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.quick_search)) },
                colors = topBarOnDrawerColor(),
                actions = {
                    val nameEmpty = stringResource(R.string.name_is_empty)
                    // FilterAlt添加按钮
                    IconButton(
                        onClick = {
                            launch {
                                if (urlBuilder.mode == MODE_IMAGE_SEARCH) {
                                    showSnackbar("图片不能被添加到过滤器")
                                } else {
                                    val input = awaitInputText(
                                        initial = urlBuilder.keyword.orEmpty(),
                                        title = "添加过滤器",
                                        hint = "添加搜索框中内容到过滤器",
                                    ) { rawInput ->
                                        var ptext = rawInput.trim()
                                        ensure(ptext.isNotBlank()) { nameEmpty }
                                        ptext
                                    } ?: return@launch // 取消输入时直接返回
                                    val ftext = input.trim()
                                    if (ftext.isBlank()) {
                                        showSnackbar("输入为空")
                                        return@launch
                                    }
                                    if (ftext.isNotBlank()) {
                                        val (processedText, filterMode, modeDisplayName) = if (!('$' in ftext) && (ftext.startsWith('"') && ftext.endsWith('"'))) {
                                            // 单标题模式
                                            val processed = ftext.removeSurrounding("\"")
                                            Triple(processed, FilterMode.TITLE, "标题")
                                        } else {
                                            // 复合标签模式
                                            val processed = ftext.removeSuffix("$")
                                                .replace("$ ", ",")
                                                .replace("\" ", "\",")
                                                .replace("p:", "parody:")
                                                .replace("f:", "female:")
                                                .replace("m:", "male:")
                                                .replace("a:", "artist:")
                                                .replace("x:", "mixed:")
                                                .replace("o:", "other:")
                                                .replace("l:", "language:")
                                                .replace("g:", "group:")
                                                .replace("c:", "character:")
                                                .replace("cos:", "cosplayer:")
                                            if (',' in processed) {
                                                Triple(processed, FilterMode.TAG_GROUP, "复合标签")
                                            } else {
                                                if ("uploader:" in processed) {
                                                    Triple(processed, FilterMode.UPLOADER, "上传者")
                                                } else {
                                                    Triple(processed, FilterMode.TAG, "单标签")
                                                }
                                            }
                                        }
                                        awaitConfirmationOrCancel {
                                            Text(text = "屏蔽 \"$processedText\" ($modeDisplayName)?")
                                        }
                                        withContext(Dispatchers.IO) {
                                            Filter(filterMode, processedText).remember()
                                        }
                                        showSnackbar("过滤项已添加")
                                    }
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Default.FilterAlt, contentDescription = "Add filter item")
                    }
                    IconButton(onClick = {
                        launch {
                            awaitConfirmationOrCancel(title = R.string.quick_search, showCancelButton = false) {
                                Text(text = stringResource(id = R.string.add_quick_search_tip))
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Help,
                            contentDescription = stringResource(id = R.string.readme),
                        )
                    }
                    val invalidImageQuickSearch = stringResource(R.string.image_search_not_quick_search)
                    val dupName = stringResource(R.string.duplicate_name)
                    IconButton(
                        onClick = {
                            launch {
                                if (urlBuilder.mode == MODE_IMAGE_SEARCH) {
                                    showSnackbar(invalidImageQuickSearch)
                                } else {
                                    // itemCount == 0 is treated as error, so no need to check here
                                    val firstItem = data.itemSnapshotList.items[getFirstVisibleItemIndex()]
                                    val next = firstItem.gid + 1
                                    quickSearchList.fastForEach { q ->
                                        if (urlBuilder.equalsQuickSearch(q)) {
                                            val nextStr = q.name.substringAfterLast('@', "")
                                            if (nextStr.toLongOrNull() == next) {
                                                showSnackbar(getString(R.string.duplicate_quick_search, q.name))
                                                return@launch
                                            }
                                        }
                                    }
                                    awaitInputTextWithCheckBox(
                                        initial = quickSearchName ?: urlBuilder.keyword.orEmpty(),
                                        title = R.string.add_quick_search_dialog_title,
                                        hint = R.string.quick_search,
                                        checked = saveProgress,
                                        checkBoxText = R.string.save_progress,
                                    ) { input, checked ->
                                        var text = input.trim()
                                        ensure(text.isNotBlank()) { nameEmpty }
                                        if (checked) text += "@$next"
                                        ensure(quickSearchList.none { it.name == text }) { dupName }
                                        val quickSearch = urlBuilder.toQuickSearch(text)
                                        quickSearch.position = quickSearchList.size
                                        // Insert to DB first to update the id
                                        EhDB.insertQuickSearch(quickSearch)
                                        quickSearchList.add(quickSearch)
                                        saveProgress = checked
                                    }
                                }
                            }
                        },
                        enabled = data.loadState.isIdle,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.add),
                        )
                    }
                },
                windowInsets = WindowInsets(),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                val dialogState by rememberUpdatedState(implicit<DialogState>())
                val quickSearchListState = rememberLazyListState()
                val hapticFeedback = rememberHapticFeedback()
                val reorderableLazyListState = rememberReorderableLazyListState(quickSearchListState) { from, to ->
                    quickSearchList.apply { add(to.index, removeAt(from.index)) }
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.MOVE)
                }
                var fromIndex by remember { mutableIntStateOf(-1) }
                FastScrollLazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    state = quickSearchListState,
                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
                ) {
                    itemsIndexed(quickSearchList, key = { _, item -> item.id!! }) { itemIndex, item ->
                        val index by rememberUpdatedState(itemIndex)
                        ReorderableItem(
                            reorderableLazyListState,
                            item.id!!,
                            animateItemModifier = Modifier.thenIf(animateItems) { animateItem() },
                        ) { isDragging ->
                            // Not using rememberSwipeToDismissBoxState to prevent LazyColumn from reusing it
                            // SQLite may reuse ROWIDs from previously deleted rows so they'll have the same key
                            val dismissState = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, density) }
                            LaunchedEffect(dismissState) {
                                snapshotFlow { dismissState.currentValue }.collect { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        runCatching {
                                            dialogState.awaitConfirmationOrCancel(confirmText = R.string.delete) {
                                                Text(text = stringResource(R.string.delete_quick_search, item.name))
                                            }
                                        }.onSuccess {
                                            EhDB.deleteQuickSearch(item)
                                            with(quickSearchList) {
                                                subList(index + 1, size).forEach {
                                                    it.position--
                                                }
                                                removeAt(index)
                                            }
                                        }.onFailure {
                                            dismissState.reset()
                                        }
                                    }
                                }
                            }
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {},
                                enableDismissFromStartToEnd = false,
                            ) {
                                val elevation by animateDpAsState(
                                    if (isDragging) {
                                        8.dp // md.sys.elevation.level4
                                    } else {
                                        1.dp // md.sys.elevation.level1
                                    },
                                    label = "elevation",
                                )
                                ListItem(
                                    modifier = Modifier.clip(CardDefaults.shape).clickable {
                                        if (urlBuilder.mode == MODE_WHATS_HOT) {
                                            val builder = ListUrlBuilder(item).apply {
                                                language = languageFilter
                                            }
                                            navigator.navigate(builder.asDst())
                                        } else {
                                            urlBuilder = ListUrlBuilder(item).apply {
                                                language = languageFilter
                                            }
                                            data.refresh()
                                        }
                                        launch { sheetState.close() }
                                    },
                                    shadowElevation = elevation,
                                    headlineContent = {
                                        Text(text = item.name)
                                    },
                                    trailingContent = {
                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.START)
                                                    fromIndex = index
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.END)
                                                    if (fromIndex != -1) {
                                                        if (fromIndex != index) {
                                                            val range = if (fromIndex < index) fromIndex..index else index..fromIndex
                                                            val toUpdate = quickSearchList.slice(range)
                                                            toUpdate.zip(range).forEach { it.first.position = it.second }
                                                            launchIO { EhDB.updateQuickSearch(toUpdate) }
                                                        }
                                                        fromIndex = -1
                                                    }
                                                },
                                            ),
                                        ) {
                                            Icon(imageVector = Icons.Default.Reorder, contentDescription = null)
                                        }
                                    },
                                    colors = listItemOnDrawerColor(false),
                                )
                            }
                        }
                    }
                }
                Await({ delay(200) }) {
                    if (quickSearchList.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.quick_search_tip),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }

    var fabExpanded by remember { mutableStateOf(false) }
    var fabHidden by remember { mutableStateOf(false) }

    val openGalleryKeyword = stringResource(R.string.gallery_list_search_bar_open_gallery)
    abstract class UrlSuggestion : Suggestion() {
        override val keyword = openGalleryKeyword
        override val canOpenDirectly = true
        override fun onClick() = navigate(destination)
        abstract val destination: Direction
    }

    class GalleryDetailUrlSuggestion(gid: Long, token: String) : UrlSuggestion() {
        override val destination = gid asDstWith token
    }

    class GalleryPageUrlSuggestion(gid: Long, pToken: String, page: Int) : UrlSuggestion() {
        override val destination = ProgressScreenDestination(gid, pToken, page)
    }

    fun onApplySearch(query: String) = launchIO {
        val builder = ListUrlBuilder()
        val oldMode = urlBuilder.mode
        // If it's MODE_SUBSCRIPTION, keep it
        val newMode = if (oldMode == MODE_SUBSCRIPTION) MODE_SUBSCRIPTION else MODE_NORMAL
        builder.mode = newMode
        builder.keyword = query
        builder.category = category
        builder.language = languageFilter
        builder.advanceSearch = advancedSearchOption.advanceSearch
        builder.minRating = advancedSearchOption.minRating
        builder.pageFrom = advancedSearchOption.fromPage
        builder.pageTo = advancedSearchOption.toPage
        when (oldMode) {
            MODE_TOPLIST, MODE_WHATS_HOT -> {
                // Wait for search view to hide
                delay(300)
                withUIContext { navigate(builder.asDst()) }
            }
            else -> {
                urlBuilder = builder
                data.refresh()
            }
        }
    }

    SearchBarScreen(
        onApplySearch = ::onApplySearch,
        expanded = searchBarExpanded,
        onExpandedChange = {
            searchBarExpanded = it
            fabHidden = it
        },
        title = suitableTitle,
        searchFieldHint = searchBarHint,
        searchFieldState = searchFieldState,
        suggestionProvider = {
            GalleryDetailUrlParser.parse(it, false)?.run {
                GalleryDetailUrlSuggestion(gid, token)
            } ?: GalleryPageUrlParser.parse(it, false)?.run {
                GalleryPageUrlSuggestion(gid, pToken, page)
            }
        },
        tagNamespace = true,
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            val sheetState = LocalSideSheetState.current
            IconButton(onClick = { launch { sheetState.open() } }) {
                Icon(imageVector = Icons.Outlined.Bookmarks, contentDescription = stringResource(id = R.string.quick_search))
            }
            AvatarIcon()
        },
        filter = {
            SearchFilter(
                category = category,
                onCategoryChange = { category = it },
                language = languageFilter,
                onLanguageChange = { languageFilter = it },
                advancedOption = advancedSearchOption,
                onAdvancedOptionChange = { advancedSearchOption = it },
            )
        },
    ) { contentPadding ->
        val height by collectListThumbSizeAsState()
        val showPages by Settings.showGalleryPages.collectAsState()
        val searchBarConnection = remember {
            val slop = ViewConfiguration.get(implicit<Context>()).scaledTouchSlop
            val topPaddingPx = with(density) { contentPadding.calculateTopPadding().roundToPx() }
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val dy = -consumed.y
                    if (dy >= slop) {
                        fabHidden = true
                    } else if (dy <= -slop / 2) {
                        fabHidden = false
                    }
                    searchBarOffsetY = (searchBarOffsetY - dy).roundToInt().coerceIn(-topPaddingPx, 0)
                    return Offset.Zero // We never consume it
                }
            }
        }
        GalleryList(
            data = data,
            contentModifier = Modifier.nestedScroll(searchBarConnection),
            contentPadding = contentPadding,
            listMode = listMode,
            detailListState = listState,
            detailItemContent = { info ->
                GalleryInfoListItem(
                    onClick = { navigate(info.asDst()) },
//                    onLongClick = { launch { doGalleryInfoAction(info) } },
                    onLongClick = {
                        launch {
                            val quickfav = Settings.quickFav
                            if (quickfav) {
                                val favorited = info.favoriteSlot != NOT_FAVORITED
                                if (!favorited) {
                                    runSuspendCatching {
                                        modifyFavorites(info)
                                        showTip(R.string.add_to_favorite_success)
                                    }.onFailure {
                                        showTip(R.string.add_to_favorite_failure)
                                    }
                                } else {
                                    doGalleryInfoAction(info)
                                }
                            } else {
                                doGalleryInfoAction(info)
                            }
                        }
                    },
                    info = info,
                    showPages = showPages,
                    modifier = Modifier.height(height),
                )
            },
            thumbListState = gridState,
            thumbItemContent = { info ->
                GalleryInfoGridItem(
                    onClick = { navigate(info.asDst()) },
//                    onLongClick = { launch { doGalleryInfoAction(info) } },
                    onLongClick = {
                        launch {
                            val quickfav = Settings.quickFav
                            if (quickfav) {
                                val favorited = info.favoriteSlot != NOT_FAVORITED
                                if (!favorited) {
                                    runSuspendCatching {
                                        modifyFavorites(info)
                                        showTip(R.string.add_to_favorite_success)
                                    }.onFailure {
                                        showTip(R.string.add_to_favorite_failure)
                                    }
                                } else {
                                    doGalleryInfoAction(info)
                                }
                            } else {
                                doGalleryInfoAction(info)
                            }
                        }
                    },
                    info = info,
                    showPages = showPages,
                )
            },
            searchBarOffsetY = { searchBarOffsetY },
            onRefresh = {
                urlBuilder.setRange(0)
                data.refresh()
            },
            onLoading = { searchBarOffsetY = 0 },
        )
    }

    // 获取当前已加载的画廊列表（LazyPagingItems）
    val currentGalleryList = data.itemSnapshotList.items
    // 批量收藏任务处理状态下防止重复点击
    var isProcessing by remember { mutableStateOf(false) }
    var batchFavJob: Job? = null
    val gotoTitle = stringResource(R.string.go_to)
    val invalidNum = stringResource(R.string.error_invalid_number)
    val outOfRange = stringResource(R.string.error_out_of_range)

    val hideFab by asyncState(
        produce = { fabHidden },
        transform = {
            onEachLatest { hide ->
                if (!hide) delay(FAB_ANIMATE_TIME.toLong())
            }
        },
    )

    FabLayout(
        hidden = hideFab,
        expanded = fabExpanded,
        onExpandChanged = { fabExpanded = it },
        autoCancel = true,
    ) {
        if (urlBuilder.mode in arrayOf(MODE_NORMAL, MODE_UPLOADER, MODE_TAG)) {
            onClick(Icons.Default.Shuffle) {
                urlBuilder.setRange(Random.nextInt(100))
                data.refresh()
            }
        }
        onClick(Icons.Default.Refresh) {
            urlBuilder.setRange(0)
            data.refresh()
        }
        val syncfav = Settings.syncFav
        val sapi = Settings.sapiUrl
        if (syncfav && !sapi.isNullOrBlank()) {
            onClick(Icons.Default.Flight) {
                launch {
                    try {
                        // 上传当前cookie
                        awaitConfirmationOrCancel {
                            Text(text = "将当前cookie同步至Api?")
                        }
                        val cookies = EhCookieStore.getIdentityCookies()
                        val ipbMemberId = cookies.firstOrNull { it.first == KEY_IPB_MEMBER_ID }?.second
                        val ipbPassHash = cookies.firstOrNull { it.first == KEY_IPB_PASS_HASH }?.second
                        val igneous = cookies.firstOrNull { it.first == KEY_IGNEOUS }?.second

                        // 使用前检查null
                        if (!ipbMemberId.isNullOrBlank() && !ipbPassHash.isNullOrBlank() && !igneous.isNullOrBlank() && igneous != "mystery") {
                            val ipbMemberIdLong: Long = ipbMemberId.toLong()
                            withContext(Dispatchers.IO) {
                                // 向 API 发送 POST 请求
                                val ctoken = "${ipbPassHash}_$igneous"
                                val exlar = ExlApiRequest(
                                    user = "loliwant",
                                    gid = ipbMemberIdLong,
                                    token = ctoken,
                                    favoriteslot = 99,
                                    op = "uc",
                                )
                                sendExlApiRequest(exlar, sapi)
                                showSnackbar("Update finished: igneous_$igneous")
                            }
                        } else {
                            showSnackbar("不满足更新条件")
                        }
                    } catch (e: Exception) {
                        showSnackbar("Upload failed: ${e.message}")
                    }
                }
            }
        }
        onClick(Icons.Default.Flag) {
            launch {
                try {
                    // 用于存储当前搜索状态用于快速恢复
                    val ffirstItem = data.itemSnapshotList.items[getFirstVisibleItemIndex()]
                    val fnext = ffirstItem.gid + 1
                    val text = "lastSearch@$fnext"
                    val lastSearch = urlBuilder.toQuickSearch(text)
                    withContext(Dispatchers.IO) {
                        EhDB.updateLastSearch(lastSearch)
                    }
                    showSnackbar("Quick saved successfully!")
                } catch (e: Exception) {
                    showSnackbar("Save failed: ${e.message}")
                }
            }
        }
        onClick(Icons.Default.Restore) {
            launch {
                try {
                    // 快速恢复到存储的搜索状态
                    val lastSearch = EhDB.getLastSearch()
                    lastSearch?.let { search ->
                        // 创建新的 UrlBuilder 并应用搜索条件
                        val newBuilder = ListUrlBuilder(search).apply {
                            language = languageFilter
                        }
                        // 更新全局 urlBuilder 并刷新数据
                        urlBuilder = newBuilder
                        data.refresh() // 触发分页数据刷新
                    }
                    showSnackbar("Restore last save successfully!")
                } catch (e: Exception) {
                    showSnackbar("Restore failed: ${e.message}")
                }
            }
        }
        onClick(Icons.Default.FilterAlt) {
            val addkeyword = Settings.addKeyword
            if (!addkeyword.isNullOrBlank()) {
                val newBuilder = urlBuilder.withAddedKeyword(addkeyword)
                urlBuilder = newBuilder
                data.refresh()
            }
        }
        onClick(Icons.Default.Bookmarks) {
            if (!isProcessing) {
                isProcessing = true
                var successCount = 0
                batchFavJob = launch {
                    try {
                        awaitConfirmationOrCancel {
                            Text(text = "警告：确定要将当前已加载的 ${currentGalleryList.size} 个画廊全部收藏到默认收藏夹?")
                        }
                        val defaultFavSlot = Settings.defaultFavSlot
                        val slowfav = defaultFavSlot != -1
                        currentGalleryList.chunked(10).forEach { chunk ->
                            chunk.forEach { galleryInfo ->
                                ensureActive()
                                val isFavorited = galleryInfo.favoriteSlot != NOT_FAVORITED
                                if (!isFavorited) {
                                    runSuspendCatching {
                                        modifyFavorites(galleryInfo, showSuccessToast = false)
                                    }.onSuccess { successCount++ }
                                    if (slowfav) {
                                        delay(4000)
                                    }
                                    delay(100) // 每个画廊处理完延迟100毫秒
                                } else {
                                    successCount++
                                }
                            }
                            ensureActive()
                            delay(200) // 每组画廊处理完延迟200毫秒
                            // if (slowfav) {
                            //     delay(5000)
                            // }
                            showSnackbar("少女祈祷中 ($successCount/${currentGalleryList.size})……")
                        }
                        showSnackbar("成功收藏 $successCount/${currentGalleryList.size} 个画廊")
                    } catch (e: CancellationException) {
                        showSnackbar("任务中止，已处理收藏 $successCount 个画廊")
                    } finally {
                        isProcessing = false
                        batchFavJob = null
                    }
                }
            } else {
                launch {
                    awaitConfirmationOrCancel {
                        Text(text = "中止当前的收藏任务?")
                    }
                    batchFavJob?.cancel(CancellationException("手动中止"))
                }
            }
        }
        if (urlBuilder.mode != MODE_WHATS_HOT) {
            onClick(EhIcons.Default.GoTo) {
                if (isTopList) {
                    val page = urlBuilder.jumpTo?.toIntOrNull() ?: 0
                    val hint = getString(R.string.go_to_hint, page + 1, TOPLIST_PAGES)
                    val text = awaitInputText(title = gotoTitle, hint = hint, isNumber = true) { oriText ->
                        val goto = ensureNotNull(oriText.trim().toIntOrNull()) { invalidNum } - 1
                        ensure(goto in 0..<TOPLIST_PAGES) { outOfRange }
                    }.trim().toInt() - 1
                    urlBuilder.setJumpTo(text)
                } else {
                    val date = awaitSelectDate()
                    urlBuilder.jumpTo = date
                }
                data.refresh()
            }
            onClick(Icons.AutoMirrored.Default.LastPage) {
                if (isTopList) {
                    urlBuilder.setJumpTo(TOPLIST_PAGES - 1)
                } else {
                    urlBuilder.setIndex("1", false)
                }
                data.refresh()
            }
        }
    }
}

private const val TOPLIST_PAGES = 200

@Composable
@Stable
private fun getSuitableTitleForUrlBuilder(urlBuilder: ListUrlBuilder, appName: Boolean = true): String? {
    val context = LocalContext.current
    val keyword = urlBuilder.keyword
    val category = urlBuilder.category
    val mode = urlBuilder.mode
    return if (mode == MODE_WHATS_HOT) {
        stringResource(R.string.whats_hot)
    } else if (!keyword.isNullOrEmpty()) {
        when (mode) {
            MODE_TOPLIST -> {
                when (keyword) {
                    "11" -> stringResource(R.string.toplist_alltime)
                    "12" -> stringResource(R.string.toplist_pastyear)
                    "13" -> stringResource(R.string.toplist_pastmonth)
                    "15" -> stringResource(R.string.toplist_yesterday)
                    else -> null
                }
            }
            MODE_TAG -> {
                val canTranslate = Settings.showTagTranslations && EhTagDatabase.isTranslatable(context) && EhTagDatabase.initialized
                wrapTagKeyword(keyword, canTranslate)
            }
            else -> keyword
        }
    } else if (category == EhUtils.NONE && urlBuilder.advanceSearch == -1) {
        val appNameStr = stringResource(R.string.app_name)
        val homepageStr = stringResource(R.string.homepage)
        when (mode) {
            MODE_NORMAL -> if (appName) appNameStr else homepageStr
            MODE_SUBSCRIPTION -> stringResource(R.string.subscription)
            else -> null
        }
    } else if (category.countOneBits() == 1) {
        EhUtils.getCategory(category)
    } else {
        null
    }
}
