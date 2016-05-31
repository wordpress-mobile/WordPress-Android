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

    public interface OnTagSelectedListener {
        void onTagSelected(String tagName);
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

}
