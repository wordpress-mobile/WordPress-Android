package org.wordpress.android.ui.reader.utils

import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

class FeaturedImageUtils
@Inject constructor() {
    fun showFeaturedImage(
        featuredImage: String,
        postText: String
    ): Boolean {
        return try {
            val featuredImageUrl = URL(featuredImage)
            val endIndex = featuredImageUrl.path.lastIndexOf("-", featuredImageUrl.path.lastIndexOf("/"))
            val featuredImageFile = featuredImageUrl.file
            val featuredImagePath = if (endIndex > 0 && !featuredImageFile.startsWith("-")) {
                featuredImageUrl.path.substring(0, endIndex)
            } else {
                featuredImageUrl.path
            }
            when {
                postText.contains(featuredImage) -> false
                postText.contains(featuredImagePath) && featuredImagePath != featuredImageFile -> false
                else -> true
            }
        } catch (e: MalformedURLException) {
            false
        }
    }
}
