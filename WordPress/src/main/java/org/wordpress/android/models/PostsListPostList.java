package org.wordpress.android.models;

import java.util.ArrayList;

public class PostsListPostList extends ArrayList<PostsListPost> {

    public boolean isSameList(PostsListPostList newPostsList) {
        if (newPostsList == null || this.size() != newPostsList.size()) {
            return false;
        }

        for (int i = 0; i < newPostsList.size(); i++) {
            PostsListPost newPost = newPostsList.get(i);
            PostsListPost currentPost = this.get(i);

            if (newPost.getPostId() != currentPost.getPostId())
                return false;
            if (!newPost.getTitle().equals(currentPost.getTitle()))
                return false;
            if (newPost.getDateCreatedGmt() != currentPost.getDateCreatedGmt())
                return false;
            if (!newPost.getOriginalStatus().equals(currentPost.getOriginalStatus()))
                return false;
            if (newPost.isUploading() != currentPost.isUploading())
                return false;
            if (newPost.isLocalDraft() != currentPost.isLocalDraft())
                return false;
            if (newPost.hasLocalChanges() != currentPost.hasLocalChanges())
                return false;
            if (!newPost.getDescription().equals(currentPost.getDescription()))
                return false;
        }

        return true;
    }

    public int indexOfPost(PostsListPost post) {
        if (post == null) {
            return -1;
        }
        for (int i = 0; i < size(); i++) {
            if (this.get(i).getPostId() == post.getPostId() && this.get(i).getBlogId() == post.getBlogId()) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfFeaturedMediaId(long mediaId) {
        if (mediaId == 0) {
            return -1;
        }
        for (int i = 0; i < size(); i++) {
            if (this.get(i).getFeaturedImageId() == mediaId) {
                return i;
            }
        }
        return -1;
    }
}
