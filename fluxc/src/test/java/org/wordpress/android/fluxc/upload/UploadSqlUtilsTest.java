package org.wordpress.android.fluxc.upload;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

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

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UploadSqlUtilsTest {
    private Random mRandom = new Random(System.currentTimeMillis());
    private PostSqlUtils mPostSqlUtils = new PostSqlUtils();

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
        assertEquals(0, UploadSqlUtils.insertOrUpdateMedia(null));
        assertEquals(0, WellSql.select(MediaUploadModel.class).getAsCursor().getCount());
    }

    @Test
    public void testInsertMedia() {
        long testId = Math.abs(mRandom.nextLong());
        MediaModel testMedia = UploadTestUtils.getTestMedia(testId);
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia));
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(UploadTestUtils.getTestSite(), testId);
        assertEquals(1, media.size());
        assertNotNull(media.get(0));

        // Store a MediaUploadModel corresponding to this MediaModel
        testMedia = media.get(0);
        MediaUploadModel mediaUploadModel = new MediaUploadModel(testMedia.getId());
        mediaUploadModel.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(testMedia.getId(), mediaUploadModel.getId());
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());

        // Update the stored MediaUploadModel, marking it as completed
        mediaUploadModel.setUploadState(MediaUploadModel.COMPLETED);
        mediaUploadModel.setProgress(1F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(testMedia.getId(), mediaUploadModel.getId());
        assertEquals(MediaUploadModel.COMPLETED, mediaUploadModel.getUploadState());

        // Deleting the MediaModel should cause the corresponding MediaUploadModel to be deleted also
        MediaSqlUtils.deleteMedia(testMedia);

        media = MediaSqlUtils.getSiteMediaWithId(UploadTestUtils.getTestSite(), testId);
        assertTrue(media.isEmpty());

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNull(mediaUploadModel);
    }

    @Test
    public void testUpdateMediaProgress() {
        long testId = Math.abs(mRandom.nextLong());
        MediaModel testMedia = UploadTestUtils.getTestMedia(testId);
        MediaSqlUtils.insertOrUpdateMedia(testMedia);
        testMedia = MediaSqlUtils.getSiteMediaWithId(UploadTestUtils.getTestSite(), testId).get(0);

        // Store a MediaUploadModel corresponding to this MediaModel
        MediaUploadModel mediaUploadModel = new MediaUploadModel(testMedia.getId());
        mediaUploadModel.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(0.65F, mediaUploadModel.getProgress());

        // Update the progress for the MediaUploadModel
        mediaUploadModel.setProgress(0.87F);
        assertEquals(1, UploadSqlUtils.updateMediaProgressOnly(mediaUploadModel));

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(testMedia.getId(), mediaUploadModel.getId());
        assertEquals(0.87F, mediaUploadModel.getProgress());

        // Attempting to update the progress for a MediaUploadModel that doesn't exist in the db should fail
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(mRandom.nextInt());
        mediaUploadModel2.setProgress(0.45F);
        assertEquals(0, UploadSqlUtils.updateMediaProgressOnly(mediaUploadModel2));
        assertNull(UploadSqlUtils.getMediaUploadModelForLocalId(mediaUploadModel2.getId()));
    }

    @Test
    public void testDeleteMediaUploadModel() {
        MediaModel testMedia1 = UploadTestUtils.getTestMedia(65);
        MediaModel testMedia2 = UploadTestUtils.getTestMedia(35);

        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia1));
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia2));
        List<MediaModel> mediaModels = MediaSqlUtils.getAllSiteMedia(UploadTestUtils.getTestSite());
        assertEquals(2, mediaModels.size());

        // Store MediaUploadModels corresponding to the MediaModels
        testMedia1 = mediaModels.get(0);
        MediaUploadModel mediaUploadModel1 = new MediaUploadModel(testMedia1.getId());
        mediaUploadModel1.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel1);

        testMedia2 = mediaModels.get(1);
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(testMedia2.getId());
        mediaUploadModel2.setProgress(0.35F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel2);

        // Delete one of the MediaUploadModels
        assertEquals(1, UploadSqlUtils.deleteMediaUploadModelWithLocalId(testMedia2.getId()));

        List<MediaUploadModel> mediaUploadModels = WellSql.select(MediaUploadModel.class).getAsModel();
        assertEquals(1, mediaUploadModels.size());
        assertEquals(testMedia1.getId(), mediaUploadModels.get(0).getId());

        // Delete the other MediaUploadModel
        Set<Integer> mediaIdSet = new HashSet<>();
        mediaIdSet.add(testMedia1.getId());
        assertEquals(1, UploadSqlUtils.deleteMediaUploadModelsWithLocalIds(mediaIdSet));

        mediaUploadModels = WellSql.select(MediaUploadModel.class).getAsModel();
        assertEquals(0, mediaUploadModels.size());

        // The corresponding MediaModels should be untouched
        mediaModels = MediaSqlUtils.getAllSiteMedia(UploadTestUtils.getTestSite());
        assertEquals(2, mediaModels.size());
    }

    // Attempts to insert null then verifies there is no post
    @Test
    public void testInsertNullPost() {
        assertEquals(0, UploadSqlUtils.insertOrUpdatePost(null));
        assertEquals(0, WellSql.select(PostUploadModel.class).getAsCursor().getCount());
    }

    @Test
    public void testInsertPost() {
        PostModel testPost = UploadTestUtils.getTestPost();
        assertEquals(1, mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(testPost));
        List<PostModel> postList = PostTestUtils.getPosts();
        assertEquals(1, postList.size());
        assertNotNull(postList.get(0));

        // Store a PostUploadModel corresponding to this PostModel
        testPost = postList.get(0);
        PostUploadModel postUploadModel = new PostUploadModel(testPost.getId());
        UploadSqlUtils.insertOrUpdatePost(postUploadModel);

        postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(testPost.getId());
        assertNotNull(postUploadModel);
        assertEquals(testPost.getId(), postUploadModel.getId());
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());

        // Deleting the PostModel should cause the corresponding PostUploadModel to be deleted also
        mPostSqlUtils.deletePost(testPost);

        postList = PostTestUtils.getPosts();
        assertTrue(postList.isEmpty());

        postUploadModel = UploadSqlUtils.getPostUploadModelForLocalId(testPost.getId());
        assertNull(postUploadModel);
    }

    @Test
    public void testGetPostModelsByState() {
        PostModel testPost1 = UploadTestUtils.getTestPost();
        testPost1.setIsLocalDraft(true);
        assertEquals(1, mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(testPost1));
        PostModel testPost2 = UploadTestUtils.getTestPost();
        testPost2.setIsLocalDraft(true);
        assertEquals(1, mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(testPost2));
        List<PostModel> postList = PostTestUtils.getPosts();
        assertEquals(2, postList.size());

        // Store PostUploadModels corresponding to the PostModels
        testPost1 = postList.get(0);
        PostUploadModel postUploadModel1 = new PostUploadModel(testPost1.getId());
        UploadSqlUtils.insertOrUpdatePost(postUploadModel1);
        testPost2 = postList.get(1);
        PostUploadModel postUploadModel2 = new PostUploadModel(testPost2.getId());
        UploadSqlUtils.insertOrUpdatePost(postUploadModel2);

        // Both PostUploadModels should be PENDING
        List<PostUploadModel> pendingPostUploadModels =
                UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.PENDING);
        assertEquals(2, pendingPostUploadModels.size());
        assertEquals(0, UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.FAILED).size());
        assertEquals(0, UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.CANCELLED).size());
        assertEquals(2, UploadSqlUtils.getAllPostUploadModels().size());

        // Fetch the corresponding PostModels
        List<PostModel> pendingPostModels = UploadSqlUtils.getPostModelsForPostUploadModels(pendingPostUploadModels);
        assertEquals(2, pendingPostModels.size());
        assertNotSame(pendingPostModels.get(0), pendingPostModels.get(1));

        // Set one PostUploadModel to CANCELLED
        postUploadModel1.setUploadState(PostUploadModel.CANCELLED);
        UploadSqlUtils.insertOrUpdatePost(postUploadModel1);

        pendingPostUploadModels = UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.PENDING);
        List<PostUploadModel> cancelledPostUploadModels =
                UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.CANCELLED);

        assertEquals(1, pendingPostUploadModels.size());
        assertEquals(1, cancelledPostUploadModels.size());
        assertEquals(0, UploadSqlUtils.getPostUploadModelsWithState(PostUploadModel.FAILED).size());
        assertEquals(2, UploadSqlUtils.getAllPostUploadModels().size());

        // Fetch the corresponding PostModels
        pendingPostModels = UploadSqlUtils.getPostModelsForPostUploadModels(pendingPostUploadModels);
        assertEquals(1, pendingPostModels.size());
        assertEquals(postUploadModel2.getId(), pendingPostModels.get(0).getId());
    }

    @Test
    public void testDeletePostUploadModel() {
        PostModel testPost1 = UploadTestUtils.getTestPost();
        testPost1.setIsLocalDraft(true);
        PostModel testPost2 = UploadTestUtils.getTestPost();
        testPost2.setIsLocalDraft(true);
        assertEquals(1, mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(testPost1));
        assertEquals(1, mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(testPost2));
        List<PostModel> postModels = PostTestUtils.getPosts();
        assertEquals(2, postModels.size());

        // Store PostUploadModels corresponding to the PostModels
        testPost1 = postModels.get(0);
        PostUploadModel postUploadModel1 = new PostUploadModel(testPost1.getId());
        UploadSqlUtils.insertOrUpdatePost(postUploadModel1);

        testPost2 = postModels.get(1);
        PostUploadModel postUploadModel2 = new PostUploadModel(testPost2.getId());
        UploadSqlUtils.insertOrUpdatePost(postUploadModel2);

        // Delete one of the PostUploadModels
        assertEquals(1, UploadSqlUtils.deletePostUploadModelWithLocalId(testPost2.getId()));

        List<PostUploadModel> postUploadModels = WellSql.select(PostUploadModel.class).getAsModel();
        assertEquals(1, postUploadModels.size());
        assertEquals(testPost1.getId(), postUploadModels.get(0).getId());

        // Delete the other PostUploadModel
        Set<Integer> postIdSet = new HashSet<>();
        postIdSet.add(testPost1.getId());
        assertEquals(1, UploadSqlUtils.deletePostUploadModelsWithLocalIds(postIdSet));

        postUploadModels = WellSql.select(PostUploadModel.class).getAsModel();
        assertEquals(0, postUploadModels.size());

        // The corresponding PostModels should be untouched
        postModels = PostTestUtils.getPosts();
        assertEquals(2, postModels.size());
    }

    @Test
    public void testGetMediaUploadModelsForPost() {
        // Check case where there are no matching MediaUploadModels for the post
        assertEquals(0, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());

        // Set up a MediaModel with a local post ID
        long testId = Math.abs(mRandom.nextLong());
        MediaModel testMedia = UploadTestUtils.getTestMedia(testId);
        testMedia.setLocalPostId(98);
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia));

        // Store a MediaUploadModel corresponding to the MediaModel
        MediaUploadModel mediaUploadModel = new MediaUploadModel(testMedia.getId());
        mediaUploadModel.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        // Test retrieving MediaUploadModels by post id
        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());

        // Set up a second MediaModel with a different post ID
        long testId2 = Math.abs(mRandom.nextLong());
        MediaModel testMedia2 = UploadTestUtils.getTestMedia(testId2);
        testMedia2.setLocalPostId(97);
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia2));

        // Results for the first post ID should be unchanged
        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());
        // Expect empty result since we haven't created a MediaUploadModel for this yet
        assertEquals(0, UploadSqlUtils.getMediaUploadModelsForPostId(97).size());

        // Store a MediaUploadModel corresponding to the second MediaModel
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(testMedia2.getId());
        mediaUploadModel2.setProgress(0.66F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel2);

        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());
        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(97).size());

        // Set up a third MediaModel, with the same post ID as the second
        long testId3 = Math.abs(mRandom.nextLong());
        MediaModel testMedia3 = UploadTestUtils.getTestMedia(testId3);
        testMedia3.setLocalPostId(97);
        assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia3));

        // Store a MediaUploadModel corresponding to the third MediaModel
        MediaUploadModel mediaUploadModel3 = new MediaUploadModel(testMedia3.getId());
        mediaUploadModel3.setProgress(0.67F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel3);

        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());
        assertEquals(2, UploadSqlUtils.getMediaUploadModelsForPostId(97).size());

        // Delete two MediaModels and verify the results
        MediaSqlUtils.deleteMedia(testMedia);
        MediaSqlUtils.deleteMedia(testMedia2);

        assertEquals(0, UploadSqlUtils.getMediaUploadModelsForPostId(98).size());
        assertEquals(1, UploadSqlUtils.getMediaUploadModelsForPostId(97).size());
    }
}
