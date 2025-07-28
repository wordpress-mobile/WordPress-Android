package org.wordpress.android.ui.reader.utils

import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.JSONUtils
import org.wordpress.android.util.UrlUtils
import androidx.core.net.toUri

/**
 * hash map of sizes of attachments in a reader post - created from the json "attachments" section
 * of the post endpoints
 */
class ImageSizeMap(private val postContent: String, private val jsonString: String) :
    HashMap<String, ImageSizeMap.ImageSize>() {
    init {
        if (jsonString.isNotEmpty() && jsonString != EMPTY_JSON) {
            parseJson()
        }
    }

    @Suppress("NestedBlockDepth")
    private fun parseJson() {
        try {
            val json = JSONObject(jsonString)
            val keys = json.keys()
            while (keys.hasNext()) {
                json.optJSONObject(keys.next())?.let { jsonAttach ->
                    val mimeType = JSONUtils.getString(jsonAttach, "mime_type")
                    if (mimeType.startsWith("image")) {
                        parseJsonImage(jsonAttach)
                    }
                }
            }
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.READER, e)
        }
    }

    private fun parseJsonImage(jsonImage: JSONObject) {
        val imageUrl = JSONUtils.getString(
            jsonImage,
            "URL"
        )
        val key = getKeyFromImageUrl(imageUrl)

        // make sure this image actually appears in the post content - it's possible for
        // an image to be in the attachments but not in the post itself
        val path = key.toUri().path
        if (path != null && postContent.contains(path)) {
            var width = jsonImage.optInt("width")
            var height = jsonImage.optInt("height")

            // check if data-orig-size is present and use it
            val originalSize = jsonImage.optString("data-orig-size")
            val sizes = originalSize.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (sizes.size == 2) {
                width = sizes[0].toInt()
                height = sizes[1].toInt()
            }

            this[key] =
                ImageSize(
                    width,
                    height
                )
        }
    }

    fun getImageSize(imageUrl: String): ImageSize? {
        return get(getKeyFromImageUrl(imageUrl))
    }

    private fun getKeyFromImageUrl(imageUrl: String) = UrlUtils.normalizeUrl(UrlUtils.removeQuery(imageUrl))

    class ImageSize(val width: Int, val height: Int)

    companion object {
        private const val EMPTY_JSON = "{}"
    }
}
