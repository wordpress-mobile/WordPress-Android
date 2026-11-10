package org.wordpress.android.ui.reader.models

import org.wordpress.android.util.UrlUtils

/*
 * used by ReaderImageScanner to compile a list of images in a specific post
 */
class ReaderImageList(val isPrivate: Boolean) : ArrayList<String>() {
    fun indexOfImageUrl(imageUrl: String?): Int {
        if (imageUrl == null || isEmpty()) {
            return -1
        }
        return indexOf(fixImageUrl(imageUrl))
    }

    fun hasImageUrl(imageUrl: String?) = indexOfImageUrl(imageUrl) > -1

    fun addImageUrl(imageUrl: String?) {
        if (imageUrl != null && imageUrl.startsWith("http")) {
            add(fixImageUrl(imageUrl))
        }
    }

    fun addImageUrl(index: Int, imageUrl: String?) {
        if (imageUrl != null && imageUrl.startsWith("http")) {
            add(index, fixImageUrl(imageUrl))
        }
    }

    // image urls are always added normalized and without query params for easier matching, and
    // to ensure there are no hard-coded sizes in the query
    private fun fixImageUrl(imageUrl: String): String = UrlUtils.normalizeUrl(UrlUtils.removeQuery(imageUrl))
}
