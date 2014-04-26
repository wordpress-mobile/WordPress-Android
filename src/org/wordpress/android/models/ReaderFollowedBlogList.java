package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReaderFollowedBlogList extends ArrayList<ReaderFollowedBlog> {

    public static ReaderFollowedBlogList fromJson(JSONObject json) {
        ReaderFollowedBlogList blogs = new ReaderFollowedBlogList();

        if (json == null) {
            return blogs;
        }

        JSONArray jsonBlogs = json.optJSONArray("subscriptions");
        if (jsonBlogs != null) {
            for (int i = 0; i < jsonBlogs.length(); i++) {
                blogs.add(ReaderFollowedBlog.fromJson(jsonBlogs.optJSONObject(i)));
            }
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

    public boolean isSameList(ReaderFollowedBlogList blogs) {
        if (blogs == null || blogs.size() != this.size()) {
            return false;
        }

        for (ReaderFollowedBlog blog: blogs) {
            if (indexOfBlogId(blog.blogId) == -1) {
                return false;
            }
        }

        return true;
    }

}
