package org.wordpress.android.fluxc.post;

import androidx.annotation.NonNull;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.revisions.Diff;
import org.wordpress.android.fluxc.model.revisions.DiffOperations;
import org.wordpress.android.fluxc.model.revisions.RevisionModel;

import java.util.ArrayList;
import java.util.List;

public class PostTestUtils {
    public static final double EXAMPLE_LATITUDE = 44.8378;
    public static final double EXAMPLE_LONGITUDE = -0.5792;

    static final int DEFAULT_LOCAL_SITE_ID = 6;

    public static PostModel generateSampleUploadedPost() {
        return generateSampleUploadedPost("text");
    }

    public static PostModel generateSampleUploadedPost(String postFormat) {
        PostModel example = new PostModel();
        example.setLocalSiteId(DEFAULT_LOCAL_SITE_ID);
        example.setRemotePostId(5);
        example.setTitle("A test post");
        example.setContent("Bunch of content here");
        example.setPostFormat(postFormat);
        return example;
    }

    public static PostModel generateSampleLocalDraftPost() {
        return generateSampleLocalDraftPost("A test post");
    }

    static PostModel generateSampleLocalDraftPost(@NonNull String title) {
        PostModel example = new PostModel();
        example.setLocalSiteId(DEFAULT_LOCAL_SITE_ID);
        example.setTitle(title);
        example.setContent("Bunch of content here");
        example.setIsLocalDraft(true);
        return example;
    }

    static PostModel generateSampleLocallyChangedPost() {
        return generateSampleLocallyChangedPost("A test post");
    }

    static PostModel generateSampleLocallyChangedPost(@NonNull String title) {
        PostModel example = new PostModel();
        example.setLocalSiteId(DEFAULT_LOCAL_SITE_ID);
        example.setRemotePostId(7);
        example.setTitle(title);
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

    public static RevisionModel generateSamplePostRevision() {
        ArrayList<Diff> testTitleDiffs = new ArrayList<>();
        testTitleDiffs.add(new Diff(DiffOperations.COPY, "copy title"));
        testTitleDiffs.add(new Diff(DiffOperations.COPY, "copy another title"));
        testTitleDiffs.add(new Diff(DiffOperations.ADD, "add new title"));
        testTitleDiffs.add(new Diff(DiffOperations.DELETE, "del title"));
        testTitleDiffs.add(new Diff(DiffOperations.ADD, "add different title"));

        ArrayList<Diff> testContentDiff = new ArrayList<>();
        testContentDiff.add(new Diff(DiffOperations.COPY, "copy some content"));
        testContentDiff.add(new Diff(DiffOperations.ADD, "add new content"));
        testContentDiff.add(new Diff(DiffOperations.DELETE, "del all the content"));

        return new RevisionModel(
                1,
                2,
                5,
                6,
                "post content",
                "post excerpt",
                "post title",
                "2018-09-04 12:19:34Z",
                "2018-09-05 13:19:34Z",
                "111111111",
                testTitleDiffs,
                testContentDiff
        );
    }
}
