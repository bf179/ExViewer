package com.hippo.ehviewer.client.data

data class GalleryGroup(
    val key: String,
    val name: String,
    val items: MutableList<BaseGalleryInfo> = mutableListOf(),
)

sealed class GroupListItem {
    data class Header(val group: GalleryGroup) : GroupListItem()
    data class Item(val info: BaseGalleryInfo, val groupKey: String) : GroupListItem()
}

fun BaseGalleryInfo.extractClusterKey(): String? {
    val tags = simpleTags ?: return null
    var artist: String? = null
    var group: String? = null
    for (tag in tags) {
        if (tag.startsWith("artist:") || tag.startsWith("cosplayer:")) {
            if (artist == null) artist = tag
        } else if (tag.startsWith("group:")) {
            if (group == null) group = tag
        }
    }
    return artist ?: group
}

fun BaseGalleryInfo.clusterDisplayName(): String {
    val tags = simpleTags ?: return "Unknown"
    for (tag in tags) {
        if (tag.startsWith("artist:")) return tag.substringAfter("artist:")
        if (tag.startsWith("cosplayer:")) return tag.substringAfter("cosplayer:")
    }
    for (tag in tags) {
        if (tag.startsWith("group:")) return tag.substringAfter("group:")
    }
    return "Unknown"
}

fun List<BaseGalleryInfo>.clusterByTag(): List<GroupListItem> {
    val groups = LinkedHashMap<String, GalleryGroup>()
    val orphanItems = mutableListOf<BaseGalleryInfo>()

    forEach { info ->
        val key = info.extractClusterKey()
        if (key != null) {
            val group = groups.getOrPut(key) {
                GalleryGroup(key = key, name = info.clusterDisplayName())
            }
            group.items.add(info)
        } else {
            orphanItems.add(info)
        }
    }

    val result = mutableListOf<GroupListItem>()
    groups.values.forEach { group ->
        if (group.items.size > 1) {
            result.add(GroupListItem.Header(group))
            group.items.forEach { info ->
                result.add(GroupListItem.Item(info, group.key))
            }
        } else {
            group.items.forEach { info ->
                result.add(GroupListItem.Item(info, ""))
            }
        }
    }
    orphanItems.forEach { info ->
        result.add(GroupListItem.Item(info, ""))
    }
    return result
}
