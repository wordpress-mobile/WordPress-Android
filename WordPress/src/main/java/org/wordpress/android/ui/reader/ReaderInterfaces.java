package org.wordpress.android.ui.reader;

import android.view.View;

import org.wordpress.android.models.ReaderPost;

public class ReaderInterfaces {
    private ReaderInterfaces() {
        throw new AssertionError();
    }

    public interface OnPostSelectedListener {
        void onPostSelected(ReaderPost post);
    }

    /*
     * Called by the [ReaderPostAdapter] to trigger the reblog action
     */
    public interface ReblogActionListener {
        void reblog(ReaderPost post);
    }

    /*
     * called from post detail fragment so toolbar can animate in/out when scrolling
     */
    public interface AutoHideToolbarListener {
        void onShowHideToolbar(boolean show);
    }

    /*
     * called when user taps the dropdown arrow next to a post to show the popup menu
     */
    public interface OnPostPopupListener {
        void onShowPostPopup(View view, ReaderPost post);
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
        void onFollowTapped(View view, String blogName, long blogId);

        void onFollowingTapped();
    }

    /*
     * used by adapters to notify when post bookmarked state has changed
     */
    public interface OnPostBookmarkedListener {
        void onBookmarkedStateChanged(boolean isBookmarked, long blogId, long postId, boolean isCachingActionRequired);
    }
}
