package org.wordpress.android.ui.reader;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

import androidx.core.content.res.ResourcesCompat;

import org.wordpress.android.R;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;

/*
 * class which holds all resource-based variables used when rendering post detail
 */
class ReaderResourceVars {
    final int mMarginMediumPx;

    final boolean mIsWideDisplay;

    final int mFullSizeImageWidthPx;
    final int mFeaturedImageHeightPx;

    final int mVideoWidthPx;
    final int mVideoHeightPx;

    final String mLinkColorStr;
    final String mGreyMediumDarkStr;
    final String mGreyLightStr;
    final String mGreyExtraLightStr;
    final String mTextColor;

    ReaderResourceVars(Context context) {
        Resources resources = context.getResources();

        int displayWidthPx = DisplayUtils.getDisplayPixelWidth(context);

        mIsWideDisplay = DisplayUtils.pxToDp(context, displayWidthPx) > 640;

        int marginLargePx = resources.getDimensionPixelSize(R.dimen.margin_large);
        int detailMarginWidthPx = resources.getDimensionPixelOffset(R.dimen.reader_detail_margin);

        mFeaturedImageHeightPx = resources.getDimensionPixelSize(R.dimen.reader_featured_image_height);
        mMarginMediumPx = resources.getDimensionPixelSize(R.dimen.margin_medium);

        int onSurfaceColor = ContextExtensionsKt
                .getColorFromAttribute(context, R.attr.colorOnSurface);

        String onSurfaceHighType = "rgba(" + Color.red(onSurfaceColor) + ", "
                                 + Color.green(onSurfaceColor) + ", " + Color
                                         .blue(onSurfaceColor) + ", " + ResourcesCompat
                                         .getFloat(resources, R.dimen.material_emphasis_high_type) + ")";

        mGreyMediumDarkStr = "rgba(" + Color.red(onSurfaceColor) + ", "
                                 + Color.green(onSurfaceColor) + ", " + Color
                                         .blue(onSurfaceColor) + ", " + ResourcesCompat
                                         .getFloat(resources, R.dimen.material_emphasis_medium) + ")";

        mGreyLightStr = "rgba(" + Color.red(onSurfaceColor) + ", "
                        + Color.green(onSurfaceColor) + ", " + Color
                                           .blue(onSurfaceColor) + ", " + ResourcesCompat
                                           .getFloat(resources, R.dimen.material_emphasis_disabled) + ")";

        mGreyExtraLightStr = "rgba(" + Color.red(onSurfaceColor) + ", "
                             + Color.green(onSurfaceColor) + ", " + Color
                                     .blue(onSurfaceColor) + ", " + ResourcesCompat
                                     .getFloat(resources, R.dimen.emphasis_low) + ")";

        mTextColor = onSurfaceHighType;
        mLinkColorStr = HtmlUtils.colorResToHtmlColor(context,
                ContextExtensionsKt.getColorResIdFromAttribute(context, R.attr.colorPrimary));

        // full-size image width must take margin into account
        mFullSizeImageWidthPx = displayWidthPx - (detailMarginWidthPx * 2);

        // 16:9 ratio (YouTube standard)
        mVideoWidthPx = mFullSizeImageWidthPx - (marginLargePx * 2);
        mVideoHeightPx = (int) (mVideoWidthPx * 0.5625f);
    }
}
