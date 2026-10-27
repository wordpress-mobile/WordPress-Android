package org.wordpress.android.ui.rs.data

import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.SiteAccessibilityInfo
import kotlin.math.abs

/** A featured image sized for each of the shapes a list row can draw it at. */
data class FeaturedImageUrls(
    val thumbnail: String,
    val hero: String,
)

/** One of the renders WordPress generated for an image at upload time. */
internal data class ScaledSize(
    val width: Int,
    val height: Int,
    val url: String,
)

internal data class MediaImage(
    val sourceUrl: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    /** Renders from `media_details.sizes`, ascending by width. Empty for non-images. */
    val sizes: List<ScaledSize>,
)

/**
 * A URL for this image at [widthPx] by [heightPx], or by nothing if [heightPx] is 0. Photon
 * resizes the original; everywhere else picks a pre-generated render, never one narrower, and
 * looks no wider than [maxRenderWidthPx].
 */
internal fun MediaImage.toDisplayUrl(
    accessibilityInfo: SiteAccessibilityInfo,
    isWpComRest: Boolean,
    widthPx: Int,
    heightPx: Int,
    maxRenderWidthPx: Int = Int.MAX_VALUE,
): String {
    // Float division: 1024/390 as ints is 2, which would admit crops of the wrong shape.
    val displayAspect = if (heightPx > 0) widthPx.toFloat() / heightPx else null
    val url = if (accessibilityInfo.isPhotonCapable) {
        sourceUrl
    } else {
        renderAtLeast(widthPx.coerceAtMost(maxRenderWidthPx), displayAspect) ?: sourceUrl
    }
    // Rewriting a self-hosted URL would drop any signed or CDN query string it carries.
    if (!isWpComRest) return url
    // Photon crops with `resize=W,H`. Elsewhere the pair becomes `?w=&h=`, which
    // files.wordpress.com treats as a bounding box, so non-Photon sites get width only.
    val photonHeight = if (accessibilityInfo.isPhotonCapable) heightPx else 0
    return ReaderUtils.getResizedImageUrl(url, widthPx, photonHeight, accessibilityInfo)
}

/**
 * The smallest render at least [targetWidth] wide. Core hard-crops `thumbnail`, and cropping a crop
 * cuts twice - so a render must match the original's proportions, or the [displayAspect] it's drawn
 * at.
 */
private fun MediaImage.renderAtLeast(targetWidth: Int, displayAspect: Float?): String? {
    if (sourceWidth <= 0 || sourceHeight <= 0) return null
    val sourceRatio = sourceWidth.toFloat() / sourceHeight
    return sizes.firstOrNull {
        it.usableWidthFor(displayAspect) >= targetWidth && (
            it.matchesRatio(sourceRatio) ||
                (displayAspect != null && it.matchesRatio(displayAspect))
            )
    }?.url
}

/** Width surviving a crop to [displayAspect]: a 300x169 render gives a square slot only 169. */
private fun ScaledSize.usableWidthFor(displayAspect: Float?): Int {
    if (displayAspect == null || height <= 0) return width
    return if (width.toFloat() / height > displayAspect) {
        (height * displayAspect).toInt()
    } else {
        width
    }
}

private fun ScaledSize.matchesRatio(ratio: Float): Boolean =
    height > 0 && abs(width.toFloat() / height - ratio) <= ratio * ASPECT_TOLERANCE

/** Ratio drift allowed before a render counts as cropped rather than scaled. */
private const val ASPECT_TOLERANCE = 0.05f

/**
 * Widest render a list hero looks for. `large` is 1024 and every stock install has one; aiming past
 * it risks matching nothing and pulling the original, at the cost of sharper renders where they
 * exist.
 */
internal const val MAX_HERO_RENDER_WIDTH_PX = 1024
