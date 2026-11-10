package org.wordpress.android.ui.reader.utils

import androidx.core.net.toUri

object ReaderVideoUtils {
    private val YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com"
    )

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
        return when {
            // youtu.be urls have the videoId as the path - ex: http://youtu.be/pEnXclbO9jg
            uri.host == "youtu.be" -> path.replace("/", "")
            uri.host !in YOUTUBE_HOSTS -> null
            // ex: https://m.youtube.com/watch?v=pEnXclbO9jg
            path.contains("watch") -> uri.getQueryParameter("v")
            // ex: https://www.youtube.com/embed/fw3w68YrKwc?version=3, https://www.youtube.com/shorts/fw3w68YrKwc
            path.contains("/embed/") || path.contains("/shorts/") -> uri.lastPathSegment
            // older mobile urls include the video id in the fragment, ex:
            // http://m.youtube.com/?dc=organic&source=mog#/watch?v=t77Vlme_pf8
            else -> uri.fragment?.substringAfterLast("v=", "")
        }.orEmpty()
    }
}
