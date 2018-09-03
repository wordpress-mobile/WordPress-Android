package org.wordpress.android.fluxc.store

class FormattableContentStore {
    fun map(text: String): FormattableContent {
        TODO()
    }
}

class FormattableContent(val text: String? = null, val ranges: List<FormattableRange>? = null)

class FormattableRange(
    val siteId: Long? = null,
    val postId: Long? = null,
    val rootId: Long? = null,
    val rangeType: FormattableRangeType? = null,
    val url: String? = null,
    val indices: IntArray? = null
)

enum class FormattableRangeType {
    POST,
    SITE,
    COMMENT,
    USER,
    STAT,
    BLOCKQUOTE,
    FOLLOW,
    NOTICON,
    LIKE,
    MATCH,
    UNKNOWN;

    fun fromString(value: String?): FormattableRangeType {
        return when (value) {
            "post" -> POST
            "site" -> SITE
            "comment" -> COMMENT
            "user" -> USER
            "stat" -> STAT
            "blockquote" -> BLOCKQUOTE
            "follow" -> FOLLOW
            "noticon" -> NOTICON
            "like" -> LIKE
            "match" -> MATCH
            else -> UNKNOWN
        }
    }
}
