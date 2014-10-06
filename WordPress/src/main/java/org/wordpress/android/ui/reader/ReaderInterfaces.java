package org.wordpress.android.ui.reader;

import android.view.View;

import org.wordpress.android.models.ReaderPost;

public class ReaderInterfaces {

    static interface OnPostSelectedListener {
        public void onPostSelected(long blogId, long postId);
    }

    public static interface OnTagSelectedListener {
        public void onTagSelected(String tagName);
    }

    /*
     * called when user taps the dropdown arrow next to a post to show the popup menu
     */
    public static interface OnPostPopupListener {
        public void onShowPostPopup(View view, ReaderPost post);
    }

    /*
     * used by adapters to notify when data has been loaded
     */
    public interface DataLoadedListener {
        public void onDataLoaded(boolean isEmpty);
    }

    /*
     * used by post list & post list adapter when user asks to reblog a post
     */
    public interface RequestReblogListener {
        public void onRequestReblog(ReaderPost post, View sourceView);
    }

    /*
     * used by ReaderPostDetailFragment to enter/exit full screen mode
     */
    public static interface FullScreenListener {
        boolean onRequestFullScreen(boolean enable);
        boolean isFullScreen();
        boolean isFullScreenSupported();
    }
}
