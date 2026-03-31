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

    /**
     * Returns the gallery URL that matches the given image URL after
     * normalization, or the original imageUrl if no match is found.
     */
    fun findMatchingUrl(gallery: List<String>, imageUrl: String): String {
        val normalized = normalizeImageUrl(imageUrl)
        return gallery.firstOrNull {
            normalizeImageUrl(it) == normalized
        } ?: imageUrl
    }

    private val PHOTON_HOST_REGEX = Regex("^https?://i\\d\\.wp\\.com/(.+)")
    private val WP_SIZE_SUFFIX_REGEX = Regex("-\\d+x\\d+(?=\\.\\w+$)")

    private fun normalizeImageUrl(url: String): String {
        val stripped = stripWpSizeSuffix(
            UrlUtils.removeQuery(stripPhotonHost(url))
        )
        return UrlUtils.normalizeUrl(stripped)
    }

    /**
     * Strips the WordPress image size suffix (e.g., "-819x1024") that
     * appears before the file extension in resized image URLs.
     */
    private fun stripWpSizeSuffix(url: String): String {
        return url.replace(WP_SIZE_SUFFIX_REGEX, "")
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
