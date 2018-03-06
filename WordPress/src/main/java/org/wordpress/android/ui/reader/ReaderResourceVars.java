package org.wordpress.android.ui.reader;

import android.content.Context;
import android.content.res.Resources;

import org.wordpress.android.R;
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

    ReaderResourceVars(Context context) {
        Resources resources = context.getResources();

        int displayWidthPx = DisplayUtils.getDisplayPixelWidth(context);

        mIsWideDisplay = DisplayUtils.pxToDp(context, displayWidthPx) > 640;

        int marginLargePx = resources.getDimensionPixelSize(R.dimen.margin_large);
        int detailMarginWidthPx = resources.getDimensionPixelOffset(R.dimen.reader_detail_margin);

        mFeaturedImageHeightPx = resources.getDimensionPixelSize(R.dimen.reader_featured_image_height);
        mMarginMediumPx = resources.getDimensionPixelSize(R.dimen.margin_medium);

        mLinkColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.reader_hyperlink);
        mGreyMediumDarkStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_darken_30);
        mGreyLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_light);
        mGreyExtraLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_lighten_30);

        // full-size image width must take margin into account
        mFullSizeImageWidthPx = displayWidthPx - (detailMarginWidthPx * 2);

        // 16:9 ratio (YouTube standard)
        mVideoWidthPx = mFullSizeImageWidthPx - (marginLargePx * 2);
        mVideoHeightPx = (int) (mVideoWidthPx * 0.5625f);
    }
}
