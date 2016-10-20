package org.wordpress.android.fluxc.post;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.PostModel;

import java.util.List;

public class PostTestUtils {
    public static PostModel generateSampleUploadedPost() {
        return generateSampleUploadedPost("text");
    }

    public static PostModel generateSampleUploadedPost(String postFormat) {
        PostModel example = new PostModel();
        example.setLocalSiteId(6);
        example.setRemotePostId(5);
        example.setTitle("A test post");
        example.setContent("Bunch of content here");
        example.setPostFormat(postFormat);
        return example;
    }

    public static PostModel generateSampleLocalDraftPost() {
        PostModel example = new PostModel();
        example.setLocalSiteId(6);
        example.setTitle("A test post");
        example.setContent("Bunch of content here");
        example.setIsLocalDraft(true);
        return example;
    }

    public static PostModel generateSampleLocallyChangedPost() {
        PostModel example = new PostModel();
        example.setLocalSiteId(6);
        example.setRemotePostId(7);
        example.setTitle("A test post");
        example.setContent("Bunch of content here");
        example.setIsLocallyChanged(true);
        return example;
    }

    public static List<PostModel> getPosts() {
        return WellSql.select(PostModel.class).getAsModel();
    }

    public static int getPostsCount() {
        return getPosts().size();
    }
}
