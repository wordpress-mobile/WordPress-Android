package org.wordpress.android.fluxc.tools

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class FormattableContentMapper @Inject constructor(val gson: Gson) {
    fun mapToFormattableContent(json: String): FormattableContent = gson.fromJson(json, FormattableContent::class.java)

    fun mapToFormattableContentList(json: String): List<FormattableContent> =
            gson.fromJson(json, object : TypeToken<List<FormattableContent>>() {}.type)

    fun mapToFormattableMeta(json: String): FormattableMeta = gson.fromJson(json, FormattableMeta::class.java)

    fun mapFormattableContentToJson(formattableContent: FormattableContent): String = gson.toJson(formattableContent)

    fun mapFormattableContentListToJson(formattableList: List<FormattableContent>): String =
            gson.toJson(formattableList)

    fun mapFormattableMetaToJson(formattableMeta: FormattableMeta): String = gson.toJson(formattableMeta)
}

data class FormattableContent(
    @SerializedName("actions") val actions: Map<String, Boolean>? = null,
    @SerializedName("media") val media: List<FormattableMedia>? = null,
    @SerializedName("meta") val meta: FormattableMeta? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("nest_level") val nestLevel: Int? = null,
    @SerializedName("ranges") val ranges: List<FormattableRange>? = null
)

data class FormattableMedia(
    @SerializedName("height") val height: String? = null,
    @SerializedName("width") val width: String? = null,
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
        @SerializedName("post") val post: Long? = null,
        @SerializedName("order") val order: Long? = null
    )

    data class Links(
        @SerializedName("site") val site: String? = null,
        @SerializedName("user") val user: String? = null,
        @SerializedName("comment") val comment: String? = null,
        @SerializedName("post") val post: String? = null,
        @SerializedName("email") val email: String? = null,
        @SerializedName("home") val home: String? = null,
        @SerializedName("order") val order: String? = null
    )

    data class Titles(
        @SerializedName("home") val home: String? = null,
        @SerializedName("tagline") val tagline: String? = null
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
    @SerializedName("value") val value: String? = null,
    @SerializedName("indices") val indices: List<Int>? = null
) {
    fun rangeType(): FormattableRangeType {
        return if (type != null) FormattableRangeType.fromString(type) else FormattableRangeType.fromString(section)
    }
}

enum class FormattableRangeType {
    POST,
    SITE,
    PAGE,
    COMMENT,
    USER,
    STAT,
    BLOCKQUOTE,
    FOLLOW,
    NOTICON,
    LIKE,
    MATCH,
    MEDIA,
    B,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): FormattableRangeType {
            return when (value) {
                "post" -> POST
                "site" -> SITE
                "page" -> PAGE
                "comment" -> COMMENT
                "user" -> USER
                "stat" -> STAT
                "blockquote" -> BLOCKQUOTE
                "follow" -> FOLLOW
                "noticon" -> NOTICON
                "like" -> LIKE
                "match" -> MATCH
                "media" -> MEDIA
                "b" -> B
                else -> UNKNOWN
            }
        }
    }
}
