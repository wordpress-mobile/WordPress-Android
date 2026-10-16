package org.wordpress.android.ui.reader;

import android.view.View;

public class ReaderInterfaces {
    private ReaderInterfaces() {
        throw new AssertionError();
    }

    /*
     * called from post detail fragment so toolbar can animate in/out when scrolling
     */
    public interface AutoHideToolbarListener {
        void onShowHideToolbar(boolean show);
    }

    /*
     * used by adapters to notify when data has been loaded
     */
    public interface DataLoadedListener {
        void onDataLoaded(boolean isEmpty);
    }

    /*
     * used by adapters to notify when follow button has been tapped
     */
    public interface OnFollowListener {
        void onFollowTapped(View view, String blogName, long blogId, long feedId);

        void onFollowingTapped();

        void onFollowTappedWhenLoggedOut();
    }
}
