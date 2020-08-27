package org.wordpress.android.ui.reader;

import android.view.View;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType;

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
     * Used by adapters to notify when button on post list item is clicked. This interface was created during
     * refactoring for the new Discover tab. It isn't ideal but we wanted to re-use some of the legacy code but
     * refactoring everything was out of scope of the project.
     */
    public interface OnPostListItemButtonListener {
        void onButtonClicked(ReaderPost post, ReaderPostCardActionType actionType);
    }

    /*
     * used by adapters to notify when post bookmarked state has changed
     */
    public interface OnPostBookmarkedListener {
        void onBookmarkClicked(long blogId, long postId);
    }
}
