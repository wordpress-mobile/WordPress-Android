package org.wordpress.android.ui.reader.models;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReaderRelatedPostList extends ArrayList<ReaderRelatedPost> {

    public static ReaderRelatedPostList fromJsonPosts(@NonNull JSONArray jsonPosts) {
        ReaderRelatedPostList posts = new ReaderRelatedPostList();
        for (int i = 0; i < jsonPosts.length(); i++) {
            JSONObject jsonRelatedPost = jsonPosts.optJSONObject(i);
            if (jsonRelatedPost != null) {
                ReaderRelatedPost relatedPost = ReaderRelatedPost.fromJson(jsonRelatedPost);
                if (relatedPost != null) {
                    posts.add(relatedPost);
                }
            }
        }
        return posts;
    }

}
