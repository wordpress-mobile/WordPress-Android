package org.wordpress.android.fluxc.media;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.MediaStore;

import java.util.List;

import static org.junit.Assert.*;
import static org.wordpress.android.fluxc.media.MediaTestUtils.*;

@RunWith(RobolectricTestRunner.class)
public class MediaStoreTest {
    private MediaStore mMediaStore = new MediaStore(new Dispatcher(), Mockito.mock(MediaRestClient.class),
            Mockito.mock(MediaXMLRPCClient.class));

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(context, MediaModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testGetAllMedia() {
        final long testSiteId = 2;
        final List<MediaModel> testMedia = insertRandomMediaIntoDatabase(testSiteId, 5);

        // get all media via MediaStore
        List<MediaModel> storeMedia = mMediaStore.getAllSiteMedia(testSiteId);
        assertNotNull(storeMedia);
        assertEquals(testMedia.size(), storeMedia.size());

        // verify media
        for (MediaModel media : storeMedia) {
            assertEquals(testSiteId, media.getBlogId());
            assertTrue(testMedia.contains(media));
        }
    }

    @Test
    public void testMediaCount() {
        // initial count is 0
        final long testSiteId = 2;
        assertTrue(mMediaStore.getSiteMediaCount(testSiteId) == 0);

        // count after insertion
        insertRandomMediaIntoDatabase(testSiteId, 5);
        assertTrue(mMediaStore.getSiteMediaCount(testSiteId) == 5);

        // count after inserting with different site ID
        final long wrongSiteId = testSiteId + 1;
        assertTrue(mMediaStore.getSiteMediaCount(wrongSiteId) == 0);
        insertRandomMediaIntoDatabase(wrongSiteId, 1);
        assertTrue(mMediaStore.getSiteMediaCount(wrongSiteId) == 1);
        assertTrue(mMediaStore.getSiteMediaCount(testSiteId) == 5);
    }
}
