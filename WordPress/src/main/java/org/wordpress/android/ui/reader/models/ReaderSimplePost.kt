package org.wordpress.android.ui.reader.models

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.JSONUtils
import org.wordpress.android.util.PhotonUtils

/**
 * Simplified version of a reader post, as returned by the related posts endpoint
 */
class ReaderSimplePost private constructor(
    val postId: Long,
    val siteId: Long,
    val title: String,
    val excerpt: String,
    val siteName: String,
    val featuredImageUrl: String,
    val railcarJson: String?
) {
    private var featuredImageForDisplay: String? = null

    fun hasTitle() = title.isNotEmpty()

    fun hasExcerpt() = excerpt.isNotEmpty()

    fun hasFeaturedImageUrl() = featuredImageUrl.isNotEmpty()

    fun getFeaturedImageForDisplay(width: Int, height: Int): String =
        featuredImageForDisplay ?: (
            if (hasFeaturedImageUrl()) PhotonUtils.getPhotonImageUrl(featuredImageUrl, width, height) else ""
        ).also { featuredImageForDisplay = it }

    companion object {
        /**
         * The fields to ask for when requesting simple posts. This leaves out the post content, which makes
         * the call much heavier
         */
        const val SIMPLE_POST_FIELDS = "ID,site_ID,title,excerpt,site_name,featured_image,featured_media,railcar"

        /**
         * Returns null if the JSON is missing the post or site ID
         */
        fun fromJson(json: JSONObject): ReaderSimplePost? {
            val postId: Long
            val siteId: Long
            try {
                postId = json.getLong("ID")
                siteId = json.getLong("site_ID")
            } catch (e: JSONException) {
                AppLog.e(AppLog.T.READER, e)
                return null
            }

            var featuredImageUrl = JSONUtils.getString(json, "featured_image")
            // if there's no featured image, check if featured media has been set to an image
            if (featuredImageUrl.isEmpty()) {
                val jsonMedia = json.optJSONObject("featured_media")
                if (JSONUtils.getString(jsonMedia, "type") == "image") {
                    featuredImageUrl = JSONUtils.getString(jsonMedia, "uri")
                }
            }

            return ReaderSimplePost(
                postId = postId,
                siteId = siteId,
                title = JSONUtils.getStringDecoded(json, "title"),
                excerpt = HtmlUtils.fastStripHtml(JSONUtils.getString(json, "excerpt")).trim(),
                siteName = JSONUtils.getStringDecoded(json, "site_name"),
                featuredImageUrl = featuredImageUrl,
                railcarJson = json.optJSONObject("railcar")?.toString()
            )
        }
    }
}

class ReaderSimplePostList : ArrayList<ReaderSimplePost>() {
    companion object {
        fun fromJsonPosts(jsonPosts: JSONArray) = ReaderSimplePostList().apply {
            for (i in 0 until jsonPosts.length()) {
                jsonPosts.optJSONObject(i)?.let { ReaderSimplePost.fromJson(it) }?.let { add(it) }
            }
        }
    }
}
