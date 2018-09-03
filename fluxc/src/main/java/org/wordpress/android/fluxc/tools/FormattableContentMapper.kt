package org.wordpress.android.fluxc.tools

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import javax.inject.Inject

class FormattableContentMapper @Inject constructor(val gson: Gson) {
    fun mapToFormattableContent(json: String): FormattableContent = gson.fromJson(json, FormattableContent::class.java)

    fun mapFormattableContentToJson(formattableContent: FormattableContent): String = gson.toJson(formattableContent)
}

data class FormattableContent(@SerializedName("text") val text: String? = null, @SerializedName("ranges") val ranges: List<FormattableRange>? = null)

data class FormattableRange(
    @SerializedName("siteID")
    val siteId: Long? = null,
    @SerializedName("postID")
    val postId: Long? = null,
    @SerializedName("rootID")
    val rootId: Long? = null,
    @SerializedName("type")
    val rangeType: FormattableRangeType? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("indices")
    val indices: List<Int>? = null
)

enum class FormattableRangeType {
    @SerializedName("post")
    POST,
    @SerializedName("site")
    SITE,
    @SerializedName("comment")
    COMMENT,
    @SerializedName("user")
    USER,
    @SerializedName("stat")
    STAT,
    @SerializedName("blockquote")
    BLOCKQUOTE,
    @SerializedName("follow")
    FOLLOW,
    @SerializedName("noticon")
    NOTICON,
    @SerializedName("like")
    LIKE,
    @SerializedName("match")
    MATCH
}
