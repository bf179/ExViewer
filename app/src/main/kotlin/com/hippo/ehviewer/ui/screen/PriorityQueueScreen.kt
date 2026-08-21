package com.hippo.ehviewer.ui.screen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.PqGalleryItem
import com.hippo.ehviewer.PqGroupItem
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.fetchPqGroupGalleries
import com.hippo.ehviewer.fetchPqGroups
import com.hippo.ehviewer.fetchPqSingles
import com.hippo.ehviewer.ui.DrawerHandle
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.tools.observed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.launch

// 优先队列每页大小（与服务器默认一致）
private const val PQ_PAGE_SIZE = 30

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.PriorityQueueScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val pqUrl by Settings::pqUrl.observed
    val hideFav by Settings::hideFav.observed
    val scope = rememberCoroutineScope()
    // 捕获当前配置值（委托属性无法智能转换，先取到局部变量）
    val pqUrlValue = pqUrl

    // 单画廊段状态
    val singles = remember { mutableStateListOf<PqGalleryItem>() }
    var singlesPage by remember { mutableIntStateOf(0) }
    var singlesLoading by remember { mutableStateOf(false) }
    var singlesEnded by remember { mutableStateOf(false) }

    // 标签组段状态
    val groups = remember { mutableStateListOf<PqGroupItem>() }
    var groupsPage by remember { mutableIntStateOf(0) }
    var groupsLoading by remember { mutableStateOf(false) }
    var groupsEnded by remember { mutableStateOf(false) }

    // 展开组状态
    var expandedGroup by remember { mutableStateOf<String?>(null) }
    val groupItems = remember { mutableStateListOf<PqGalleryItem>() }
    var groupPage by remember { mutableIntStateOf(0) }
    var groupLoading by remember { mutableStateOf(false) }
    var groupEnded by remember { mutableStateOf(false) }

    suspend fun loadSingles() {
        val baseUrl = pqUrlValue ?: return
        if (singlesLoading || singlesEnded) return
        singlesLoading = true
        try {
            val page = singlesPage
            val result = withIOContext { fetchPqSingles(baseUrl, page, PQ_PAGE_SIZE, hideFav) }
            if (result != null) {
                // hideFav 时除服务端过滤外，再本地双查（本地收藏也视为已收藏）
                val filtered = if (hideFav) {
                    buildList {
                        for (item in result) {
                            if (item.fav_status == 0 && !EhDB.containLocalFavorites(item.gid)) add(item)
                        }
                    }
                } else {
                    result
                }
                singles.addAll(filtered)
                singlesPage = page + 1
                if (result.size < PQ_PAGE_SIZE) singlesEnded = true
            }
        } finally {
            singlesLoading = false
        }
    }

    suspend fun loadGroups() {
        val baseUrl = pqUrlValue ?: return
        if (groupsLoading || groupsEnded) return
        groupsLoading = true
        try {
            val page = groupsPage
            val result = withIOContext { fetchPqGroups(baseUrl, page, PQ_PAGE_SIZE, hideFav) }
            if (result != null) {
                groups.addAll(result)
                // 存在未收藏新画廊（new_count>0）的组置前，其余保持服务端顺序
                groups.sortWith(compareByDescending<PqGroupItem> { it.new_count > 0 }.thenByDescending { it.new_count })
                groupsPage = page + 1
                if (result.size < PQ_PAGE_SIZE) groupsEnded = true
            }
        } finally {
            groupsLoading = false
        }
    }

    suspend fun loadGroupGalleries(group: String) {
        val baseUrl = pqUrlValue ?: return
        if (groupLoading || groupEnded) return
        groupLoading = true
        try {
            val page = groupPage
            val result = withIOContext { fetchPqGroupGalleries(baseUrl, group, page, PQ_PAGE_SIZE, hideFav) }
            if (result != null) {
                val filtered = if (hideFav) {
                    buildList {
                        for (item in result) {
                            if (item.fav_status == 0 && !EhDB.containLocalFavorites(item.gid)) add(item)
                        }
                    }
                } else {
                    result
                }
                groupItems.addAll(filtered)
                groupPage = page + 1
                if (result.size < PQ_PAGE_SIZE) groupEnded = true
            }
        } finally {
            groupLoading = false
        }
    }

    // 配置变化或首次进入时重置并加载
    LaunchedEffect(pqUrlValue, hideFav) {
        singles.clear()
        singlesPage = 0
        singlesEnded = false
        groups.clear()
        groupsPage = 0
        groupsEnded = false
        expandedGroup = null
        groupItems.clear()
        groupPage = 0
        groupEnded = false
        if (pqUrlValue.isNullOrBlank()) return@LaunchedEffect
        launch { loadSingles() }
        launch { loadGroups() }
    }

    DrawerHandle(true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.priority_queue)) },
                navigationIcon = {
                    IconButton(onClick = { popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        if (pqUrlValue.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "未配置优先队列地址", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "请到 设置-高级 填写 PQ URL 与 API Token",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val pqBase = pqUrlValue
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // ===== 单画廊段（在前）=====
                item(key = "single_header") {
                    SectionHeader(text = "单画廊")
                }
                items(singles, key = { "single_${it.gid}" }) { item ->
                    PqGalleryRow(item = item, pqUrl = pqBase, onClick = { navigate(item.gid asDstWith item.token) })
                }
                if (singles.isEmpty() && singlesEnded) {
                    item(key = "single_empty") {
                        EmptyHint(text = "暂无单画廊")
                    }
                }
                if (!singlesEnded) {
                    item(key = "single_more") {
                        LoadMoreButton(loading = singlesLoading, onLoad = { scope.launch { loadSingles() } })
                    }
                }

                // ===== 标签组段（在后）=====
                item(key = "group_header") {
                    SectionHeader(text = "标签组")
                }
                items(groups, key = { "group_${it.group_content}" }) { group ->
                    val expanded = expandedGroup == group.group_content
                    PqGroupRow(
                        group = group,
                        expanded = expanded,
                        onClick = {
                            if (expanded) {
                                expandedGroup = null
                                groupItems.clear()
                            } else {
                                expandedGroup = group.group_content
                                groupItems.clear()
                                groupPage = 0
                                groupEnded = false
                                scope.launch { loadGroupGalleries(group.group_content) }
                            }
                        },
                    )
                    if (expanded) {
                        groupItems.forEach { item ->
                            PqGalleryRow(item = item, pqUrl = pqBase, onClick = { navigate(item.gid asDstWith item.token) })
                        }
                        if (groupItems.isEmpty() && groupEnded) {
                            EmptyHint(text = "该组暂无画廊")
                        }
                        if (!groupEnded) {
                            LoadMoreButton(loading = groupLoading, onLoad = { scope.launch { loadGroupGalleries(group.group_content) } })
                        }
                    }
                }
                if (groups.isEmpty() && groupsEnded) {
                    item(key = "group_empty") {
                        EmptyHint(text = "暂无标签组")
                    }
                }
                if (!groupsEnded) {
                    item(key = "group_more") {
                        LoadMoreButton(loading = groupsLoading, onLoad = { scope.launch { loadGroups() } })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LoadMoreButton(loading: Boolean, onLoad: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            TextButton(onClick = onLoad) {
                Text(text = "加载更多")
            }
        }
    }
}

// 画廊行：展示缩略图 / 标题 / 收藏状态
@Composable
private fun PqGalleryRow(item: PqGalleryItem, pqUrl: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumb?.let { resolvePqThumb(it, pqUrl) },
            contentDescription = null,
            modifier = Modifier.size(width = 72.dp, height = 96.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(
                text = item.title ?: "未知标题",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (item.fav_status == 1) {
                Spacer(modifier = Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "已收藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// 标签组行：组名 + 组内画廊数 + 未收藏数角标 + 展开指示
@Composable
private fun PqGroupRow(group: PqGroupItem, expanded: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(text = group.group_content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = "共 ${group.count} 个画廊")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.new_count > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text(text = "${group.new_count}")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
        },
    )
}

// 服务端可能返回相对路径缩略图，拼接服务器基地址
private fun resolvePqThumb(thumb: String, pqUrl: String): String {
    if (thumb.startsWith("http://") || thumb.startsWith("https://")) return thumb
    return pqUrl.substringBeforeLast('/') + "/" + thumb.trimStart('/')
}
