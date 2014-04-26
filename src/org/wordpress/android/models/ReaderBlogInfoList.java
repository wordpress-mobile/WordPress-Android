package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReaderBlogInfoList extends ArrayList<ReaderFollowedBlog> {

    public static ReaderBlogInfoList fromJson(JSONObject json) {
        ReaderBlogInfoList blogs = new ReaderBlogInfoList();

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

}
