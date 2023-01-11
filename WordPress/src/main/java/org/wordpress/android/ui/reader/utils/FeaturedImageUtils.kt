package org.wordpress.android.ui.reader.utils

import androidx.annotation.NonNull
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.PhotonUtilsWrapper
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

class FeaturedImageUtils
@Inject constructor(
    private val photonUtilsWrapper: PhotonUtilsWrapper
) {
    fun showFeaturedImage(
        featuredImage: String,
        postText: String
    ): Boolean {
        return try {
            val featuredImageUrl = URL(featuredImage)
            val endIndex = getLastIndexOfDashInUrl(featuredImageUrl)
            val featuredImageFile = featuredImageUrl.file
            val featuredImagePath = if (endIndex > 0 && !featuredImageFile.startsWith("-")) {
                featuredImageUrl.path.substring(0, endIndex + 1)
            } else {
                featuredImageUrl.path
            }
            when {
                postText.contains(featuredImage) -> false
                postText.contains(featuredImagePath) && featuredImagePath != featuredImageFile -> false
                else -> true
            }
        } catch (e: MalformedURLException) {
            AppLog.e(T.READER, "Featured image URL is malformed so it's not shown")
            false
        }
    }

    private fun getLastIndexOfDashInUrl(url: URL) = url.path.split("/").last()
        .lastIndexOf("-").takeIf { it > 0 }
        ?.let { it + url.path.lastIndexOf("/") } ?: -1

    /*
     * returns true if the post has a featured image and the featured image is not found in the post body
     */
    fun shouldAddFeaturedImage(@NonNull readerPost: ReaderPost): Boolean {
        return (readerPost.hasFeaturedImage() &&
                !photonUtilsWrapper.isMshotsUrl(readerPost.featuredImage) &&
                showFeaturedImage(readerPost.featuredImage, readerPost.text))
    }
}
