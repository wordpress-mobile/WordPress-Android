package org.wordpress.android.util.image

import org.wordpress.android.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePlaceholderManager @Inject constructor() {
    fun getErrorResource(imgType: ImageType): Int? {
        return when (imgType) {
            ImageType.AVATAR -> R.drawable.bg_rectangle_placeholder_user_32dp
            ImageType.AVATAR_WITH_BACKGROUND -> R.drawable.bg_oval_placeholder_user_32dp
            ImageType.AVATAR_WITHOUT_BACKGROUND -> R.drawable.ic_user_circle_grey_24dp
            ImageType.BLAVATAR -> R.drawable.bg_rectangle_placeholder_globe_32dp
            ImageType.BLAVATAR_CIRCULAR -> R.drawable.bg_oval_placeholder
            ImageType.IMAGE -> null // don't display any error drawable
            ImageType.PHOTO -> R.color.placeholder
            ImageType.READER -> R.drawable.bg_rectangle_placholder_reader_radius_4dp
            ImageType.PLAN -> R.drawable.bg_oval_placholder_plans_32dp
            ImageType.PLUGIN -> R.drawable.plugin_placeholder
            ImageType.THEME -> R.color.placeholder
            ImageType.UNKNOWN -> R.drawable.ic_notice_white_24dp
            ImageType.USER -> R.drawable.ic_user_white_24dp
            ImageType.VIDEO -> R.color.placeholder
            ImageType.ICON -> R.drawable.bg_rectangle_placeholder_radius_2dp
            ImageType.NO_PLACEHOLDER -> null
        }
    }

    fun getPlaceholderResource(imgType: ImageType): Int? {
        return when (imgType) {
            ImageType.AVATAR -> R.drawable.bg_oval_placeholder
            ImageType.AVATAR_WITH_BACKGROUND -> R.drawable.bg_oval_placeholder_user_32dp
            ImageType.AVATAR_WITHOUT_BACKGROUND -> R.drawable.ic_user_circle_grey_24dp
            ImageType.BLAVATAR -> R.color.placeholder
            ImageType.BLAVATAR_CIRCULAR -> R.drawable.bg_oval_placeholder
            ImageType.IMAGE -> null // don't display any placeholder
            ImageType.PHOTO -> R.color.placeholder
            ImageType.READER -> R.drawable.bg_rectangle_placholder_reader_radius_4dp
            ImageType.PLAN -> R.drawable.bg_oval_placholder_plans_32dp
            ImageType.PLUGIN -> R.drawable.plugin_placeholder
            ImageType.THEME -> R.drawable.bg_rectangle_placeholder_themes_100dp
            ImageType.UNKNOWN -> R.drawable.legacy_dashicon_format_image_big_grey
            ImageType.USER -> R.drawable.ic_user_white_24dp
            ImageType.VIDEO -> R.color.placeholder
            ImageType.ICON -> R.drawable.bg_rectangle_placeholder_radius_2dp
            ImageType.NO_PLACEHOLDER -> null
        }
    }
}
