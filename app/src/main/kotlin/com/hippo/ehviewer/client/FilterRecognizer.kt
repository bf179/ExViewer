package com.hippo.ehviewer.client

import com.hippo.ehviewer.dao.FilterMode

// 展开标签缩写（cos:/p:/f:/m:/a:/x:/o:/l:/g:/c:），只在令牌开头匹配，
// 避免旧的全局替换把 "group:" 误替换成 "grouparody:" 等顺序 bug
internal fun expandTagAbbreviation(token: String): String = when {
    token.startsWith("cos:") -> "cosplayer:" + token.removePrefix("cos:")
    token.startsWith("p:") -> "parody:" + token.removePrefix("p:")
    token.startsWith("f:") -> "female:" + token.removePrefix("f:")
    token.startsWith("m:") -> "male:" + token.removePrefix("m:")
    token.startsWith("a:") -> "artist:" + token.removePrefix("a:")
    token.startsWith("x:") -> "mixed:" + token.removePrefix("x:")
    token.startsWith("o:") -> "other:" + token.removePrefix("o:")
    token.startsWith("l:") -> "language:" + token.removePrefix("l:")
    token.startsWith("g:") -> "group:" + token.removePrefix("g:")
    token.startsWith("c:") -> "character:" + token.removePrefix("c:")
    else -> token
}

// 过滤输入智能识别：复合($/逗号) > 标题("...") > 命名空间(artist:/uploader:等) > 裸文本标题。
// 返回 (处理后的内容, 过滤模式, 识别模式显示名)，供过滤快捷添加与隐藏列表快捷添加共用。
internal fun recognizeFilterInput(raw: String): Triple<String, FilterMode, String> {
    // 1. 含 $ 或逗号 → 复合标签（TAG_GROUP）：去掉 $ 操作符，按逗号拆分令牌并展开缩写
    if ('$' in raw || ',' in raw) {
        val processed = raw.replace("$", "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(",") { token -> expandTagAbbreviation(token) }
        return Triple(processed, FilterMode.TAG_GROUP, "复合标签")
    }
    // 2. "..." 包裹 → 标题
    if (raw.length > 1 && raw.startsWith('"') && raw.endsWith('"')) {
        return Triple(raw.removeSurrounding("\"").trim(), FilterMode.TITLE, "标题")
    }
    // 3. 带命名空间前缀 → 对应单标签/上传者（先展开缩写再判定）
    val expanded = expandTagAbbreviation(raw)
    if (expanded.startsWith("uploader:")) {
        return Triple(expanded.removePrefix("uploader:").trim(), FilterMode.UPLOADER, "上传者")
    }
    val namespaces = listOf(
        "parody:", "female:", "male:", "artist:", "mixed:", "other:",
        "language:", "group:", "character:", "cosplayer:",
    )
    if (namespaces.any { expanded.startsWith(it) }) {
        return Triple(expanded, FilterMode.TAG, "单标签")
    }
    // 4. 其余裸文本 → 标题
    return Triple(raw, FilterMode.TITLE, "标题")
}
