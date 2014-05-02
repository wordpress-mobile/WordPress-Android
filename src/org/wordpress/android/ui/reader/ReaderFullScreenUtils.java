package org.wordpress.android.ui.reader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.SysUtils;

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
        addListViewHeader(context, listView, DisplayUtils.getActionBarHeight(context));
    }

    static void addListViewHeader(Context context, ListView listView, int height) {
        RelativeLayout headerFake = new RelativeLayout(context);
        headerFake.setLayoutParams(new AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                height));
        listView.addHeaderView(headerFake, null, false);
    }

    /*
     * add a top margin to the passed view that's the same height as the ActionBar
     */
    static void addTopMargin(Context context, View view) {
        if (view.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
            params.topMargin = DisplayUtils.getActionBarHeight(context);
        }
    }
}
