package org.wordpress.android.ui.reader;

import android.content.Context;
import android.content.res.Resources;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;

/*
 * class which holds all resource-based variables used by this fragment
 */
class ReaderResourceVars {
    final int displayWidth;
    final int actionBarHeight;
    final int likeAvatarSize;
    final int videoOverlaySize;

    final int marginLarge;
    final int marginSmall;
    final int marginExtraSmall;
    final int listMarginWidth;
    final int fullSizeImageWidth;

    final int videoWidth;
    final int videoHeight;

    final int colorGreyExtraLight;
    final int mediumAnimTime;

    final String linkColorStr;
    final String greyLightStr;
    final String greyExtraLightStr;

    ReaderResourceVars(Context context) {
        Resources resources = context.getResources();

        displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        actionBarHeight = DisplayUtils.getActionBarHeight(context);
        likeAvatarSize = resources.getDimensionPixelSize(R.dimen.avatar_sz_small);
        videoOverlaySize = resources.getDimensionPixelSize(R.dimen.reader_video_overlay_size);

        marginLarge = resources.getDimensionPixelSize(R.dimen.margin_large);
        marginSmall = resources.getDimensionPixelSize(R.dimen.margin_small);
        marginExtraSmall = resources.getDimensionPixelSize(R.dimen.margin_extra_small);
        listMarginWidth = resources.getDimensionPixelOffset(R.dimen.reader_list_margin);

        colorGreyExtraLight = resources.getColor(R.color.grey_extra_light);
        mediumAnimTime = resources.getInteger(android.R.integer.config_mediumAnimTime);

        linkColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.reader_hyperlink);
        greyLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_light);
        greyExtraLightStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_extra_light);

        int imageWidth = displayWidth - (listMarginWidth * 2);
        boolean hasStaticMenuDrawer =
                (context instanceof WPActionBarActivity)
                        && (((WPActionBarActivity) context).isStaticMenuDrawer());
        if (hasStaticMenuDrawer) {
            int drawerWidth = resources.getDimensionPixelOffset(R.dimen.menu_drawer_width);
            imageWidth -= drawerWidth;
        }
        fullSizeImageWidth = imageWidth;

        videoWidth = DisplayUtils.pxToDp(context, fullSizeImageWidth - (marginLarge * 2));
        videoHeight = (int) (videoWidth * 0.5625f);
    }
}
