package org.wordpress.android.ui.reader.utils

import org.jsoup.Jsoup
import org.wordpress.android.util.UrlUtils

/**
 * Scans post HTML for gallery blocks and extracts image URLs from each gallery.
 * Used to show only gallery images (not all post images) in the photo viewer
 * when a user taps an image that belongs to a gallery.
 */
object ReaderGalleryScanner {
    private const val GALLERY_SELECTOR =
        ".wp-block-gallery, .wp-block-jetpack-tiled-gallery, " +
            ".tiled-gallery, div.gallery"

    /**
     * Parses galleries from the given post HTML content.
     * Returns a list of galleries, where each gallery is a list of image URLs.
     */
    fun parseGalleries(html: String?): List<List<String>> {
        if (html.isNullOrBlank()) return emptyList()

        val document = Jsoup.parse(html)
        val galleries = mutableListOf<List<String>>()

        for (element in document.select(GALLERY_SELECTOR)) {
            // Skip elements nested inside an already-removed gallery
            if (element.root() !== document) continue
            val imageUrls = element.select("img")
                .mapNotNull { img ->
                    img.attr("src").takeIf { it.startsWith("http") }
                }
            if (imageUrls.isNotEmpty()) {
                galleries.add(imageUrls)
            }
            element.remove()
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
        val normalized = normalizeImageUrl(imageUrl)
        return galleries.firstOrNull { gallery ->
            gallery.any { normalizeImageUrl(it) == normalized }
        }
    }

    private val PHOTON_HOST_REGEX = Regex("^https?://i\\d\\.wp\\.com/(.+)")

    private fun normalizeImageUrl(url: String): String {
        return UrlUtils.normalizeUrl(UrlUtils.removeQuery(stripPhotonHost(url)))
    }

    /**
     * Strips the Photon CDN wrapper from a URL so that the original host
     * and path can be compared directly.
     * Example: https://i0.wp.com/example.com/photo.jpg?w=600 → https://example.com/photo.jpg?w=600
     */
    private fun stripPhotonHost(url: String): String {
        val match = PHOTON_HOST_REGEX.find(url) ?: return url
        return "https://${match.groupValues[1]}"
    }
}
