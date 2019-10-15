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
            val featuredImagePath = featuredImageUrl.path
            val featuredImageFile = featuredImageUrl.file
            when {
                postText.contains(featuredImage) -> false
                postText.contains(featuredImagePath) && featuredImagePath != featuredImageFile -> false
                else -> true
            }
        } catch (e: MalformedURLException) {
            false
        }
    }

    companion object {
        fun getInstance() = FeaturedImageUtils()
    }
}
