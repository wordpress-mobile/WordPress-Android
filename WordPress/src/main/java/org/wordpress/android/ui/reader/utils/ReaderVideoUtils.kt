package org.wordpress.android.ui.reader.utils

import android.net.Uri
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.ReaderThumbnailTable
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.JSONUtils
import org.wordpress.android.util.MediaUtils
import kotlin.coroutines.resume

object ReaderVideoUtils {
    /*
     * determine whether we can show a thumbnail image for the passed video - currently
     * we support YouTube, Vimeo & standard images
     */
    fun canShowVideoThumbnail(videoUrl: String?) =
        isVimeoLink(videoUrl) || isYouTubeVideoLink(videoUrl) || MediaUtils.isValidImage(videoUrl)

    /*
     * returns the thumbnail url for the passed video if it's known without a network request: YouTube
     * thumbnails are derived from the video url, Vimeo ones come from the cache, and a video url that's
     * an image is its own thumbnail. Returns null otherwise, in which case a Vimeo video's thumbnail can
     * be fetched with fetchVimeoThumbnailUrl().
     */
    fun getThumbnailUrlWithoutRequest(videoUrl: String): String? {
        val thumbnailUrl = if (isYouTubeVideoLink(videoUrl)) {
            getYouTubeThumbnailUrl(videoUrl)
        } else {
            // older versions cached a Vimeo video's own url as its thumbnail, so ignore that
            ReaderThumbnailTable.getThumbnailUrl(videoUrl)?.takeIf { it != videoUrl }
        }
        return when {
            !thumbnailUrl.isNullOrEmpty() -> thumbnailUrl
            MediaUtils.isValidImage(videoUrl) -> videoUrl
            else -> null
        }
    }

    /*
     * returns the url to get the full-size (480x360) thumbnail url for the passed video
     */
    private fun getYouTubeThumbnailUrl(videoUrl: String): String {
        val videoId = getYouTubeVideoId(videoUrl)
        // note that this *must* use https rather than http - ex: https://img.youtube.com/vi/ClbE019cLNI/0.jpg
        return if (videoId.isEmpty()) "" else "https://img.youtube.com/vi/$videoId/0.jpg"
    }

    /*
     * returns true if the passed url is a link to a YouTube video
     */
    fun isYouTubeVideoLink(link: String?) = getYouTubeVideoId(link).isNotEmpty()

    /*
     * extract the video id from the passed YouTube url
     */
    @Suppress("ReturnCount")
    private fun getYouTubeVideoId(link: String?): String {
        if (link == null) {
            return ""
        }
        val uri = Uri.parse(link)
        return try {
            when (uri.host) {
                // if link contains "watch" in the path, then the id is in the "v=" query param; if it
                // contains "embed" then the id is the last path segment,
                // ex: https://www.youtube.com/embed/fw3w68YrKwc?version=3&#038;rel=1&#038;
                "youtube.com", "www.youtube.com" -> when {
                    link.contains("watch") -> uri.getQueryParameter("v")
                    link.contains("/embed/") -> uri.lastPathSegment
                    else -> null
                }
                // youtu.be urls have the videoId as the path - ex: http://youtu.be/pEnXclbO9jg
                "youtu.be" -> uri.path?.replace("/", "")
                // YouTube mobile urls include video id in fragment, ex:
                // http://m.youtube.com/?dc=organic&source=mog#/watch?v=t77Vlme_pf8
                "m.youtube.com" -> uri.fragment?.let { fragment ->
                    val index = fragment.lastIndexOf("v=")
                    if (index != -1) fragment.substring(index + 2) else null
                }
                else -> null
            }.orEmpty()
        } catch (e: UnsupportedOperationException) {
            AppLog.e(T.READER, e)
            ""
        }
    }

    /*
     * returns true if the passed url is a link to a Vimeo video
     */
    fun isVimeoLink(link: String?) = getVimeoVideoId(link).isNotEmpty()

    /*
     * extract the video id from the passed Vimeo url
     * ex: http://player.vimeo.com/video/72386905 -> 72386905
     */
    private fun getVimeoVideoId(link: String?): String {
        if (link == null || !link.contains("player.vimeo.com")) {
            return ""
        }
        return Uri.parse(link).lastPathSegment.orEmpty()
    }

    /*
     * unlike YouTube thumbnails, Vimeo thumbnails require a network request - returns null if the
     * video has no thumbnail or the request failed
     */
    suspend fun fetchVimeoThumbnailUrl(videoUrl: String): String? {
        val id = getVimeoVideoId(videoUrl)
        if (id.isEmpty()) {
            return null
        }
        return suspendCancellableCoroutine { continuation ->
            val listener = Response.Listener<JSONArray> { response ->
                val thumbnailUrl = response?.optJSONObject(0)?.let { JSONUtils.getString(it, "thumbnail_large") }
                if (continuation.isActive) continuation.resume(thumbnailUrl?.takeIf { it.isNotEmpty() })
            }
            val errorListener = Response.ErrorListener { volleyError ->
                AppLog.e(T.READER, volleyError)
                if (continuation.isActive) continuation.resume(null)
            }
            val request = JsonArrayRequest("https://vimeo.com/api/v2/video/$id.json", listener, errorListener)
            continuation.invokeOnCancellation { request.cancel() }
            WordPress.requestQueue?.add(request) ?: continuation.resume(null)
        }
    }
}
