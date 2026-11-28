package org.wordpress.android.ui.reader.views.compose.article

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How the post's images are loaded and opened: [url] sizes an image's URL for a width in pixels (Photon, and the
 * private-post rules), and [onClick] opens it in the photo viewer
 */
class ReaderArticleImages(
    val url: (url: String, widthPx: Int) -> String = { url, _ -> url },
    val onClick: (url: String) -> Unit = {},
)

internal val LocalReaderArticleImages = staticCompositionLocalOf { ReaderArticleImages() }
