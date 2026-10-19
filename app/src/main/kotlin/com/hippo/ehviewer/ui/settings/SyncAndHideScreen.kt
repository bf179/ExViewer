package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.HideListScreenDestination
import com.hippo.ehviewer.ui.tools.observed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 设置二级页"同步与隐藏过滤"：收纳收藏同步、优先队列地址、隐藏列表开关与管理/同步入口
@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.SyncAndHideScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    fun launchSnackBar(content: String) = launch { showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "同步与隐藏过滤") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState()).padding(paddingValues)) {
            SwitchPreference(
                title = "[Self] 同步收藏变动到云端",
                summary = "仅在sapi不为空时有效",
                value = Settings::syncFav,
            )
            var showSapi by Settings::syncFav.observed
            var sapiUrl by Settings::sapiUrl.observed
            AnimatedVisibility(visible = showSapi) {
                Preference(
                    title = "SAPI",
                    summary = sapiUrl ?: "Not set",
                ) {
                    coroutineScope.launch {
                        val newSapiUrl = awaitInputText(
                            initial = sapiUrl ?: "",
                            title = "Set SAPI Endpoint",
                            hint = "https://api.example.com/exlocal",
                        )
                        // 空字符串转为 null 存储
                        sapiUrl = newSapiUrl.ifBlank { null }
                    }
                }
            }
            var papiUrl by Settings::papiUrl.observed
            Preference(
                title = "PAPI",
                summary = papiUrl ?: "Not set",
            ) {
                coroutineScope.launch {
                    val newPapiUrl = awaitInputText(
                        initial = papiUrl ?: "",
                        title = "Set PAPI Endpoint",
                        hint = "https://api.example.com/pq",
                    )
                    // 空字符串转为 null 存储
                    papiUrl = newPapiUrl.ifBlank { null }
                }
            }
            var pqUrl by Settings::pqUrl.observed
            Preference(
                title = "优先队列地址 (PQ URL)",
                summary = pqUrl ?: "Not set",
            ) {
                coroutineScope.launch {
                    val newPqUrl = awaitInputText(
                        initial = pqUrl ?: "",
                        title = "Set PQ Endpoint",
                        hint = "https://api.example.com/pq_galleries",
                    )
                    // 空字符串转为 null 存储
                    pqUrl = newPqUrl.ifBlank { null }
                }
            }
            var apiToken by Settings::apiToken.observed
            Preference(
                title = "API Token",
                summary = apiToken?.let { "已配置 (${it.take(4)}…)" } ?: "Not set",
            ) {
                coroutineScope.launch {
                    val newApiToken = awaitInputText(
                        initial = apiToken ?: "",
                        title = "Set API Token",
                        hint = "与服务器 server.api_token 保持一致",
                    )
                    // 空字符串转为 null 存储
                    apiToken = newApiToken.ifBlank { null }
                }
            }
            SwitchPreference(
                title = "[Self] 启用隐藏列表",
                summary = "在搜索/主页/热门/排行中应用隐藏列表（历史页不生效）",
                value = Settings::hideListEnabled,
            )
            SwitchPreference(
                title = "[Self] 隐藏优先队列标签画廊",
                summary = "在搜索/热门/排行中隐藏命中优先队列标签的画廊",
                value = Settings::hidePqTagged,
            )
            SwitchPreference(
                title = "[Self] 历史页隐藏优先队列标签画廊",
                summary = "历史记录页独立控制（不随 hidePqTagged）",
                value = Settings::hidePqTaggedInHistory,
            )
            Preference(
                title = "[Self] 隐藏列表管理",
                summary = "管理标题/上传者/标签三类隐藏条目",
            ) { navigator.navigate(HideListScreenDestination) }
            // 隐藏列表同步：先拉取服务器条目合并，再上传本地全量（需 PQ URL 与 API Token）
            Preference(
                title = "[Self] 同步隐藏列表",
                summary = "拉取合并服务器条目并上传本地全量",
            ) {
                coroutineScope.launch {
                    if (EhDB.syncHideList()) {
                        launchSnackBar("隐藏列表同步完成")
                    }
                }
            }
        }
    }
}
