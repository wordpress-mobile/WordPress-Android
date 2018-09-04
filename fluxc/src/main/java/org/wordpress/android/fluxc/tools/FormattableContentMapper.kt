package org.wordpress.android.fluxc.tools

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import javax.inject.Inject

class FormattableContentMapper @Inject constructor(val gson: Gson) {
    fun mapToFormattableContent(json: String): FormattableContent = gson.fromJson(json, FormattableContent::class.java)

    fun mapFormattableContentToJson(formattableContent: FormattableContent): String = gson.toJson(formattableContent)
}

data class FormattableContent(
    @SerializedName("actions") val actions: Map<String, Boolean>? = null,
    @SerializedName("media") val media: List<FormattableMedia>? = null,
    @SerializedName("meta") val meta: FormattableMeta? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("ranges") val ranges: List<FormattableRange>? = null
)

data class FormattableMedia(
    @SerializedName("height") val height: Int? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("indices") val indices: List<Int>? = null
)

data class FormattableMeta(
    @SerializedName("ids") val ids: Ids? = null,
    @SerializedName("links") val links: Links? = null,
    @SerializedName("titles") val titles: Titles? = null
) {
    data class Ids(
        @SerializedName("site") val site: Long? = null,
        @SerializedName("user") val user: Long? = null,
        @SerializedName("comment") val comment: Long? = null,
        @SerializedName("post") val post: Long? = null
    )

    data class Links(
        @SerializedName("site") val site: String? = null,
        @SerializedName("user") val user: String? = null,
        @SerializedName("comment") val comment: String? = null,
        @SerializedName("post") val post: String? = null,
        @SerializedName("email") val email: String? = null,
        @SerializedName("home") val home: String? = null
    )

    data class Titles(
        @SerializedName("home") val home: String? = null
    )
}

data class FormattableRange(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("site_id") val siteId: Long? = null,
    @SerializedName("post_id") val postId: Long? = null,
    @SerializedName("root_id") val rootId: Long? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("section") val section: String? = null,
    @SerializedName("intent") val intent: String? = null,
    @SerializedName("context") val context: String? = null,
    @SerializedName("indices") val indices: List<Int>? = null
) {
    val rangeType by lazy { FormattableRangeType.fromString(type) }
}

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

    companion object {
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
}
