package org.wordpress.android.ui.reader;

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
     * implemented by the activity hosting a blog preview, which can't be shown if its blog info can't be loaded
     */
    public interface OnBlogInfoFailedListener {
        void onBlogInfoFailed();
    }
}
