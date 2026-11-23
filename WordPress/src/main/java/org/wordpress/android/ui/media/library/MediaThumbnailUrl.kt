package org.wordpress.android.ui.media.library

import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils

/**
 * The best URL for showing a remote image at [widthPx] x [heightPx] in the grid: a Photon URL at that exact size when
 * the site allows it, otherwise the smallest generated size that still looks sharp, then the thumbnail, then the
 * full image without its query.
 */
fun MediaGridItemUiState.bestImageUrl(source: MediaThumbnailSource, widthPx: Int, heightPx: Int): String? {
    if (source.usePhoton && !url.isNullOrEmpty()) {
        return PhotonUtils.getPhotonImageUrl(url, widthPx, heightPx, source.isPrivateAtomic)
    }
    // medium-large and medium are favoured over large because they're more bandwidth-friendly
    return listOf(mediumLargeUrl, mediumUrl, largeUrl, thumbnailUrl).firstOrNull { !it.isNullOrEmpty() }
        ?: url?.let { UrlUtils.removeQuery(it) }
}
