package org.wordpress.android.ui.reader.utils

import android.net.Uri
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

object ReaderVideoUtils {
    /*
     * returns true if the passed url is a link to a YouTube video
     */
    fun isYouTubeVideoLink(link: String?) = getYouTubeVideoId(link).isNotEmpty()

    /*
     * extract the video id from the passed YouTube url
     */
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
}
