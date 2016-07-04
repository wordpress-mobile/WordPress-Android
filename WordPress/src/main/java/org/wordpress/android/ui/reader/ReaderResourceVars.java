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
    final int marginSmallPx;
    final int marginExtraSmallPx;

    final boolean isWideDisplay;

    final int fullSizeImageWidthPx;
    final int featuredImageHeightPx;

    final int videoWidthPx;
    final int videoHeightPx;

    final String linkColorStr;
    final String greyMediumDarkStr;
    final String greyLightStr;
    final String greyExtraLightStr;

    ReaderResourceVars(Context context) {
        Resources resources = context.getResources();

        int displayWidthPx = DisplayUtils.getDisplayPixelWidth(context);

        isWideDisplay = DisplayUtils.pxToDp(context, displayWidthPx) > 640;

        int marginLargePx = resources.getDimensionPixelSize(R.dimen.margin_large);
        int detailMarginWidthPx = resources.getDimensionPixelOffset(R.dimen.reader_detail_margin);

        featuredImageHeightPx = resources.getDimensionPixelSize(R.dimen.reader_featured_image_height);
        marginSmallPx = resources.getDimensionPixelSize(R.dimen.margin_small);
        marginExtraSmallPx = resources.getDimensionPixelSize(R.dimen.margin_extra_small);

        linkColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.reader_hyperlink);
        greyMediumDarkStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_darken_30);
        greyLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_light);
        greyExtraLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_lighten_30);

        // full-size image width must take margin into account
        fullSizeImageWidthPx = displayWidthPx - (detailMarginWidthPx * 2);

        // 16:9 ratio (YouTube standard)
        videoWidthPx = fullSizeImageWidthPx - (marginLargePx * 2);
        videoHeightPx = (int) (videoWidthPx * 0.5625f);
    }
}
