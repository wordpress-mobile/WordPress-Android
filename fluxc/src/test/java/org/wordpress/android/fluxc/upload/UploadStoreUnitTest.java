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
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
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
    private PostSqlUtils mPostSqlUtils = new PostSqlUtils();
    private PostStore mPostStore = new PostStore(mDispatcher, Mockito.mock(PostRestClient.class),
            Mockito.mock(PostXMLRPCClient.class), mPostSqlUtils);

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
        mediaUploadModel = UploadTestUtils.getMediaUploadModelForMediaModel(testMedia);
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
        mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel);
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
        PostUploadModel postUploadModel = UploadTestUtils.getPostUploadModelForPostModel(postModel);
        assertNotNull(postUploadModel);
        assertEquals(2, postUploadModel.getAssociatedMediaIdSet().size());
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(5));
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(6));
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isPendingPost(postModel));

        // Register the same post again with media changes
        MediaModel media3 = UploadTestUtils.getLocalTestMedia();
        media3.setId(8);
        // Remove one media and add a new one
        associatedMedia.clear();
        associatedMedia.add(media1);
        associatedMedia.add(media3);
        mUploadStore.registerPostModel(postModel, associatedMedia);

        // Expect the updated model to have both the original media and the new one
        postUploadModel = UploadTestUtils.getPostUploadModelForPostModel(postModel);
        assertNotNull(postUploadModel);
        assertEquals(3, postUploadModel.getAssociatedMediaIdSet().size());
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(5));
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(6));
        assertTrue(postUploadModel.getAssociatedMediaIdSet().contains(8));
        assertEquals(PostUploadModel.PENDING, postUploadModel.getUploadState());
        assertTrue(mUploadStore.isPendingPost(postModel));
    }

    @Test
    public void testGetUploadErrorForPost() {
        // Create a PostModel and add it to the PostStore
        PostModel postModel = UploadTestUtils.getTestPost();
        postModel.setId(55);
        mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel);
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
        PostUploadModel postUploadModel = UploadTestUtils.getPostUploadModelForPostModel(postModel);
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
        MediaUploadModel mediaUploadModel1 = UploadTestUtils.getMediaUploadModelForMediaModel(media1);
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

        // Create another PostModel and add it to the PostStore - this time, without registering it with the UploadStore
        PostModel unregisteredPostModel = UploadTestUtils.getTestPost();
        unregisteredPostModel.setId(55);
        mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(unregisteredPostModel);

        // Create a MediaModel attached to the above post
        MediaModel mediaLinkedToPost = UploadTestUtils.getLocalTestMedia();
        mediaLinkedToPost.setId(7);
        mediaLinkedToPost.setLocalPostId(unregisteredPostModel.getId());
        MediaSqlUtils.insertMediaForResult(mediaLinkedToPost);
        UploadSqlUtils.insertOrUpdateMedia(new MediaUploadModel(mediaLinkedToPost.getId()));
        MediaUploadModel linkedMediaUploadModel = UploadTestUtils.getMediaUploadModelForMediaModel(mediaLinkedToPost);

        // Add an error to the MediaUploadModel
        linkedMediaUploadModel.setMediaError(new MediaError(MediaErrorType.EXCEEDS_MEMORY_LIMIT, "Too large!"));
        UploadSqlUtils.insertOrUpdateMedia(linkedMediaUploadModel);

        // Confirm that the store returns a media error for the post (even though there's no associated PostUploadModel)
        uploadError = mUploadStore.getUploadErrorForPost(unregisteredPostModel);
        assertNotNull(uploadError);
        assertNull(uploadError.postError);
        assertNotNull(uploadError.mediaError);
        assertEquals(MediaErrorType.EXCEEDS_MEMORY_LIMIT, uploadError.mediaError.type);
    }

    @Test
    public void testNumberOfAutoUploadsAttemptsCounter() {
        // Create a PostModel and add it to the PostStore
        PostModel postModel = UploadTestUtils.getTestPost();
        postModel.setId(55);
        mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel);
        postModel = mPostStore.getPostByLocalPostId(postModel.getId());
        assertNotNull(postModel);

        // Register the PostModel with the UploadStore
        mUploadStore.registerPostModel(postModel, new ArrayList<MediaModel>());

        assertEquals(0, UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId())
                                      .getNumberOfAutoUploadAttempts());

        mUploadStore.onAction(UploadActionBuilder.newIncrementNumberOfAutoUploadAttemptsAction(postModel));

        assertEquals(1, UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId())
                                      .getNumberOfAutoUploadAttempts());

        mUploadStore.onAction(UploadActionBuilder.newIncrementNumberOfAutoUploadAttemptsAction(postModel));

        assertEquals(2, UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId())
                                      .getNumberOfAutoUploadAttempts());
    }
}
