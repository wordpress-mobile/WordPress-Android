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
    val mMarginMediumPx: Int

    val mIsWideDisplay: Boolean

    val mFullSizeImageWidthPx: Int
    val mFeaturedImageHeightPx: Int

    val mVideoWidthPx: Int
    val mVideoHeightPx: Int

    val mLinkColorStr: String
    val mGreyMediumDarkStr: String
    val mGreyLightStr: String
    val mGreyExtraLightStr: String
    val mTextColor: String
    val mGreyDisabledStr: String

    init {
        val resources = context.resources

        val displayWidthPx = DisplayUtils.getWindowPixelWidth(context)

        mIsWideDisplay = DisplayUtils.pxToDp(context, displayWidthPx) > 640

        val marginLargePx = resources.getDimensionPixelSize(R.dimen.margin_large)
        val detailMarginWidthPx = resources.getDimensionPixelOffset(R.dimen.reader_detail_margin)

        mFeaturedImageHeightPx =
            resources.getDimensionPixelSize(R.dimen.reader_featured_image_height)
        mMarginMediumPx = resources.getDimensionPixelSize(R.dimen.margin_medium)

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

        mGreyMediumDarkStr = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(
            resources,
            com.google.android.material.R.dimen.material_emphasis_medium
        )
                + ")")

        mGreyLightStr = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(
            resources,
            com.google.android.material.R.dimen.material_emphasis_disabled
        )
                + ")")

        mGreyExtraLightStr = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(resources, R.dimen.emphasis_low) + ")")

        mGreyDisabledStr = ("rgba(" + Color.red(onSurfaceColor) + ", "
                + Color.green(onSurfaceColor) + ", "
                + Color.blue(onSurfaceColor) + ", "
                + ResourcesCompat.getFloat(
            resources,
            com.google.android.material.R.dimen.material_emphasis_disabled
        )
                + ")")

        mTextColor = onSurfaceHighType
        mLinkColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.reader_post_body_link)

        // full-size image width must take margin into account
        mFullSizeImageWidthPx = displayWidthPx - (detailMarginWidthPx * 2)

        // 16:9 ratio (YouTube standard)
        mVideoWidthPx = mFullSizeImageWidthPx - (marginLargePx * 2)
        mVideoHeightPx = (mVideoWidthPx * 0.5625f).toInt()
    }
}
