package org.wordpress.android.ui.reader.models;

import android.support.annotation.NonNull;

import org.json.JSONArray;

import java.util.ArrayList;

public class ReaderRelatedPostList extends ArrayList<ReaderRelatedPost> {

    public static ReaderRelatedPostList fromJsonPosts(@NonNull JSONArray jsonPosts) {
        ReaderRelatedPostList posts = new ReaderRelatedPostList();
        for (int i = 0; i < jsonPosts.length(); i++) {
            posts.add(ReaderRelatedPost.fromJson(jsonPosts.optJSONObject(i)));
        }
        return posts;
    }

}
