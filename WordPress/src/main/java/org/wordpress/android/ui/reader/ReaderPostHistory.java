package org.wordpress.android.ui.reader;

import android.os.Bundle;

import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.Stack;

/*
 * used to maintain a history of posts viewed in the detail fragment so we can navigate back
 * through them when the user hits the back button - currently used only for related posts
 */
class ReaderPostHistory extends Stack<ReaderBlogIdPostId> {
    private static final String HISTORY_KEY_NAME = "reader_post_history";

    void restoreInstance(Bundle bundle) {
        clear();
        if (bundle != null && bundle.containsKey(HISTORY_KEY_NAME)) {
            ArrayList<String> history = bundle.getStringArrayList(HISTORY_KEY_NAME);
            if (history != null) {
                this.fromArrayList(history);
            }
        }
    }

    void saveInstance(Bundle bundle) {
        if (bundle != null && !isEmpty()) {
            bundle.putStringArrayList(HISTORY_KEY_NAME, this.toArrayList());
        }
    }

    private ArrayList<String> toArrayList() {
        ArrayList<String> list = new ArrayList<>();
        for (ReaderBlogIdPostId ids : this) {
            list.add(ids.getBlogId() + ":" + ids.getPostId());
        }
        return list;
    }

    private void fromArrayList(ArrayList<String> list) {
        if (list == null || list.isEmpty()) return;

        for (String idPair: list) {
            String[] split = idPair.split(":");
            long blogId = StringUtils.stringToLong(split[0]);
            long postId = StringUtils.stringToLong(split[1]);
            this.add(new ReaderBlogIdPostId(blogId, postId));
        }
    }
}