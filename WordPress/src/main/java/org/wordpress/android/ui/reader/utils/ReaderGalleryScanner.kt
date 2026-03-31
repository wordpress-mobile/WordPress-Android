package org.wordpress.android.ui.reader.utils

import org.jsoup.Jsoup
import org.wordpress.android.util.UrlUtils

/**
 * Scans post HTML for gallery blocks and extracts image URLs from each gallery.
 * Used to show only gallery images (not all post images) in the photo viewer
 * when a user taps an image that belongs to a gallery.
 */
object ReaderGalleryScanner {
    private val GALLERY_SELECTORS = listOf(
        "figure.wp-block-gallery",
        "div.wp-block-gallery",
        "figure.wp-block-jetpack-tiled-gallery",
        "div.wp-block-jetpack-tiled-gallery",
        "div.tiled-gallery",
        "div.gallery",
    )

    /**
     * Parses galleries from the given post HTML content.
     * Returns a list of galleries, where each gallery is a list of image URLs.
     */
    fun parseGalleries(html: String?): List<List<String>> {
        if (html.isNullOrBlank()) return emptyList()

        val document = Jsoup.parse(html)
        val galleries = mutableListOf<List<String>>()

        for (selector in GALLERY_SELECTORS) {
            val elements = document.select(selector)
            for (element in elements) {
                val imageUrls = element.select("img")
                    .mapNotNull { img ->
                        img.attr("src").takeIf { it.startsWith("http") }
                    }
                if (imageUrls.isNotEmpty()) {
                    galleries.add(imageUrls)
                }
                // Remove from DOM to avoid nested matches
                element.remove()
            }
        }

        return galleries
    }

    /**
     * Finds the gallery containing the given image URL.
     * Returns the gallery's image URLs, or null if no gallery contains it.
     */
    fun findGalleryContaining(
        galleries: List<List<String>>,
        imageUrl: String
    ): List<String>? {
        val normalizedTapped = normalizeImageUrl(imageUrl)
        for (gallery in galleries) {
            if (gallery.any { normalizeImageUrl(it) == normalizedTapped }) {
                return gallery
            }
        }
        return null
    }

    private fun normalizeImageUrl(url: String): String {
        return UrlUtils.normalizeUrl(UrlUtils.removeQuery(url))
    }
}
