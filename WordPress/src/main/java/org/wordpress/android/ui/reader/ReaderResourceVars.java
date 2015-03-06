package org.wordpress.android.ui.reader;

import android.content.Context;
import android.content.res.Resources;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPDrawerActivity;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;

/*
 * class which holds all resource-based variables used when rendering post detail
 */
class ReaderResourceVars {
    final int displayWidthPx;
    final int likeAvatarSizePx;
    final int headerAvatarSizePx;

    final int marginLargePx;
    final int marginSmallPx;
    final int marginExtraSmallPx;
    final int detailMarginWidthPx;

    final int fullSizeImageWidthPx;
    final int featuredImageHeightPx;

    final int videoWidthPx;
    final int videoHeightPx;

    final int colorGreyExtraLight;
    final int mediumAnimTime;

    final String linkColorStr;
    final String greyMediumDarkStr;
    final String greyLightStr;
    final String greyExtraLightStr;

    ReaderResourceVars(Context context) {
        Resources resources = context.getResources();

        displayWidthPx = DisplayUtils.getDisplayPixelWidth(context);
        likeAvatarSizePx = resources.getDimensionPixelSize(R.dimen.avatar_sz_small);
        headerAvatarSizePx = resources.getDimensionPixelSize(R.dimen.avatar_sz_medium);
        featuredImageHeightPx = resources.getDimensionPixelSize(R.dimen.reader_featured_image_height);

        marginLargePx = resources.getDimensionPixelSize(R.dimen.margin_large);
        marginSmallPx = resources.getDimensionPixelSize(R.dimen.margin_small);
        marginExtraSmallPx = resources.getDimensionPixelSize(R.dimen.margin_extra_small);
        detailMarginWidthPx = resources.getDimensionPixelOffset(R.dimen.reader_detail_margin);

        colorGreyExtraLight = resources.getColor(R.color.grey_lighten_30);
        mediumAnimTime = resources.getInteger(android.R.integer.config_mediumAnimTime);

        linkColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.reader_hyperlink);
        greyMediumDarkStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_darken_30);
        greyLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_light);
        greyExtraLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_lighten_30);

        // full-size image width must take margin and padding into account
        int listPadding = resources.getDimensionPixelOffset(R.dimen.margin_large);
        int imageWidth = displayWidthPx - (detailMarginWidthPx * 2) - (listPadding * 2);
        boolean hasStaticMenuDrawer =
                (context instanceof WPDrawerActivity)
                        && (((WPDrawerActivity) context).isStaticMenuDrawer());
        if (hasStaticMenuDrawer) {
            int drawerWidth = resources.getDimensionPixelOffset(R.dimen.drawer_width_static);
            imageWidth -= drawerWidth;
        }
        fullSizeImageWidthPx = imageWidth;

        // 16:9 ratio (YouTube standard)
        videoWidthPx = fullSizeImageWidthPx - (marginLargePx * 2);
        videoHeightPx = (int) (videoWidthPx * 0.5625f);
    }
}
