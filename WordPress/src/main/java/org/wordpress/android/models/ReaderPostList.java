package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;

import java.util.ArrayList;

public class ReaderPostList extends ArrayList<ReaderPost> {
    public static ReaderPostList fromJson(JSONObject json) {
        if (json == null) {
            throw new IllegalArgumentException("null json post list");
        }

        ReaderPostList posts = new ReaderPostList();
        JSONArray jsonPosts = json.optJSONArray("posts");
        if (jsonPosts != null) {
            for (int i = 0; i < jsonPosts.length(); i++) {
                posts.add(ReaderPost.fromJson(jsonPosts.optJSONObject(i)));
            }
        }

        return posts;
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    public int indexOfPost(ReaderPost post) {
        if (post == null) {
            return -1;
        }
        for (int i = 0; i < size(); i++) {
            if (this.get(i).postId == post.postId) {
                if (post.isExternal && post.feedId == this.get(i).feedId) {
                    return i;
                } else if (!post.isExternal && post.blogId == this.get(i).blogId) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int indexOfIds(ReaderBlogIdPostId ids) {
        if (ids == null) {
            return -1;
        }
        for (int i = 0; i < size(); i++) {
            if (this.get(i).hasIds(ids)) {
                return i;
            }
        }
        return -1;
    }
}
