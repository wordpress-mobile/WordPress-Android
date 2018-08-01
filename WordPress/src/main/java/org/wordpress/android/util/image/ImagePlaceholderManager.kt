package org.wordpress.android.util.image

import org.wordpress.android.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePlaceholderManager @Inject constructor() {
    fun getErrorResource(imgType: ImageType): Int? {
        return when (imgType) {
            ImageType.IMAGE -> null // don't display any error drawable
            ImageType.PHOTO -> R.color.grey_lighten_30
            ImageType.VIDEO -> R.color.grey_lighten_30
            ImageType.AVATAR -> R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp
            ImageType.BLAVATAR -> R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp
            ImageType.PLAN -> R.drawable.ic_reader_blue_wordpress_18dp
            ImageType.THEME -> R.color.grey_lighten_30
            ImageType.FULLSCREEN_PHOTO -> null // manually handled in the view
            ImageType.UNKNOWN -> R.drawable.ic_notice_grey_500_48dp
            ImageType.PLUGIN -> R.drawable.plugin_placeholder
        }
    }

    fun getPlaceholderResource(imgType: ImageType): Int? {
        return when (imgType) {
            ImageType.IMAGE -> null // don't display any placeholder
            ImageType.PHOTO -> R.color.grey_light
            ImageType.VIDEO -> R.color.grey_light
            ImageType.AVATAR -> R.drawable.shape_oval_grey_light
            ImageType.BLAVATAR -> R.color.grey_light
            ImageType.PLAN -> R.drawable.ic_reader_blue_wordpress_18dp
            ImageType.THEME -> R.drawable.theme_loading
            ImageType.FULLSCREEN_PHOTO -> null // manually handled in the view
            ImageType.UNKNOWN -> R.drawable.legacy_dashicon_format_image_big_grey
            ImageType.PLUGIN -> R.drawable.plugin_placeholder
        }
    }
}
