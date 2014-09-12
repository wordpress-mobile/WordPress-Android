package org.wordpress.android.ui.reader;

import android.content.Context;
import android.content.res.Resources;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;

/*
 * class which holds all resource-based variables used when rendering post detail
 */
class ReaderResourceVars {
    final int displayWidthPx;
    final int actionBarHeightPx;
    final int likeAvatarSizePx;
    final int headerAvatarSizePx;

    final int marginLargePx;
    final int marginSmallPx;
    final int marginExtraSmallPx;
    final int listMarginWidthPx;

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
        actionBarHeightPx = DisplayUtils.getActionBarHeight(context);
        likeAvatarSizePx = resources.getDimensionPixelSize(R.dimen.avatar_sz_small);
        headerAvatarSizePx = resources.getDimensionPixelSize(R.dimen.avatar_sz_medium);
        featuredImageHeightPx = resources.getDimensionPixelSize(R.dimen.reader_featured_image_height);

        marginLargePx = resources.getDimensionPixelSize(R.dimen.margin_large);
        marginSmallPx = resources.getDimensionPixelSize(R.dimen.margin_small);
        marginExtraSmallPx = resources.getDimensionPixelSize(R.dimen.margin_extra_small);
        listMarginWidthPx = resources.getDimensionPixelOffset(R.dimen.reader_list_margin);

        colorGreyExtraLight = resources.getColor(R.color.grey_extra_light);
        mediumAnimTime = resources.getInteger(android.R.integer.config_mediumAnimTime);

        linkColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.reader_hyperlink);
        greyMediumDarkStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_medium_dark);
        greyLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_light);
        greyExtraLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_extra_light);

        // full-size image width must take list margin and padding into account
        int listPadding = resources.getDimensionPixelOffset(R.dimen.margin_large);
        int imageWidth = displayWidthPx - (listMarginWidthPx * 2) - (listPadding * 2);
        boolean hasStaticMenuDrawer =
                (context instanceof WPActionBarActivity)
                        && (((WPActionBarActivity) context).isStaticMenuDrawer());
        if (hasStaticMenuDrawer) {
            int drawerWidth = resources.getDimensionPixelOffset(R.dimen.menu_drawer_width);
            imageWidth -= drawerWidth;
        }
        fullSizeImageWidthPx = imageWidth;

        // 16:9 ratio (YouTube standard)
        videoWidthPx = fullSizeImageWidthPx - (marginLargePx * 2);
        videoHeightPx = (int) (videoWidthPx * 0.5625f);
    }
}
