package org.wordpress.android.ui.reader.utils

import androidx.core.net.toUri

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
        // only hierarchical uris have a host, so getQueryParameter() below can't throw
        val uri = link.toUri()
        val path = uri.path.orEmpty()
        return when (uri.host) {
            // if the path contains "watch" then the id is in the "v=" query param; if it contains
            // "embed" then the id is the last path segment,
            // ex: https://www.youtube.com/embed/fw3w68YrKwc?version=3&#038;rel=1&#038;
            "youtube.com", "www.youtube.com" -> when {
                path.contains("watch") -> uri.getQueryParameter("v")
                path.contains("/embed/") -> uri.lastPathSegment
                else -> null
            }
            // youtu.be urls have the videoId as the path - ex: http://youtu.be/pEnXclbO9jg
            "youtu.be" -> path.replace("/", "")
            // YouTube mobile urls include video id in fragment, ex:
            // http://m.youtube.com/?dc=organic&source=mog#/watch?v=t77Vlme_pf8
            "m.youtube.com" -> uri.fragment?.substringAfterLast("v=", "")
            else -> null
        }.orEmpty()
    }
}
