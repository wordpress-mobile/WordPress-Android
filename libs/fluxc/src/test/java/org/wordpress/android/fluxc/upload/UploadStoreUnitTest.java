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
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.UploadStore;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
