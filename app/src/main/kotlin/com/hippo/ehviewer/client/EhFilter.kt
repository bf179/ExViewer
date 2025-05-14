package com.hippo.ehviewer.client

import arrow.core.memoize
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private val regex = { p: Filter -> Regex(p.text) }.memoize()

object EhFilter : CoroutineScope {
    override val coroutineContext = Dispatchers.IO.limitedParallelism(1)
    val filters = async { EhDB.getAllFilter() as MutableList }
    private suspend inline fun anyActive(mode: FilterMode, predicate: (Filter) -> Boolean) = filters.await().any { it.mode == mode && it.enable && predicate(it) }
    private fun <R> Filter.launchOps(
        callback: (suspend (R) -> Unit)? = null,
        ops: suspend Filter.() -> R,
    ) = launch { ops().let { callback?.invoke(it) } }
    fun Filter.remember(callback: (suspend (Boolean) -> Unit)? = null) = launchOps(callback) {
        EhDB.addFilter(this).also { if (it) filters.await().add(this) }
    }
    fun Filter.trigger(callback: (suspend (Unit) -> Unit)? = null) = launchOps(callback) {
        enable = !enable
        EhDB.updateFilter(this)
    }
    fun Filter.forget(callback: (suspend (Unit) -> Unit)? = null) = launchOps(callback) {
        EhDB.deleteFilter(this)
        filters.await().remove(this)
    }

    private fun spiltTag(tag: String) = tag.run {
        val index = indexOf(':')
        if (index < 0) null to this else substring(0, index) to substring(index + 1)
    }

    private fun matchTag(tag: String, filter: String): Boolean {
        val (tagNamespace, tagName) = spiltTag(tag)
        val (filterNamespace, filterName) = spiltTag(filter)
        return if (null != tagNamespace && null != filterNamespace && tagNamespace != filterNamespace) {
            false
        } else {
            tagName == filterName
        }
    }

    private fun matchTagNamespace(tag: String, filter: String): Boolean {
        val (nameSpace, _) = spiltTag(tag)
        return nameSpace == filter
    }

    suspend fun needTags() = filters.await().any { it.enable && (it.mode == FilterMode.TAG || it.mode == FilterMode.TAG_NAMESPACE) }
    suspend fun filterTitle(info: GalleryInfo) = anyActive(FilterMode.TITLE) { info.title.orEmpty().contains(it.text, true) }
    suspend fun filterUploader(info: GalleryInfo) = anyActive(FilterMode.UPLOADER) { it.text == info.uploader }
    suspend fun filterTag(info: GalleryInfo) = info.simpleTags?.any { tag -> anyActive(FilterMode.TAG) { matchTag(tag, it.text.lowercase()) } } == true
    suspend fun filterTagNamespace(info: GalleryInfo) = info.simpleTags?.any { tag -> anyActive(FilterMode.TAG_NAMESPACE) { matchTagNamespace(tag, it.text.lowercase()) } } == true
    suspend fun filterCommenter(commenter: String) = anyActive(FilterMode.COMMENTER) { it.text == commenter }
    suspend fun filterComment(comment: String) = anyActive(FilterMode.COMMENT) { regex(it).containsMatchIn(comment) }

    // suspend fun filterFav(info: GalleryInfo) = anyActive(FilterMode.TITLE) { it.text.contains("已收藏", true) } && info.favoriteSlot != -2
    suspend fun filterFav(info: GalleryInfo): Boolean {
        val hidefav = Settings.hideFav
        return hidefav && info.favoriteSlot != -2
    }

    // // 标签组屏蔽
    // suspend fun filterTagGroup(info: GalleryInfo) = anyActive(FilterMode.TAG_GROUP) { tagGroupFilter ->
    //     // 将标签组文本按逗号分割并处理
    //     val requiredTags = tagGroupFilter.text.split(',')
    //         .map { it.trim().lowercase() }
    //         .filter { it.isNotEmpty() }
    //     // 检查画廊是否包含该组中的所有标签
    //     requiredTags.all { requiredTag ->
    //         info.simpleTags?.any { galleryTag ->
    //             matchTag(galleryTag.lowercase(), requiredTag)
    //         } ?: false
    //     }
    // }
    // 复合类型标签组屏蔽_filterCompositeTags
    suspend fun filterTagGroup(info: GalleryInfo): Boolean {
        // 收藏作品直接跳过过滤
        if (info.favoriteSlot != -2) return false

        return anyActive(FilterMode.TAG_GROUP) { compositeFilter ->
            // 用逗号分割复合标签文本
            val parts = compositeFilter.text.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            // 初始化分组
            val uploaderFilters = mutableListOf<String>()
            val titleFilters = mutableListOf<String>()
            val blacklistTagFilters = mutableListOf<String>() // 黑名单标签（全部匹配）
            val whitelistTagFilters = mutableListOf<String>() // 白名单标签（任意匹配则不过滤）

            // 分类规则：
            parts.forEach { part ->
                when {
                    // 1. uploader: → 上传者
                    part.startsWith("uploader:") -> {
                        uploaderFilters.add(part.removePrefix("uploader:").trim())
                    }
                    // 2. 双引号包裹 → 标题组
                    part.startsWith("\"") && part.endsWith("\"") && part.length > 1 -> {
                        titleFilters.add(part.substring(1, part.length - 1).trim())
                    }
                    // 3. 以 "-" 开头 → 白名单标签
                    part.startsWith("-") -> {
                        whitelistTagFilters.add(part.substring(1).lowercase())
                    }
                    // 4. 其他 → 黑名单标签
                    else -> {
                        blacklistTagFilters.add(part.lowercase())
                    }
                }
            }

            // 0. 如果存在白名单标签，且匹配任意一个 → 直接跳过过滤（不屏蔽）
            val hasWhitelistMatch = whitelistTagFilters.isNotEmpty() &&
                info.simpleTags?.any { galleryTag ->
                    whitelistTagFilters.any { whiteTag ->
                        matchTag(galleryTag.lowercase(), whiteTag)
                    }
                } ?: false
            if (hasWhitelistMatch) return@anyActive false

            // 1. 上传者检测（如果非空，必须全部匹配）
            val isUploaderMatched = uploaderFilters.isEmpty() ||
                uploaderFilters.all { it.equals(info.uploader, ignoreCase = true) }

            // 2. 标题检测（如果非空，必须全部匹配）
            val isTitleMatched = titleFilters.isEmpty() ||
                titleFilters.all { info.title.orEmpty().contains(it, ignoreCase = true) }

            // 3. 黑名单标签检测（如果非空，必须全部匹配）
            val isBlacklistTagMatched = blacklistTagFilters.isEmpty() ||
                blacklistTagFilters.all { requiredTag ->
                    info.simpleTags?.any { galleryTag ->
                        matchTag(galleryTag.lowercase(), requiredTag)
                    } ?: false
                }

            // 最终结果：必须同时满足上传者、标题、黑名单标签的条件
            isUploaderMatched && isTitleMatched && isBlacklistTagMatched
        }
    }
}
