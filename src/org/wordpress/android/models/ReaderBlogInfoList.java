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
            if (index == -1 || !this.get(index).isSameAs(blogInfo)) {
                return false;
            }
        }

        return true;
    }

    /*
     * returns list of incomplete blogInfos in this list
     */
    public ReaderBlogInfoList getIncompleteList() {
        ReaderBlogInfoList incompleteList = new ReaderBlogInfoList();
        for (ReaderBlogInfo info: this) {
            if (info.isIncomplete()) {
                incompleteList.add(info);
            }
        }
        return incompleteList;
    }

    /*
     * remove information about incomplete blogs
     */
    public void removeIncomplete() {
        this.removeAll(getIncompleteList());
    }

}
