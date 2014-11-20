package org.wordpress.android.models;

import org.json.JSONArray;
import org.json.JSONObject;

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

    private int indexOfPost(long blogId, long postId) {
        for (int i = 0; i < size(); i++) {
            if (this.get(i).blogId == blogId && this.get(i).postId == postId) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfPost(ReaderPost post) {
        if (post == null) {
            return -1;
        }
        return indexOfPost(post.blogId, post.postId);
    }

    /*
     * does passed list contain the same posts as this list?
     */
    public boolean isSameList(ReaderPostList posts) {
        if (posts == null || posts.size() != this.size()) {
            return false;
        }

        for (ReaderPost post: posts) {
            int index = indexOfPost(post.blogId, post.postId);
            if (index == -1 || !post.isSamePost(this.get(index))) {
                return false;
            }
        }

        return true;
    }

    /*
     * returns posts in this list which are in the passed blog
     */
    public ReaderPostList getPostsInBlog(long blogId) {
        ReaderPostList postsInBlog = new ReaderPostList();
        for (ReaderPost post: this) {
            if (post.blogId == blogId) {
                postsInBlog.add(post);
            }
        }
        return postsInBlog;
    }
}
