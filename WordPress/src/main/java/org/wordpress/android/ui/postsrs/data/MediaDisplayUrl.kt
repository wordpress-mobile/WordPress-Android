package org.wordpress.android.ui.postsrs.data

import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.SiteAccessibilityInfo
import kotlin.math.abs

/**
 * What a media lookup found: [resolved] is what came back, [absentIds] the ids the server answered
 * without. Ids in neither were never successfully asked for, so they are retried rather than
 * written off.
 */
data class MediaLookup<T>(
    val resolved: Map<Long, T> = emptyMap(),
    val absentIds: Set<Long> = emptySet(),
)

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
 * resizes the original; everywhere else picks a pre-generated render, never one narrower.
 */
internal fun MediaImage.toDisplayUrl(
    accessibilityInfo: SiteAccessibilityInfo,
    isWpComRest: Boolean,
    widthPx: Int,
    heightPx: Int,
): String {
    // Float division: 1024/390 as ints is 2, which would admit crops of the wrong shape.
    val displayAspect = if (heightPx > 0) widthPx.toFloat() / heightPx else null
    val url = if (accessibilityInfo.isPhotonCapable) {
        sourceUrl
    } else {
        // A self-hosted site only has the renders it registered at upload, and asking wider than
        // all of them falls back to the full-size original - so the search is capped even when the
        // slot is wider. Photon needs no cap; it generates whatever size is asked for.
        renderAtLeast(widthPx.coerceAtMost(MAX_RENDER_WIDTH_PX), displayAspect) ?: sourceUrl
    }
    // Rewriting a self-hosted URL would drop any signed or CDN query string it carries.
    if (!isWpComRest) return url
    // Photon reads `resize=W,H` as the same centre crop the row does. Without it the pair becomes
    // `?w=&h=`, which files.wordpress.com bounds by rather than crops to, returning something
    // narrower than the slot - so non-Photon sites get width only.
    val photonHeight = if (accessibilityInfo.isPhotonCapable) heightPx else 0
    return ReaderUtils.getResizedImageUrl(url, widthPx, photonHeight, accessibilityInfo)
}

/**
 * The smallest render at least [targetWidth] wide. Themes register hard-cropped sizes (core crops
 * `thumbnail`), and cropping one again cuts the image down twice - so a render qualifies only if
 * it matches the original's proportions, or the [displayAspect] it will be drawn at.
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

/**
 * How much of this render's width survives a crop to [displayAspect]: a 300x169 render only has
 * 169px to give a square slot.
 */
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
 * Widest render to look for. `large` is 1024 and every stock install has one; the bigger default
 * sizes only exist when the upload was big enough, so aiming past `large` risks matching nothing
 * and pulling the original instead. Costs a render sharper than `large` on the sites that do have
 * one.
 */
private const val MAX_RENDER_WIDTH_PX = 1024
