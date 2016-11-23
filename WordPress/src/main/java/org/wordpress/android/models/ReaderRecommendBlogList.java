package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReaderRecommendBlogList extends ArrayList<ReaderRecommendedBlog> {

    @Override
    public Object clone() {
        return super.clone();
    }

    public static ReaderRecommendBlogList fromJson(JSONObject json) {
        ReaderRecommendBlogList blogs = new ReaderRecommendBlogList();

        if (json == null) {
            return blogs;
        }

        JSONArray jsonBlogs = json.optJSONArray("blogs");
        if (jsonBlogs != null) {
            for (int i = 0; i < jsonBlogs.length(); i++)
                blogs.add(ReaderRecommendedBlog.fromJson(jsonBlogs.optJSONObject(i)));
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

    public boolean isSameList(ReaderRecommendBlogList blogs) {
        if (blogs == null || blogs.size() != this.size()) {
            return false;
        }

        for (ReaderRecommendedBlog blog: blogs) {
            int index = indexOfBlogId(blog.blogId);
            if (index == -1 || !this.get(index).isSameAs(blog)) {
                return false;
            }
        }

        return true;
    }

}
