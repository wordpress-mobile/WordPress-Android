package org.wordpress.android.util.image

import org.wordpress.android.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePlaceholderManager @Inject constructor() {
    fun getErrorResource(imgType: ImageType): Int? {
        return when (imgType) {
            ImageType.PHOTO -> R.color.grey_lighten_30
            ImageType.VIDEO -> R.color.grey_lighten_30
            ImageType.AVATAR -> R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp
            ImageType.BLAVATAR -> R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp
            ImageType.THEME -> R.color.grey_lighten_30
            ImageType.UNKNOWN_DIMENSIONS -> R.drawable.ic_notice_grey_500_48dp
        }
    }

    fun getPlaceholderResource(imgType: ImageType): Int? {
        return when (imgType) {
            ImageType.PHOTO -> R.color.grey_light
            ImageType.VIDEO -> R.color.grey_light
            ImageType.AVATAR -> R.drawable.shape_oval_grey_light
            ImageType.BLAVATAR -> R.color.grey_light
            ImageType.THEME -> R.drawable.theme_loading
            ImageType.UNKNOWN_DIMENSIONS -> R.drawable.legacy_dashicon_format_image_big_grey
        }
    }
}
