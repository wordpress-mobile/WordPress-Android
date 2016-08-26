package org.wordpress.android.fluxc.model;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.Payload;

import java.util.ArrayList;
import java.util.List;

public class PostsModel extends Payload {
    private List<PostModel> mPosts;

    public PostsModel() {
        mPosts = new ArrayList<>();
    }

    public PostsModel(@NonNull List<PostModel> posts) {
        mPosts = posts;
    }

    public List<PostModel> getPosts() {
        return mPosts;
    }

    public void setSites(List<PostModel> posts) {
        this.mPosts = posts;
    }
}
