package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.tools.Await
import com.hippo.ehviewer.ui.tools.thenIf
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.coroutines.resume
import kotlinx.coroutines.launch

// 隐藏列表管理：标题/上传者/标签三类隐藏条目增删（无逐条开关）
@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.HideListScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hideEntries = remember { mutableStateListOf<QuickSearch>() }
    val animateItems by Settings.animateItems.collectAsState()

    // 加载隐藏列表（首次访问会触发一次性 HIDE_TYPE 数据分类）
    LaunchedEffect(Unit) {
        val list = EhDB.getHideList()
        hideEntries.addAll(list)
    }

    // 隐藏类型显示名
    fun typeName(hideType: Int): String = when (hideType) {
        1 -> "标题"
        2 -> "上传者"
        3 -> "标签"
        else -> "未知"
    }

    fun addEntry() {
        launch {
            dialog { cont ->
                val typeNames = listOf("标题", "上传者", "标签")
                val typeValues = listOf(1, 2, 3)
                val type = rememberTextFieldState(typeNames[0])
                val state = rememberTextFieldState()
                var error by remember { mutableStateOf<String?>(null) }
                fun invalidateAndSave() {
                    if (state.text.isBlank()) {
                        error = "内容不能为空"
                        return
                    }
                    error = null
                    val hideType = typeValues[typeNames.indexOf(type.text)]
                    val text = state.text.toString().trim()
                    launch {
                        EhDB.addHideEntry(text, hideType)
                        hideEntries.addAll(EhDB.getHideList().filter { entry -> hideEntries.none { it.id == entry.id } })
                    }
                    cont.resume(Unit)
                }
                AlertDialog(
                    onDismissRequest = { cont.cancel() },
                    confirmButton = {
                        TextButton(onClick = ::invalidateAndSave) {
                            Text(text = "添加")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { cont.cancel() }) {
                            Text(text = "取消")
                        }
                    },
                    title = { Text(text = "添加隐藏条目") },
                    text = {
                        var expanded by remember { mutableStateOf(false) }
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    readOnly = true,
                                    state = type,
                                    label = { Text(text = "隐藏类型") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    typeNames.forEach {
                                        DropdownMenuItem(
                                            text = { Text(text = it) },
                                            onClick = {
                                                expanded = false
                                                type.setTextAndPlaceCursorAtEnd(it)
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                        )
                                    }
                                }
                            }
                            val isError = error != null
                            OutlinedTextField(
                                state = state,
                                label = { Text(text = "内容") },
                                supportingText = { error?.let { Text(text = it) } },
                                trailingIcon = {
                                    if (isError) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                isError = isError,
                                lineLimits = TextFieldLineLimits.SingleLine,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                            )
                        }
                    },
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "隐藏列表") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        launch {
                            awaitConfirmationOrCancel(showCancelButton = false) {
                                Text(text = "标题：画廊标题包含该文本时隐藏；上传者：画廊上传者等于该文本时隐藏；标签：画廊标签命中该标签时隐藏。\n\n任一隐藏条目命中即隐藏该画廊（OR 语义）。")
                            }
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.Help, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = ::addEntry) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            (1..3).forEach { hideType ->
                val entries = hideEntries.filter { it.hideType == hideType }
                if (entries.isNotEmpty()) {
                    item(key = "header_$hideType") {
                        Text(
                            text = typeName(hideType),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).thenIf(animateItems) { animateItem() },
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(entries, key = { requireNotNull(it.id) }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().thenIf(animateItems) { animateItem() },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = entry.name,
                                modifier = Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 8.dp),
                            )
                            IconButton(
                                onClick = {
                                    launch {
                                        awaitConfirmationOrCancel {
                                            Text(text = "删除隐藏条目 \"${entry.name}\"?")
                                        }
                                        EhDB.removeHideEntry(entry)
                                        hideEntries.remove(entry)
                                    }
                                },
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
            item {
                Await({ hideEntries }) {
                    if (hideEntries.isEmpty()) {
                        Column(
                            modifier = Modifier.padding(paddingValues).fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = Modifier.size(80.dp))
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp).size(120.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "暂无隐藏条目",
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
