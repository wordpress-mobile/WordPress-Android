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

    public static interface OnPostPopupListener {
        public void onShowPostPopup(View view, ReaderPost post, int position);
    }
}
