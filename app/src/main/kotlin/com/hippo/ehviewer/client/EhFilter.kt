package com.hippo.ehviewer.client

import arrow.core.memoize
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.dao.QuickSearch
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
    // 复合类型标签组屏蔽（新语义：空格分隔 token，逗号兼容；A B=全含 AND，-B=排除；收藏作品跳过）
    suspend fun filterTagGroup(info: GalleryInfo): Boolean {
        // 收藏作品直接跳过过滤
        if (info.favoriteSlot != NOT_FAVORITED) return false

        return anyActive(FilterMode.TAG_GROUP) { compositeFilter ->
            // 空格分隔 token（逗号兼容视为空格），trim 过滤空 token
            val tokens = compositeFilter.text
                .replace(',', ' ')
                .split(' ')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            // 约束分类：包含（uploader/标题/标签）与排除（- 前缀，去除后可属任意类别）
            val uploaderFilters = mutableListOf<String>()
            val titleFilters = mutableListOf<String>()
            val tagFilters = mutableListOf<String>()
            val excludeUploaderFilters = mutableListOf<String>()
            val excludeTitleFilters = mutableListOf<String>()
            val excludeTagFilters = mutableListOf<String>()

            tokens.forEach { rawToken ->
                var token = rawToken
                // 1. 排除约束：- 前缀（先去掉前缀再分类）
                val excluded = token.startsWith("-")
                if (excluded) {
                    token = token.removePrefix("-").trim()
                }
                if (token.isEmpty()) return@forEach
                // 2. 展开标签缩写（f: → female: 等），再分类
                token = expandTagAbbreviation(token)
                // 3. uploader: → 上传者；"..." → 标题；其余 → 标签（matchTag 命名空间感知）
                val isUploader = token.startsWith("uploader:", ignoreCase = true)
                val isQuotedTitle = token.length > 1 && token.startsWith("\"") && token.endsWith("\"")
                when {
                    excluded && isUploader -> excludeUploaderFilters.add(token.removePrefix("uploader:").trim().lowercase())
                    excluded && isQuotedTitle -> excludeTitleFilters.add(token.substring(1, token.length - 1).trim())
                    excluded -> excludeTagFilters.add(token.lowercase())
                    isUploader -> uploaderFilters.add(token.removePrefix("uploader:").trim())
                    isQuotedTitle -> titleFilters.add(token.substring(1, token.length - 1).trim())
                    else -> tagFilters.add(token.lowercase())
                }
            }

            // 包含约束：全部满足（AND）
            val isUploaderMatched = uploaderFilters.isEmpty() ||
                uploaderFilters.all { it.equals(info.uploader, ignoreCase = true) }
            val isTitleMatched = titleFilters.isEmpty() ||
                titleFilters.all { info.title.orEmpty().contains(it, ignoreCase = true) }
            val isTagMatched = tagFilters.isEmpty() ||
                tagFilters.all { requiredTag ->
                    info.simpleTags?.any { galleryTag -> matchTag(galleryTag.lowercase(), requiredTag) } ?: false
                }
            if (!(isUploaderMatched && isTitleMatched && isTagMatched)) return@anyActive false

            // 排除约束：任一命中则整条不命中（A -B = 含 A 且不含 B 时命中）
            val excludeHit =
                excludeUploaderFilters.isNotEmpty() && excludeUploaderFilters.any { it == info.uploader?.lowercase() } ||
                    excludeTitleFilters.isNotEmpty() && excludeTitleFilters.any { info.title.orEmpty().contains(it, ignoreCase = true) } ||
                    excludeTagFilters.isNotEmpty() && excludeTagFilters.any { excludedTag ->
                        info.simpleTags?.any { galleryTag -> matchTag(galleryTag.lowercase(), excludedTag) } ?: false
                    }
            if (excludeHit) return@anyActive false

            true
        }
    }

    // ===== 隐藏列表与优先队列标签隐藏（OR 语义，任一命中即隐藏）=====

    // 隐藏列表缓存（QUICK_SEARCH HIDE_TYPE∈1..3），增删后需 refreshHideList 失效
    @Volatile
    private var hideListCache: List<QuickSearch>? = null
    private suspend fun getHideListCache(): List<QuickSearch> = hideListCache ?: EhDB.getHideList().also { hideListCache = it }

    fun refreshHideList() {
        hideListCache = null
    }

    // 优先队列标签缓存（pq_tag 表），拉取成功后 refreshPqTags 失效重建
    @Volatile
    private var pqTagCache: Set<String>? = null
    private suspend fun getPqTagCache(): Set<String> = pqTagCache ?: EhDB.getAllPqTags().toSet().also { pqTagCache = it }

    fun refreshPqTags() {
        pqTagCache = null
    }

    // 隐藏列表判定：hideListEnabled 开启时遍历隐藏列表（1=标题 contains；2=上传者 equals；3=标签 matchTag）
    suspend fun hideByList(info: GalleryInfo): Boolean {
        if (!Settings.hideListEnabled) return false
        val list = getHideListCache()
        if (list.isEmpty()) return false
        return list.any { entry ->
            when (entry.hideType) {
                1 -> info.title.orEmpty().contains(entry.keyword ?: entry.name, true)
                2 -> (entry.keyword ?: entry.name) == info.uploader
                3 -> info.simpleTags?.any { tag -> matchTag(tag.lowercase(), (entry.keyword ?: entry.name).lowercase()) } == true
                else -> false
            }
        }
    }

    // 优先队列标签判定：画廊任一 simpleTags 命中缓存 pq_tag 集合（matchTag 命名空间感知）
    suspend fun hideByPqTag(info: GalleryInfo): Boolean {
        val tags = getPqTagCache()
        if (tags.isEmpty()) return false
        return info.simpleTags?.any { tag -> tags.any { matchTag(tag.lowercase(), it.lowercase()) } } == true
    }
}
