package com.hippo.ehviewer.ui.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.builtInCensoredDoHUrls
import com.hippo.ehviewer.client.builtInDoHUrls
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.client.systemDns
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.screen.implicit
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor
import com.hippo.ehviewer.util.AdsPlaceholderFile
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.CrashHandler
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.displayPath
import com.hippo.ehviewer.util.getAppLanguage
import com.hippo.ehviewer.util.getLanguages
import com.hippo.ehviewer.util.isAtLeastO
import com.hippo.ehviewer.util.sendTo
import com.hippo.ehviewer.util.setAppLanguage
import com.hippo.files.delete
import com.hippo.files.toOkioPath
import com.jamal.composeprefs3.ui.prefs.DropDownPref
import com.jamal.composeprefs3.ui.prefs.SwitchPref
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import java.net.InetAddress
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import tech.relaycorp.doh.DoHClient

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.AdvancedScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val cloudflareIPhint = stringResource(id = R.string.settings_advanced_cloudflare_ip_hint)
    val cloudflareIPtitle = stringResource(id = R.string.settings_advanced_cloudflare_ip)
    fun launchSnackBar(content: String) = launch { showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_advanced)) },
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
                title = stringResource(id = R.string.settings_advanced_save_parse_error_body),
                summary = stringResource(id = R.string.settings_advanced_save_parse_error_body_summary),
                value = Settings::saveParseErrorBody,
            )
            val stripAds = Settings.stripExtraneousAds.asMutableState()
            SwitchPreference(
                title = stringResource(id = R.string.settings_block_extraneous_ads),
                value = stripAds.rememberedAccessor,
            )
            AnimatedVisibility(visible = stripAds.value) {
                LauncherPreference(
                    title = stringResource(id = R.string.settings_ads_placeholder),
                    contract = ActivityResultContracts.PickVisualMedia(),
                    key = PickVisualMediaRequest(mediaType = ImageOnly),
                ) { uri ->
                    withIOContext {
                        if (uri != null) {
                            uri.toOkioPath() sendTo AdsPlaceholderFile
                        } else {
                            AdsPlaceholderFile.delete()
                        }
                    }
                }
            }
            SwitchPreference(
                title = "[Self] 隐藏已收藏画廊",
                summary = "从搜索列表中隐藏已经收藏的画廊",
                value = Settings::hideFav,
            )
            SwitchPreference(
                title = "[Self] 显示过滤信息",
                summary = "显示被过滤的画廊数量",
                value = Settings::debugFilter,
            )
            SwitchPreference(
                title = "[Self] 禁用过滤",
                summary = "禁用全部本地过滤器",
                value = Settings::disableFilter,
            )
            SwitchPreference(
                title = "[Self] 快捷收藏",
                summary = "长按快捷收藏，此状态下画廊页收藏可选择收藏夹",
                value = Settings::quickFav,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_advanced_save_crash_log),
                summary = stringResource(id = R.string.settings_advanced_save_crash_log_summary),
                value = Settings::saveCrashLog,
            )
            val dumpLogError = stringResource(id = R.string.settings_advanced_dump_logcat_failed)
            LauncherPreference(
                title = stringResource(id = R.string.settings_advanced_dump_logcat),
                summary = stringResource(id = R.string.settings_advanced_dump_logcat_summary),
                contract = ActivityResultContracts.CreateDocument("application/zip"),
                key = "log-" + ReadableTime.getFilenamableTime() + ".zip",
            ) { uri ->
                uri?.run {
                    runCatching {
                        grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val files = ArrayList<File>()
                            AppConfig.externalParseErrorDir?.listFiles()?.let { files.addAll(it) }
                            AppConfig.externalCrashDir?.listFiles()?.let { files.addAll(it) }
                            ZipOutputStream(outputStream).use { zipOs ->
                                files.forEach { file ->
                                    if (!file.isFile) return@forEach
                                    val entry = ZipEntry(file.name)
                                    zipOs.putNextEntry(entry)
                                    file.inputStream().use { it.copyTo(zipOs) }
                                }
                                val logcatEntry = ZipEntry("logcat-" + ReadableTime.getFilenamableTime() + ".txt")
                                zipOs.putNextEntry(logcatEntry)
                                CrashHandler.collectInfo(zipOs.writer())
                                Runtime.getRuntime().exec("logcat -d").inputStream.use { it.copyTo(zipOs) }
                            }
                            launchSnackBar(getString(R.string.settings_advanced_dump_logcat_to, uri.displayPath))
                        }
                    }.onFailure {
                        launchSnackBar(dumpLogError)
                        logcat(it)
                    }
                }
            }
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_advanced_read_cache_size),
                entry = R.array.read_cache_size_entries,
                entryValueRes = R.array.read_cache_size_entry_values,
                value = Settings::readCacheSize.observed,
            )
            var currentLanguage by remember { mutableStateOf(getAppLanguage()) }
            val languages = remember { getLanguages() }
            DropDownPref(
                title = stringResource(id = R.string.settings_advanced_app_language_title),
                defaultValue = currentLanguage,
                onValueChange = {
                    setAppLanguage(it)
                    currentLanguage = it
                },
                useSelectedAsSummary = true,
                entries = languages,
            )
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
            var addKeyword by Settings::addKeyword.observed
            Preference(
                title = "AddKeyword",
                summary = addKeyword ?: "Not set",
            ) {
                coroutineScope.launch {
                    val newaddKeyword = awaitInputText(
                        initial = addKeyword ?: "",
                        title = "设置快捷添加搜索词",
                        hint = "keyword",
                    )
                    // 空字符串转为 null 存储
                    addKeyword = newaddKeyword.ifBlank { null }
                }
            }
            var enableCronet by Settings::enableQuic.observed
            var enableDf by Settings::dF.observed
            SwitchPreference(
                title = stringResource(id = R.string.settings_advanced_bypass_vpn_title),
                summary = stringResource(id = R.string.settings_advanced_bypass_vpn_summary),
                value = Settings::bypassVpn,
            )
            Preference(
                title = stringResource(id = R.string.settings_advanced_http_engine),
                summary = if (enableCronet) "Cronet" else "OkHttp",
            ) {
                coroutineScope.launch {
                    awaitConfirmationOrCancel(
                        title = R.string.settings_advanced_http_engine,
                        showCancelButton = false,
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.align(Alignment.Center)) {
                                SegmentedButton(
                                    selected = !enableCronet,
                                    onClick = {
                                        enableCronet = false
                                        enableDf = true
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                ) {
                                    Text("OkHttp")
                                }
                                SegmentedButton(
                                    selected = enableCronet,
                                    onClick = {
                                        enableCronet = true
                                        enableDf = false
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                ) {
                                    Text("Cronet")
                                }
                            }
                        }
                    }
                }
            }
            val ifCloudflareIPOverride = Settings::cloudflareIpOverride.observed
            AnimatedVisibility(visible = enableCronet) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_advanced_cloudflare_ip_override),
                    summary = stringResource(id = R.string.settings_advanced_cloudflare_ip_override_summary),
                    value = ifCloudflareIPOverride.rememberedAccessor,
                )
            }
            var cloudflareIp by Settings::cloudflareIp.observed
            AnimatedVisibility(visible = ifCloudflareIPOverride.value && enableCronet) {
                Preference(
                    title = cloudflareIPtitle,
                    summary = cloudflareIp,
                ) {
                    coroutineScope.launch {
                        val newCloudflareIP = awaitInputText(
                            initial = Settings.cloudflareIp.toString(),
                            title = cloudflareIPtitle,
                            hint = cloudflareIPhint,
                        )
                        if (newCloudflareIP.isNotEmpty()) {
                            cloudflareIp = newCloudflareIP
                        }
                    }
                }
            }
            var dohUrl by Settings::dohUrl.observed
            AnimatedVisibility(visible = enableDf) {
                Preference(title = stringResource(id = R.string.settings_advanced_dns_over_http_title)) {
                    coroutineScope.launch {
                        val newDoHUrl = awaitInputText(
                            initial = Settings.dohUrl?.toString() ?: "",
                            title = context.getString(R.string.settings_advanced_dns_over_http_title),
                            hint = context.getString(R.string.settings_advanced_dns_over_http_hint),
                            invalidator = { input ->
                                if (input.isNotEmpty()) {
                                    runCatching {
                                        if (input.isNotBlank()) {
                                            buildDoHDNS(input)
                                        }
                                    }.onFailure {
                                        raise("Invalid URL!")
                                    }
                                }
                            },
                        )
                        dohUrl = newDoHUrl
                    }
                }
            }
            AnimatedVisibility(visible = enableDf) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_advanced_ech_title),
                    value = Settings::enableECH,
                )
            }
            if (isAtLeastO) {
                IntSliderPreference(
                    maxValue = 16384,
                    step = 3,
                    title = stringResource(id = R.string.settings_advanced_hardware_bitmap_threshold),
                    summary = stringResource(id = R.string.settings_advanced_hardware_bitmap_threshold_summary),
                    value = Settings::hardwareBitmapThreshold,
                )
            }
            SwitchPreference(
                title = stringResource(id = R.string.preload_thumb_aggressively),
                value = Settings::preloadThumbAggressively,
            )
            var animateItems by Settings.animateItems.asMutableState()
            SwitchPref(
                checked = animateItems,
                onMutate = { animateItems = !animateItems },
                title = stringResource(id = R.string.animate_items),
                summary = stringResource(id = R.string.animate_items_summary),
            )
            var desktopSite by Settings.desktopSite.asMutableState()
            SwitchPref(
                checked = desktopSite,
                onMutate = { desktopSite = !desktopSite },
                title = stringResource(id = R.string.desktop_site),
                summary = stringResource(id = R.string.desktop_site_summary),
            )
            val exportFailed = stringResource(id = R.string.settings_advanced_export_data_failed)
            LauncherPreference(
                title = stringResource(id = R.string.settings_advanced_export_data),
                summary = stringResource(id = R.string.settings_advanced_export_data_summary),
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
                key = ReadableTime.getFilenamableTime() + ".db",
            ) { uri ->
                uri?.let {
                    runCatching {
                        grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        EhDB.exportDB(implicit<Context>(), uri.toOkioPath())
                        launchSnackBar(getString(R.string.settings_advanced_export_data_to, uri.displayPath))
                    }.onFailure {
                        logcat(it)
                        launchSnackBar(exportFailed)
                    }
                }
            }
            val importFailed = stringResource(id = R.string.cant_read_the_file)
            val importSucceed = stringResource(id = R.string.settings_advanced_import_data_successfully)
            LauncherPreference(
                title = stringResource(id = R.string.settings_advanced_import_data),
                summary = stringResource(id = R.string.settings_advanced_import_data_summary),
                contract = ActivityResultContracts.GetContent(),
                key = "application/octet-stream",
            ) { uri ->
                uri?.let {
                    runCatching {
                        grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        EhDB.importDB(implicit<Context>(), uri)
                        launchSnackBar(importSucceed)
                    }.onFailure {
                        logcat(it)
                        launchSnackBar(importFailed)
                    }
                }
            }
            val hasSignedIn by Settings.hasSignedIn.collectAsState()
            if (hasSignedIn) {
                val backupNothing = stringResource(id = R.string.settings_advanced_backup_favorite_nothing)
                val backupFailed = stringResource(id = R.string.settings_advanced_backup_favorite_failed)
                val backupSucceed = stringResource(id = R.string.settings_advanced_backup_favorite_success)
                Preference(
                    title = stringResource(id = R.string.settings_advanced_backup_favorite),
                    summary = stringResource(id = R.string.settings_advanced_backup_favorite_summary),
                ) {
                    val favListUrlBuilder = FavListUrlBuilder()
                    var favTotal = 0
                    var favIndex = 0
                    tailrec suspend fun doBackup() {
                        val result = EhEngine.getFavorites(favListUrlBuilder.build())
                        if (result.galleryInfoList.isEmpty()) {
                            launchSnackBar(backupNothing)
                        } else {
                            if (favTotal == 0) favTotal = result.countArray.sum()
                            favIndex += result.galleryInfoList.size
                            val status = "($favIndex/$favTotal)"
                            EhDB.putLocalFavorites(result.galleryInfoList)
                            launchSnackBar(getString(R.string.settings_advanced_backup_favorite_start, status))
                            if (result.next != null) {
                                delay(Settings.downloadDelay.toLong())
                                favListUrlBuilder.setIndex(result.next, true)
                                doBackup()
                            }
                        }
                    }
                    launchIO {
                        runSuspendCatching {
                            doBackup()
                        }.onSuccess {
                            launchSnackBar(backupSucceed)
                        }.onFailure {
                            logcat(it)
                            launchSnackBar(backupFailed)
                        }
                    }
                }
            }
            Preference(title = stringResource(id = R.string.open_by_default)) {
                try {
                    @SuppressLint("InlinedApi")
                    val intent = Intent(
                        ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                        "package:$packageName".toUri(),
                    )
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    val intent = Intent(
                        ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:$packageName".toUri(),
                    )
                    startActivity(intent)
                }
            }
        }
    }
}

private fun getEffectiveDoHUrl(): String {
    val userDefinedUrl = Settings.dohUrl
    if (!userDefinedUrl.isNullOrBlank()) {
        return userDefinedUrl
    }
    val randomizedUrls = builtInDoHUrls.shuffled()
    val selectedUrl = randomizedUrls.firstOrNull()
    return selectedUrl ?: throw IllegalStateException("No DoH URLs available")
}

private fun buildDoHDNS(url: String): DnsOverHttps = DnsOverHttps.Builder().apply {
    client(EhApplication.okHttpClient)
    url(url.toHttpUrl())
    post(true)
    systemDns(systemDns)
}.build()

private var doh: DnsOverHttps? = getEffectiveDoHUrl().runCatching { buildDoHDNS(this) }.getOrNull()
var censoredDoh = builtInCensoredDoHUrls.shuffled().first().runCatching { DoHClient(this) }.getOrNull()

object EhDoH {
    fun lookup(hostname: String): List<InetAddress>? = doh?.runCatching { lookup(hostname).takeIf { it.isNotEmpty() } }?.onFailure { it.printStackTrace() }?.getOrNull()
}
