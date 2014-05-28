package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReaderBlogList extends ArrayList<ReaderBlog> {

    public static ReaderBlogList fromJson(JSONObject json) {
        ReaderBlogList blogs = new ReaderBlogList();
        if (json == null) {
            return blogs;
        }

        JSONArray jsonBlogs = json.optJSONArray("subscriptions");
        if (jsonBlogs != null) {
            for (int i = 0; i < jsonBlogs.length(); i++)
                blogs.add(ReaderBlog.fromJson(jsonBlogs.optJSONObject(i)));
        }

        return blogs;
    }

    private int indexOfBlogId(long blogId) {
        for (int i = 0; i < size(); i++) {
            if (this.get(i).blogId == blogId)
                return i;
        }
        return -1;
    }

    private int indexOfFeedId(long feedId) {
        for (int i = 0; i < size(); i++) {
            if (this.get(i).feedId == feedId)
                return i;
        }
        return -1;
    }

    public boolean isSameList(ReaderBlogList blogs) {
        if (blogs == null || blogs.size() != this.size()) {
            return false;
        }

        for (ReaderBlog blogInfo: blogs) {
            int index;
            if (blogInfo.isExternal()) {
                index = indexOfFeedId(blogInfo.feedId);
            } else {
                index = indexOfBlogId(blogInfo.blogId);
            }
            if (index == -1 || !this.get(index).isSameAs(blogInfo)) {
                return false;
            }
        }

        return true;
    }
}
