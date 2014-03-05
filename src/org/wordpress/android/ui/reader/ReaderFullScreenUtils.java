package org.wordpress.android.ui.reader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.SysUtils;

/**
 * Created by nbradbury on 2/8/14.
 */
class ReaderFullScreenUtils {
    public static interface FullScreenListener {
        public boolean onRequestFullScreen(boolean enable);
        public boolean isFullScreen();
        public boolean isFullScreenSupported();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static void enableActionBarOverlay(Activity activity) {
        if ((SysUtils.isGteAndroid4()))
            activity.getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
    }

    /*
     * add a header to the listView that's the same height as the ActionBar - used when fullscreen
     * mode is supported, should be called before adding any other listView headers
     */
    static void addListViewHeader(Context context, ListView listView) {
        final int actionbarHeight = DisplayUtils.getActionBarHeight(context);
        RelativeLayout headerFake = new RelativeLayout(context);
        headerFake.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, actionbarHeight));
        listView.addHeaderView(headerFake, null, false);
    }

    /*
     * returns true if the listView can scroll up/down vertically - always returns true prior to ICS
     * because canScrollVertically() requires API 14
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static boolean canScrollUp(ListView listView) {
        if (listView == null)
            return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            return true;
        return listView.canScrollVertically(-1);
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static boolean canScrollDown(ListView listView) {
        if (listView == null)
            return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            return true;
        return listView.canScrollVertically(1);
    }
}
