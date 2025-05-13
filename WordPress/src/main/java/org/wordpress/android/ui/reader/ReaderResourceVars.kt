package org.wordpress.android.ui.reader

import android.content.Context
import android.graphics.Color
import androidx.core.content.res.ResourcesCompat
import org.wordpress.android.R
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.extensions.getColorFromAttribute

/*
* class which holds all resource-based variables used when rendering post detail
*/
internal class ReaderResourceVars(context: Context) {
    val marginMediumPx: Int

    val isWideDisplay: Boolean

    val fullSizeImageWidthPx: Int
    val featuredImageHeightPx: Int

    val videoWidthPx: Int
    val videoHeightPx: Int

    val linkColorStr: String
    val greyMediumDarkStr: String
    val greyLightStr: String
    val greyExtraLightStr: String
    val textColor: String
    val greyDisabledStr: String

    init {
        val resources = context.resources

        val displayWidthPx = DisplayUtils.getWindowPixelWidth(context)

        isWideDisplay = DisplayUtils.pxToDp(context, displayWidthPx) > 640

        val marginLargePx = resources.getDimensionPixelSize(R.dimen.margin_large)
        val detailMarginWidthPx = resources.getDimensionPixelOffset(R.dimen.reader_detail_margin)

        featuredImageHeightPx =
            resources.getDimensionPixelSize(R.dimen.reader_featured_image_height)
        marginMediumPx = resources.getDimensionPixelSize(R.dimen.margin_medium)

        val onSurfaceColor =
            context.getColorFromAttribute(com.google.android.material.R.attr.colorOnSurface)

        val onSurfaceHighType = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(
            resources,
            com.google.android.material.R.dimen.material_emphasis_high_type
        )
                + ")")

        greyMediumDarkStr = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(
            resources,
            com.google.android.material.R.dimen.material_emphasis_medium
        )
                + ")")

        greyLightStr = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(
            resources,
            com.google.android.material.R.dimen.material_emphasis_disabled
        )
                + ")")

        greyExtraLightStr = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(resources, R.dimen.emphasis_low) + ")")

        greyDisabledStr = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(
            resources,
            com.google.android.material.R.dimen.material_emphasis_disabled
        )
                + ")")

        textColor = onSurfaceHighType
        linkColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.reader_post_body_link)

        // full-size image width must take margin into account
        fullSizeImageWidthPx = displayWidthPx - (detailMarginWidthPx * 2)

        // 16:9 ratio (YouTube standard)
        videoWidthPx = fullSizeImageWidthPx - (marginLargePx * 2)
        videoHeightPx = (videoWidthPx * 0.5625f).toInt()
    }
}
