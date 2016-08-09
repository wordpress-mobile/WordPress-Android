package org.wordpress.android.fluxc.post;

import org.wordpress.android.fluxc.model.PostModel;

public class PostTestUtils {
    public static PostModel generateSampleUploadedPost() {
        PostModel example = new PostModel();
        example.setId(1);
        example.setLocalSiteId(6);
        example.setRemotePostId(5);
        example.setTitle("A test post");
        example.setContent("Bunch of content here");
        return example;
    }

    public static PostModel generateSampleLocalDraftPost() {
        PostModel example = new PostModel();
        example.setId(2);
        example.setLocalSiteId(6);
        example.setTitle("A test post");
        example.setContent("Bunch of content here");
        example.setIsLocalDraft(true);
        return example;
    }

    public static PostModel generateSampleLocallyChangedPost() {
        PostModel example = new PostModel();
        example.setId(3);
        example.setLocalSiteId(6);
        example.setRemotePostId(7);
        example.setTitle("A test post");
        example.setContent("Bunch of content here");
        example.setIsLocallyChanged(true);
        return example;
    }
}
