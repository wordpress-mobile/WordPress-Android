package org.wordpress.android.ui.reader.utils

import android.net.Uri
import android.text.TextUtils
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.JSONUtils
import org.wordpress.android.util.UrlUtils

/**
 * hash map of sizes of attachments in a reader post - created from the json "attachments" section
 * of the post endpoints
 */
class ImageSizeMap(postContent: String, jsonString: String) :
    HashMap<String?, ImageSizeMap.ImageSize?>() {
    init {
        if (TextUtils.isEmpty(jsonString) || jsonString == EMPTY_JSON) {
            return
        }

        try {
            val json = JSONObject(jsonString)
            val it = json.keys()
            if (!it.hasNext()) {
                return
            }

            while (it.hasNext()) {
                val jsonAttach = json.optJSONObject(it.next())
                if (jsonAttach != null && JSONUtils.getString(jsonAttach, "mime_type")
                        .startsWith("image")
                ) {
                    val normUrl =
                        UrlUtils.normalizeUrl(
                            UrlUtils.removeQuery(
                                JSONUtils.getString(
                                    jsonAttach,
                                    "URL"
                                )
                            )
                        )

                    // make sure this image actually appears in the post content - it's possible for
                    // an image to be in the attachments but not in the post itself
                    val path = Uri.parse(normUrl).path
                    if (postContent.contains(path!!)) {
                        var width = jsonAttach.optInt("width")
                        var height = jsonAttach.optInt("height")

                        // chech if data-orig-size is present and use it
                        val originalSize = jsonAttach.optString("data-orig-size", null)
                        if (originalSize != null) {
                            val sizes =
                                originalSize.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            if (sizes != null && sizes.size == 2) {
                                width = sizes[0].toInt()
                                height = sizes[1].toInt()
                            }
                        }

                        this[normUrl] =
                            ImageSize(
                                width,
                                height
                            )
                    }
                }
            }
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.READER, e)
        }
    }

    fun getImageSize(imageUrl: String?): ImageSize? {
        return if (imageUrl == null) {
            null
        } else {
            super.get(UrlUtils.normalizeUrl(UrlUtils.removeQuery(imageUrl)))
        }
    }

    fun getLargestImageUrl(minImageWidth: Int): String? {
        var currentImageUrl: String? = null
        var currentMaxWidth = minImageWidth
        for ((key, value) in this) {
            if (value!!.width > currentMaxWidth) {
                currentImageUrl = key
                currentMaxWidth = value.width
            }
        }

        return currentImageUrl
    }

    class ImageSize(val width: Int, val height: Int)
    companion object {
        private const val EMPTY_JSON = "{}"
    }
}
