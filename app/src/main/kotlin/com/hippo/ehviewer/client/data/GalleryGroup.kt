package com.hippo.ehviewer.client.data

data class GalleryGroup(
    val key: String,
    val name: String,
    val items: MutableList<BaseGalleryInfo> = mutableListOf(),
)

enum class GroupMode(val value: Int) {
    NONE(0),
    ARTIST(1),
    GROUP(2),
    UPLOADER(3),
    ;

    companion object {
        fun fromValue(value: Int): GroupMode = when (value) {
            1 -> ARTIST
            2 -> GROUP
            3 -> UPLOADER
            else -> NONE
        }
    }
}

fun BaseGalleryInfo.getGroupKey(mode: GroupMode): String? {
    return when (mode) {
        GroupMode.ARTIST -> getArtistTag()
        GroupMode.GROUP -> getGroupTag()
        GroupMode.UPLOADER -> uploader?.takeIf { it.isNotBlank() && it != "(Disowned)" }
        GroupMode.NONE -> null
    }
}

fun BaseGalleryInfo.getArtistTag(): String? {
    val detail = this as? GalleryDetail ?: return null
    return detail.tagGroups.find { (ns, _) -> ns == TagNamespace.Artist || ns == TagNamespace.Cosplayer }
        ?.tags?.firstOrNull()?.text
}

fun BaseGalleryInfo.getGroupTag(): String? {
    val detail = this as? GalleryDetail ?: return null
    return detail.tagGroups.find { (ns, _) -> ns == TagNamespace.Group }
        ?.tags?.firstOrNull()?.text
}

fun BaseGalleryInfo.getGroupName(mode: GroupMode): String {
    return when (mode) {
        GroupMode.ARTIST -> getArtistTag() ?: "Unknown Artist"
        GroupMode.GROUP -> getGroupTag() ?: "Unknown Group"
        GroupMode.UPLOADER -> uploader ?: "Unknown Uploader"
        GroupMode.NONE -> ""
    }
}

fun List<BaseGalleryInfo>.groupByMode(mode: GroupMode): List<GalleryGroup> {
    if (mode == GroupMode.NONE) {
        return emptyList()
    }
    val groups = mutableMapOf<String, GalleryGroup>()
    forEach { info ->
        val key = info.getGroupKey(mode)
        if (key != null) {
            val group = groups.getOrPut(key) {
                GalleryGroup(key = key, name = info.getGroupName(mode))
            }
            group.items.add(info)
        }
    }
    return groups.values.toList().sortedBy { it.name }
}