package org.wordpress.android.ui.reader.utils

import org.jsoup.Jsoup
import org.wordpress.android.util.AppLog
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
            val tag = element.tagName()
            val classes = element.className()
            if (element.root() !== document) {
                AppLog.d(
                    AppLog.T.READER,
                    "GalleryScanner: skipping nested <$tag class='$classes'>"
                )
                continue
            }
            val imageUrls = element.select("img")
                .mapNotNull { img ->
                    img.attr("src").takeIf { it.startsWith("http") }
                }
            AppLog.d(
                AppLog.T.READER,
                "GalleryScanner: <$tag class='$classes'> " +
                    "has ${imageUrls.size} images"
            )
            if (imageUrls.isNotEmpty()) {
                galleries.add(imageUrls)
                imageUrls.forEach { url ->
                    AppLog.d(AppLog.T.READER, "  -> $url")
                }
            }
            element.remove()
        }

        AppLog.d(
            AppLog.T.READER,
            "GalleryScanner: parsed ${galleries.size} galleries"
        )
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
        AppLog.d(
            AppLog.T.READER,
            "GalleryScanner: findGalleryContaining tapped=$imageUrl"
        )
        AppLog.d(
            AppLog.T.READER,
            "GalleryScanner: normalized tapped=$normalized"
        )
        for ((i, gallery) in galleries.withIndex()) {
            val normalizedGallery = gallery.map { normalizeImageUrl(it) }
            AppLog.d(
                AppLog.T.READER,
                "GalleryScanner: gallery[$i] normalized=$normalizedGallery"
            )
            val matchIndex = normalizedGallery.indexOf(normalized)
            if (matchIndex >= 0) {
                AppLog.d(
                    AppLog.T.READER,
                    "GalleryScanner: matched gallery[$i] at index $matchIndex"
                )
                return gallery
            }
        }
        AppLog.d(AppLog.T.READER, "GalleryScanner: no gallery match")
        return null
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
