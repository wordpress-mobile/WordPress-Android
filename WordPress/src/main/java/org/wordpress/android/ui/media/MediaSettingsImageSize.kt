package org.wordpress.android.ui.media

import android.content.Context
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import org.wordpress.android.R

/** The size options for an image in a post, as shown by [MediaSettingsFragment] */
enum class MediaSettingsImageSize(
    @StringRes val label: Int,
    @StringRes val cssClass: Int,
    @IntegerRes val size: Int
) {
    THUMBNAIL(R.string.image_size_thumbnail_label, R.string.image_size_thumbnail_css_class,
        R.integer.image_size_thumbnail_px),
    MEDIUM(R.string.image_size_medium_label, R.string.image_size_medium_css_class, R.integer.image_size_medium_px),
    LARGE(R.string.image_size_large_label, R.string.image_size_large_css_class, R.integer.image_size_large_px),
    FULL(R.string.image_size_full_label, R.string.image_size_full_css_class, R.integer.image_size_large_px);

    companion object {
        fun fromCssClass(context: Context, cssClass: String?): MediaSettingsImageSize =
            entries.firstOrNull { context.getString(it.cssClass) == cssClass } ?: FULL
    }
}
