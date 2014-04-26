package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReaderRecommendBlogList extends ArrayList<ReaderRecommendedBlog> {

    public static ReaderRecommendBlogList fromJson(JSONObject json) {
        ReaderRecommendBlogList blogs = new ReaderRecommendBlogList();

        if (json == null) {
            return blogs;
        }

        JSONArray jsonBlogs = json.optJSONArray("blogs");
        if (jsonBlogs != null) {
            for (int i=0; i < jsonBlogs.length(); i++)
                blogs.add(ReaderRecommendedBlog.fromJson(jsonBlogs.optJSONObject(i)));
        }

        return blogs;
    }

}
