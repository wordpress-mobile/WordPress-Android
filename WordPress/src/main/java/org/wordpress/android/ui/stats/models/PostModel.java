package org.wordpress.android.ui.stats.models;

import java.io.Serializable;

public class PostModel extends SingleItemModel implements Serializable {

    private String mPostType = "post";

    public PostModel(String blogId, String date, String itemID, String title, int totals, String url, String icon, String postType) {
        super(blogId, date, itemID, title, totals, url, icon);
        this.mPostType = postType;
    }

    public PostModel(String blogId, long date, String itemID, String title, int totals, String url, String icon, String postType) {
        super(blogId, date, itemID, title, totals, url, icon);
        this.mPostType = postType;
    }

    public String getPostType() {
        return mPostType;
    }
}
