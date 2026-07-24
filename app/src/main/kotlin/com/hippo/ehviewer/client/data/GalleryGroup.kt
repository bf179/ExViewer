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

fun BaseGalleryInfo.extractTitleBracket(): String? {
    val title = title ?: return null
    val match = Regex("^\\[(.*?)\\]").find(title)
    return match?.groupValues?.get(1)
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
    val bracketToGroupKey = mutableMapOf<String, String>()

    forEach { info ->
        val tagKey = info.extractClusterKey()
        val bracketKey = info.extractTitleBracket()

        if (tagKey != null) {
            val group = groups.getOrPut(tagKey) {
                GalleryGroup(key = tagKey, name = info.clusterDisplayName())
            }
            group.items.add(info)
            if (bracketKey != null) {
                bracketToGroupKey[bracketKey] = tagKey
            }
        } else if (bracketKey != null) {
            val existingGroupKey = bracketToGroupKey[bracketKey]
            if (existingGroupKey != null && groups.containsKey(existingGroupKey)) {
                groups[existingGroupKey]?.items?.add(info)
            } else {
                val bracketTagKey = "bracket:$bracketKey"
                val group = groups.getOrPut(bracketTagKey) {
                    GalleryGroup(key = bracketTagKey, name = "[$bracketKey]")
                }
                group.items.add(info)
                bracketToGroupKey[bracketKey] = bracketTagKey
            }
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
