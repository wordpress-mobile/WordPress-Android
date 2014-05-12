package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.Context;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.wordpress.android.util.DisplayUtils;

class ReaderFullScreenUtils {
    public static interface FullScreenListener {
        public boolean onRequestFullScreen(boolean enable);
        public boolean isFullScreen();
        public boolean isFullScreenSupported();
    }

    static void enableActionBarOverlay(Activity activity) {
        activity.getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
    }

    /**
     * add a header to the listView that's the same height as the ActionBar - used when fullscreen
     * mode is supported, should be called before adding any other listView headers
     */
    static void addListViewHeader(Context context, ListView listView) {
        final int actionbarHeight = DisplayUtils.getActionBarHeight(context);
        RelativeLayout headerFake = new RelativeLayout(context);
        headerFake.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, actionbarHeight));
        listView.addHeaderView(headerFake, null, false);
    }

    /**
     * returns true if the listView can scroll up/down vertically
     */
    static boolean canScrollUp(ListView listView) {
        if (listView == null) {
            return false;
        }
        return listView.canScrollVertically(-1);
    }

    static boolean canScrollDown(ListView listView) {
        if (listView == null) {
            return false;
        }
        return listView.canScrollVertically(1);
    }
}
