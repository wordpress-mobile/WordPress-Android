package org.wordpress.android.fluxc.upload;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.post.PostTestUtils;

import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class UploadSqlUtilsTest {
    private Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    // Attempts to insert null then verifies there is no media
    @Test
    public void testInsertNullMedia() {
        Assert.assertEquals(0, UploadSqlUtils.insertOrUpdateMedia(null));
        Assert.assertEquals(0, WellSql.select(MediaUploadModel.class).getAsCursor().getCount());
    }

    @Test
    public void testInsertMedia() {
        long testId = Math.abs(mRandom.nextLong());
        MediaModel testMedia = UploadTestUtils.getTestMedia(testId);
        Assert.assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia));
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(UploadTestUtils.getTestSite(), testId);
        Assert.assertEquals(1, media.size());
        Assert.assertNotNull(media.get(0));

        // Store a MediaUploadModel corresponding to this MediaModel
        testMedia = media.get(0);
        MediaUploadModel mediaUploadModel = new MediaUploadModel(testMedia.getId());
        mediaUploadModel.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        Assert.assertNotNull(mediaUploadModel);
        Assert.assertEquals(testMedia.getId(), mediaUploadModel.getId());
        Assert.assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Update the stored MediaUploadModel, marking it as completed
        mediaUploadModel.setUploadState(MediaUploadModel.COMPLETED);
        mediaUploadModel.setProgress(1F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        Assert.assertNotNull(mediaUploadModel);
        Assert.assertEquals(testMedia.getId(), mediaUploadModel.getId());
        Assert.assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());

        // Deleting the MediaModel should cause the corresponding MediaUploadModel to be deleted also
        MediaSqlUtils.deleteMedia(testMedia);

        media = MediaSqlUtils.getSiteMediaWithId(UploadTestUtils.getTestSite(), testId);
        Assert.assertTrue(media.isEmpty());

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        Assert.assertNull(mediaUploadModel);
    }

    // Attempts to insert null then verifies there is no post
    @Test
    public void testInsertNullPost() {
        Assert.assertEquals(0, UploadSqlUtils.insertOrUpdatePost(null));
        Assert.assertEquals(0, WellSql.select(PostUploadModel.class).getAsCursor().getCount());
    }

    @Test
    public void testInsertPost() {
        PostModel testPost = UploadTestUtils.getTestPost();
        Assert.assertEquals(1, PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(testPost));
        List<PostModel> postList = PostTestUtils.getPosts();
        Assert.assertEquals(1, postList.size());
        Assert.assertNotNull(postList.get(0));

        // Store a PostUploadModel corresponding to this PostModel
        testPost = postList.get(0);
        PostUploadModel postUploadModel = new PostUploadModel(testPost.getId());
        UploadSqlUtils.insertOrUpdatePost(postUploadModel);

        postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(testPost.getId());
        Assert.assertNotNull(postUploadModel);
        Assert.assertEquals(testPost.getId(), postUploadModel.getId());
        Assert.assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());

        // Deleting the PostModel should cause the corresponding PostUploadModel to be deleted also
        PostSqlUtils.deletePost(testPost);

        postList = PostTestUtils.getPosts();
        Assert.assertTrue(postList.isEmpty());

        postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(testPost.getId());
        Assert.assertNull(postUploadModel);
    }
}
