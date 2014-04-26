package org.wordpress.android.models;

import java.util.ArrayList;

public class ReaderBlogInfoList extends ArrayList<ReaderBlogInfo> {

    private int indexOfBlogId(long blogId) {
        for (int i = 0; i < size(); i++) {
            if (this.get(i).blogId == blogId)
                return i;
        }
        return -1;
    }

    public boolean isSameList(ReaderBlogInfoList blogs) {
        if (blogs == null || blogs.size() != this.size()) {
            return false;
        }

        for (ReaderBlogInfo blogInfo: blogs) {
            int index = indexOfBlogId(blogInfo.blogId);
            if (index == -1) {
                return false;
            }

            if (!this.get(index).isSameAs(blogInfo)) {
                return false;
            }
        }

        return true;
    }

}
