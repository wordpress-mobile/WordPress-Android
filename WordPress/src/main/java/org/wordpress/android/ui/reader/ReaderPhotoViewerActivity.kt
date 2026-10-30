package org.wordpress.android.ui.reader

import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.reader.models.ReaderImageList
import org.wordpress.android.ui.reader.utils.ReaderImageScanner
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.views.compose.photoviewer.ReaderPhoto
import org.wordpress.android.ui.reader.views.compose.photoviewer.ReaderPhotoViewerScreen
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.extensions.setContent
import kotlin.math.max

private const val LO_RES_FRACTION = 0.10f

/**
 * Full-screen photo viewer, which pages through the images in a post or gallery but also shows a single image
 */
class ReaderPhotoViewerActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isPrivate = intent.getBooleanExtra(ReaderConstants.ARG_IS_PRIVATE, false)
        var initialImageUrl = intent.getStringExtra(ReaderConstants.ARG_IMAGE_URL)
        val galleryImageUrls = intent.getStringArrayListExtra(ReaderConstants.ARG_GALLERY_IMAGE_URLS)
        val content = intent.getStringExtra(ReaderConstants.ARG_CONTENT)

        val imageList = ReaderImageList(isPrivate)
        if (!galleryImageUrls.isNullOrEmpty()) {
            // gallery-scoped: only show images from the tapped gallery
            galleryImageUrls.forEach { imageList.addImageUrl(it) }
            // The initial URL may be the full-size variant while the gallery contains a resized version (e.g.
            // image.jpg vs image-800x600.jpg). Find the matching gallery URL so the pager starts on the right image
            // without adding a duplicate.
            if (!initialImageUrl.isNullOrEmpty() && !imageList.hasImageUrl(initialImageUrl)) {
                initialImageUrl = findSizeSuffixMatch(initialImageUrl, galleryImageUrls) ?: initialImageUrl
            }
        } else if (!content.isNullOrEmpty()) {
            // parse all images from post HTML - content is empty when viewing a single image
            val isGallery = intent.getBooleanExtra(ReaderConstants.ARG_IS_GALLERY, false)
            val minImageWidth = if (isGallery) ReaderConstants.MIN_GALLERY_IMAGE_WIDTH else 0
            imageList.addAll(ReaderImageScanner(content, isPrivate).getImageList(0, minImageWidth))
        }
        // make sure initial image is in the list
        if (!initialImageUrl.isNullOrEmpty() && !imageList.hasImageUrl(initialImageUrl)) {
            imageList.addImageUrl(0, initialImageUrl)
        }

        // use max of width/height so image is cached the same regardless of orientation
        val windowSize = DisplayUtils.getWindowSize(this)
        val hiResWidth = max(windowSize.width(), windowSize.height())
        val loResWidth = (hiResWidth * LO_RES_FRACTION).toInt()
        val photos = imageList.map { url ->
            // don't use AT media proxy here
            ReaderPhoto(
                loResUrl = ReaderUtils.getResizedImageUrl(
                    url, loResWidth, 0, isPrivate, false, PhotonUtils.Quality.LOW
                ),
                hiResUrl = ReaderUtils.getResizedImageUrl(
                    url, hiResWidth, 0, isPrivate, false, PhotonUtils.Quality.MEDIUM
                )
            )
        }
        val initialPage = imageList.indexOfImageUrl(initialImageUrl).coerceAtLeast(0)

        setContent {
            AppThemeM3(isDarkTheme = true) {
                ReaderPhotoViewerScreen(
                    photos = photos,
                    initialPage = initialPage,
                    onBack = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    // overridePendingTransition is deprecated in SDK 34 in favor of overrideActivityTransition, but the latter requires
    // SDK 34. overridePendingTransition still works on Android 14 so using it should be safe for now.
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}

/**
 * Finds a gallery URL that matches the given URL after stripping WordPress size suffixes (e.g. -800x600) from
 * pathnames.
 */
private fun findSizeSuffixMatch(targetUrl: String, galleryUrls: List<String>): String? {
    val basePath = stripSizeSuffix(UrlUtils.removeQuery(targetUrl))
    return galleryUrls.firstOrNull { stripSizeSuffix(UrlUtils.removeQuery(it)) == basePath }
}

/**
 * Strips WordPress size suffixes (e.g. -800x600) from a URL. Keep in sync with the JS counterpart in
 * [ReaderPostDetailFragment.buildGalleryDetectionJs].
 */
private fun stripSizeSuffix(url: String) = url.replace(Regex("-\\d+x\\d+(\\.[^.]+)$"), "$1")
