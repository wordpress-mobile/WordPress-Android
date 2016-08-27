package org.wordpress.android.models;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.ui.posts.services.PostUploadService;

import java.util.ArrayList;
import java.util.List;

public class PostsListPostList extends ArrayList<PostModel> {

    // TODO: Delete when PostsListPostList is phased out
    public boolean isSameList(PostsListPostList newPostsList) {
        return isSameList((List<PostModel>) newPostsList);
    }

    public boolean isSameList(List<PostModel> newPostsList) {
        if (newPostsList == null || this.size() != newPostsList.size()) {
            return false;
        }

        for (int i = 0; i < newPostsList.size(); i++) {
            PostModel newPost = newPostsList.get(i);
            PostModel currentPost = this.get(i);

            if (newPost.getId() != currentPost.getId())
                return false;
            if (!newPost.getTitle().equals(currentPost.getTitle()))
                return false;
            if (!newPost.getDateCreated().equals(currentPost.getDateCreated()))
                return false;
            if (!newPost.getStatus().equals(currentPost.getStatus()))
                return false;
            if (PostUploadService.isPostUploading(newPost) != PostUploadService.isPostUploading(currentPost))
                return false;
            if (newPost.isLocalDraft() != currentPost.isLocalDraft())
                return false;
            if (newPost.isLocallyChanged() != currentPost.isLocallyChanged())
                return false;
            if (!newPost.getContent().equals(currentPost.getContent()))
                return false;
        }

        return true;
    }

    public int indexOfPost(PostModel post) {
        if (post == null) {
            return -1;
        }
        for (int i = 0; i < size(); i++) {
            if (this.get(i).getId() == post.getId() && this.get(i).getLocalSiteId() == post.getLocalSiteId()) {
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
