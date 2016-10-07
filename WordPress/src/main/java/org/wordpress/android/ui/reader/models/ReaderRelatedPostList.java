package org.wordpress.android.ui.reader.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReaderRelatedPostList extends ArrayList<ReaderRelatedPost> {

    public static ReaderRelatedPostList fromJson(JSONObject json) {
        if (json == null) {
            throw new IllegalArgumentException("null json related post list");
        }

        ReaderRelatedPostList posts = new ReaderRelatedPostList();
        JSONArray jsonPosts = json.optJSONArray("posts");
        if (jsonPosts != null) {
            for (int i = 0; i < jsonPosts.length(); i++) {
                posts.add(ReaderRelatedPost.fromJson(jsonPosts.optJSONObject(i)));
            }
        }

        return posts;
    }

}
