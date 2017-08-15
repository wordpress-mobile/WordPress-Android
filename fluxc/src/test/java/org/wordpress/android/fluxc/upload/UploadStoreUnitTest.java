package org.wordpress.android.fluxc.upload;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.store.UploadStore.UploadError;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UploadStoreUnitTest {
    private Dispatcher mDispatcher = new Dispatcher();
    private UploadStore mUploadStore = new UploadStore(mDispatcher);
    private PostStore mPostStore = new PostStore(mDispatcher, Mockito.mock(PostRestClient.class),
            Mockito.mock(PostXMLRPCClient.class), mUploadStore);

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testMediaUploadProgress() {
        // Create a MediaModel and add it to both the MediaModelTable and the MediaUploadTable
        // (simulating an upload action)
        MediaModel testMedia = UploadTestUtils.getLocalTestMedia();
        testMedia.setId(5);
        MediaSqlUtils.insertMediaForResult(testMedia);

        MediaUploadModel mediaUploadModel = new MediaUploadModel(testMedia.getId());
        mediaUploadModel.setProgress(0.65F);
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);

        // Check that the stored MediaUploadModel has the right state
        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        assertNotNull(mediaUploadModel);
        assertEquals(testMedia.getId(), mediaUploadModel.getId());
        assertEquals(MediaUploadModel.UPLOADING, mediaUploadModel.getUploadState());
        assertEquals(0.65F, mUploadStore.getUploadProgressForMedia(testMedia), 0.1F);
    }

    @Test
    public void testPostModelRegistration() {
        // Create a PostModel and add it to the PostStore
        PostModel postModel = UploadTestUtils.getTestPost();
        postModel.setId(55);
        PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel);
        postModel = mPostStore.getPostByLocalPostId(postModel.getId());
        assertNotNull(postModel);

        // Register the PostModel with the UploadStore, creating a PostUploadModel with some associated media
        List<MediaModel> associatedMedia = new ArrayList<>();
        MediaModel media1 = UploadTestUtils.getLocalTestMedia();
        media1.setId(5);
        MediaModel media2 = UploadTestUtils.getLocalTestMedia();
        media2.setId(6);
        associatedMedia.add(media1);
        associatedMedia.add(media2);
        mUploadStore.registerPostModel(postModel, associatedMedia);

        // Confirm that the PostUploadModel has been created and has the expected status
        PostUploadModel postUploadModel = mUploadStore.getPostUploadModelForPostModel(postModel);
        assertNotNull(postUploadModel);
        assertEquals(2, postUploadModel.getAssociatedMediaIdSet().size());
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(5));
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(6));
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());

        // Register the same post again with media changes
        MediaModel media3 = UploadTestUtils.getLocalTestMedia();
        media3.setId(8);
        // Remove one media and add a new one
        associatedMedia.clear();
        associatedMedia.add(media1);
        associatedMedia.add(media3);
        mUploadStore.registerPostModel(postModel, associatedMedia);

        // Expect the updated model to have both the original media and the new one
        postUploadModel = mUploadStore.getPostUploadModelForPostModel(postModel);
        assertNotNull(postUploadModel);
        assertEquals(3, postUploadModel.getAssociatedMediaIdSet().size());
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(5));
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(6));
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(8));
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
    }

    @Test
    public void testGetUploadErrorForPost() {
        // Create a PostModel and add it to the PostStore
        PostModel postModel = UploadTestUtils.getTestPost();
        postModel.setId(55);
        PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel);
        postModel = mPostStore.getPostByLocalPostId(postModel.getId());
        assertNotNull(postModel);

        // Create some MediaModels and add them to both the MediaModelTable and the MediaUploadTable
        // (simulating an upload action)
        MediaModel media1 = UploadTestUtils.getLocalTestMedia();
        media1.setId(5);
        MediaSqlUtils.insertMediaForResult(media1);
        UploadSqlUtils.insertOrUpdateMedia(new MediaUploadModel(media1.getId()));
        MediaModel media2 = UploadTestUtils.getLocalTestMedia();
        media2.setId(6);
        MediaSqlUtils.insertMediaForResult(media2);
        UploadSqlUtils.insertOrUpdateMedia(new MediaUploadModel(media2.getId()));

        // Register the PostModel with the UploadStore, creating a PostUploadModel with some associated media
        List<MediaModel> associatedMedia = new ArrayList<>();
        associatedMedia.add(media1);
        associatedMedia.add(media2);
        mUploadStore.registerPostModel(postModel, associatedMedia);

        // Confirm that the PostUploadModel has been created and has a null error state
        PostUploadModel postUploadModel = mUploadStore.getPostUploadModelForPostModel(postModel);
        assertNotNull(postUploadModel);
        assertNull(mUploadStore.getUploadErrorForPost(postModel));

        // Add an error to this PostUploadModel
        postUploadModel.setPostError(new PostError(PostErrorType.UNKNOWN_POST, "Unknown!"));
        UploadSqlUtils.insertOrUpdatePost(postUploadModel);

        // Confirm that the store represents the post error correctly
        UploadError uploadError = mUploadStore.getUploadErrorForPost(postModel);
        assertNotNull(uploadError);
        assertNull(uploadError.mediaError);
        assertNotNull(uploadError.postError);
        assertEquals(PostErrorType.UNKNOWN_POST, uploadError.postError.type);

        // Null out the post error again
        postUploadModel.setPostError(null);
        UploadSqlUtils.insertOrUpdatePost(postUploadModel);
        assertNull(mUploadStore.getUploadErrorForPost(postModel));

        // Confirm that the MediaUploadModel's default state is error-free
        MediaUploadModel mediaUploadModel1 = mUploadStore.getMediaUploadModelForMediaModel(media1);
        assertNull(mediaUploadModel1.getMediaError());

        // Add an error to the first MediaUploadModel
        mediaUploadModel1.setMediaError(new MediaError(MediaErrorType.EXCEEDS_MEMORY_LIMIT, "Too large!"));
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel1);

        // Confirm that the store now returns a media error for the post, since it has an associated media item
        // with an error
        uploadError = mUploadStore.getUploadErrorForPost(postModel);
        assertNotNull(uploadError);
        assertNull(uploadError.postError);
        assertNotNull(uploadError.mediaError);
        assertEquals(MediaErrorType.EXCEEDS_MEMORY_LIMIT, uploadError.mediaError.type);
    }
}
