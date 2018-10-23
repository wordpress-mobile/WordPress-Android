package org.wordpress.android.util.image

import org.wordpress.android.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePlaceholderManager @Inject constructor() {
    fun getErrorResource(imgType: ImageType): Int? {
        return when (imgType) {
            ImageType.AVATAR -> R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp
            ImageType.AVATAR_WITH_BACKGROUND -> R.drawable.bg_oval_grey_user_32dp
            ImageType.AVATAR_WITHOUT_BACKGROUND -> R.drawable.ic_user_circle_grey_24dp
            ImageType.BLAVATAR -> R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp
            ImageType.IMAGE -> null // don't display any error drawable
            ImageType.PHOTO -> R.color.grey_lighten_30
            ImageType.PLAN -> R.drawable.ic_reader_blue_wordpress_18dp
            ImageType.PLUGIN -> R.drawable.plugin_placeholder
            ImageType.THEME -> R.color.grey_lighten_30
            ImageType.UNKNOWN -> R.drawable.ic_notice_grey_500_48dp
            ImageType.VIDEO -> R.color.grey_lighten_30
        }
    }

    fun getPlaceholderResource(imgType: ImageType): Int? {
        return when (imgType) {
            ImageType.AVATAR -> R.drawable.shape_oval_grey_light
            ImageType.AVATAR_WITH_BACKGROUND -> R.drawable.bg_oval_grey_user_32dp
            ImageType.AVATAR_WITHOUT_BACKGROUND -> R.drawable.ic_user_circle_grey_24dp
            ImageType.BLAVATAR -> R.color.grey_light
            ImageType.IMAGE -> null // don't display any placeholder
            ImageType.PHOTO -> R.color.grey_light
            ImageType.PLAN -> R.drawable.ic_reader_blue_wordpress_18dp
            ImageType.PLUGIN -> R.drawable.plugin_placeholder
            ImageType.THEME -> R.drawable.theme_loading
            ImageType.UNKNOWN -> R.drawable.legacy_dashicon_format_image_big_grey
            ImageType.VIDEO -> R.color.grey_light
        }
    }
}
