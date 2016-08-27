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
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.wordpress.android.fluxc.media.MediaTestUtils.*;

@RunWith(RobolectricTestRunner.class)
public class MediaStoreTest {
    private final String TEST_TITLE = "Test Title";
    private final String TEST_DESCRIPTION = "Test Description";
    private final String TEST_CAPTION = "Test Caption";
    private final String TEST_ALT = "Test Alt";

    MediaStore mMediaStore = new MediaStore(new Dispatcher(),
            Mockito.mock(MediaRestClient.class), Mockito.mock(MediaXMLRPCClient.class));

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

    @Test
    public void testHasSiteMediaWithId() {
        final long testSiteId = 24;
        final long testMediaId = 22;
        assertTrue(mMediaStore.getSiteMediaCount(testSiteId) == 0);
        assertFalse(mMediaStore.hasSiteMediaWithId(testSiteId, testMediaId));

        // add test media
        MediaModel testMedia = generateMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
        testMedia.setBlogId(testSiteId);
        testMedia.setMediaId(testMediaId);
        assertTrue(insertMediaIntoDatabase(testMedia) == 0);

        // verify store has inserted media
        assertTrue(mMediaStore.getSiteMediaCount(testSiteId) == 1);
        assertTrue(mMediaStore.hasSiteMediaWithId(testSiteId, testMediaId));
    }

    @Test
    public void testGetSpecificSiteMedia() {
        final long testSiteId = 25;
        final long testMediaId = 11;
        assertFalse(mMediaStore.hasSiteMediaWithId(testSiteId, testMediaId));

        // add test media
        MediaModel testMedia = generateMedia(TEST_TITLE, TEST_DESCRIPTION, TEST_CAPTION, TEST_ALT);
        testMedia.setBlogId(testSiteId);
        testMedia.setMediaId(testMediaId);
        assertTrue(insertMediaIntoDatabase(testMedia) == 0);

        // cannot get media with incorrect site ID
        final long wrongSiteId = testSiteId + 1;
        assertNull(mMediaStore.getSiteMediaWithId(wrongSiteId, testMediaId));

        // verify stored media
        final MediaModel storeMedia = mMediaStore.getSiteMediaWithId(testSiteId, testMediaId);
        assertNotNull(storeMedia);
        assertEquals(testMedia, storeMedia);
    }

    @Test
    public void testGetListOfSiteMedia() {
        // insert list of media
        final int testListSize = 10;
        final long testSiteId = 55;
        List<MediaModel> insertedMedia = insertRandomMediaIntoDatabase(testSiteId, testListSize);
        assertTrue(mMediaStore.getSiteMediaCount(testSiteId) == testListSize);

        // create whitelist
        List<Long> whitelist = new ArrayList<>(testListSize / 2);
        for (int i = 0; i < testListSize; i += 2) {
            whitelist.add(insertedMedia.get(i).getMediaId());
        }

        final List<MediaModel> storeMedia = mMediaStore.getSiteMediaWithIds(testSiteId, whitelist);
        assertNotNull(storeMedia);
        assertTrue(storeMedia.size() == whitelist.size());
        for (MediaModel media : storeMedia) {
            assertTrue(whitelist.contains(media.getMediaId()));
        }
    }

    @Test
    public void testGetSiteImages() {
        final String testVideoPath = "/test/test_video.mp4";
        final String testImagePath = "/test/test_image.jpg";
        final long testSiteId = 55;
        final long testVideoId = 987;
        final long testImageId = 654;

        // insert media of different types
        MediaModel videoMedia = generateMediaFromPath(testSiteId, testVideoId, testVideoPath);
        assertTrue(MediaUtils.isVideoMimeType(videoMedia.getMimeType()));
        MediaModel imageMedia = generateMediaFromPath(testSiteId, testImageId, testImagePath);
        assertTrue(MediaUtils.isImageMimeType(imageMedia.getMimeType()));
        insertMediaIntoDatabase(videoMedia);
        insertMediaIntoDatabase(imageMedia);

        final List<MediaModel> storeImages = mMediaStore.getSiteImages(testSiteId);
        assertNotNull(storeImages);
        assertTrue(storeImages.size() == 1);
        assertEquals(testImageId, storeImages.get(0).getMediaId());
        assertTrue(MediaUtils.isImageMimeType(storeImages.get(0).getMimeType()));
    }

    @Test
    public void testGetSiteImageCount() {
        final long testSiteId = 9001;
        assertTrue(mMediaStore.getSiteImageCount(testSiteId) == 0);

        // insert both images and videos
        final int testListSize = 10;
        final List<MediaModel> testImages = new ArrayList<>(testListSize);
        final List<MediaModel> testVideos = new ArrayList<>(testListSize);
        final String testVideoPath = "/test/test_video%d.mp4";
        final String testImagePath = "/test/test_image%d.png";
        for (int i = 0; i < testListSize; ++i) {
            MediaModel testImage = generateMediaFromPath(testSiteId, i, String.format(testImagePath, i));
            MediaModel testVideo = generateMediaFromPath(testSiteId, i + testListSize, String.format(testVideoPath, i));
            assertTrue(insertMediaIntoDatabase(testImage) == 0);
            assertTrue(insertMediaIntoDatabase(testVideo) == 0);
            testImages.add(testImage);
            testVideos.add(testVideo);
        }

        assertTrue(mMediaStore.getSiteMediaCount(testSiteId) == testImages.size() + testVideos.size());
        assertTrue(mMediaStore.getSiteImageCount(testSiteId) == testImages.size());
    }

    @Test
    public void testGetSiteImagesBlacklist() {
        final long testSiteId = 3;
        assertTrue(mMediaStore.getSiteImageCount(testSiteId) == 0);

        final int testListSize = 10;
        final List<MediaModel> testImages = new ArrayList<>(testListSize);
        final String testImagePath = "/test/test_image%d.png";
        for (int i = 0; i < testListSize; ++i) {
            MediaModel image = generateMediaFromPath(testSiteId, i, String.format(testImagePath, i));
            assertTrue(insertMediaIntoDatabase(image) == 0);
            testImages.add(image);
        }
        assertTrue(mMediaStore.getSiteImageCount(testSiteId) == testListSize);

        // create blacklist
        List<Long> blacklist = new ArrayList<>(testListSize / 2);
        for (int i = 0; i < testListSize; i += 2) {
            blacklist.add(testImages.get(i).getMediaId());
        }

        final List<MediaModel> storeMedia = mMediaStore.getSiteImagesExcludingIds(testSiteId, blacklist);
        assertNotNull(storeMedia);
        assertEquals(testListSize - blacklist.size(), storeMedia.size());
        for (MediaModel media : storeMedia) {
            assertFalse(blacklist.contains(media.getMediaId()));
        }
    }

    @Test
    public void testGetUnattachedSiteMedia() {
    }

    @Test
    public void testGetUnattachedSiteMediaCount() {
    }

    @Test
    public void testGetLocalSiteMedia() {
    }

    @Test
    public void testGetUrlForVideoWithVideoPressGuid() {
        // insert video
        final long testSiteId = 13;
        final long testMediaId = 42;
        final String testVideoPath = "/test/test_video.mp4";
        final MediaModel testVideo = generateMediaFromPath(testSiteId, testMediaId, testVideoPath);
        final String testUrl = "http://notarealurl.testfluxc.org/not/a/real/resource/path.mp4";
        final String testVideoPressGuid = "thisisonlyatest";
        testVideo.setUrl(testUrl);
        testVideo.setVideoPressGuid(testVideoPressGuid);
        assertTrue(insertMediaIntoDatabase(testVideo) == 0);

        // retrieve video and verify
        final String storeUrl = mMediaStore.getUrlForSiteVideoWithVideoPressGuid(testSiteId, testVideoPressGuid);
        assertNotNull(storeUrl);
        assertEquals(testUrl, storeUrl);
    }

    @Test
    public void testGetThumbnailUrl() {
        // create and insert media with defined thumbnail URL
        final long testSiteId = 180;
        final long testMediaId = 360;
        final MediaModel testMedia = generateRandomizedMedia(testSiteId);
        final String testUrl = "http://notarealurl.testfluxc.org/not/a/real/resource/path.mp4";
        testMedia.setThumbnailUrl(testUrl);
        testMedia.setMediaId(testMediaId);
        assertTrue(insertMediaIntoDatabase(testMedia) == 0);

        // retrieve media and verify
        final String storeUrl = mMediaStore.getThumbnailUrlForSiteMediaWithId(testSiteId, testMediaId);
        assertNotNull(storeUrl);
        assertEquals(testUrl, storeUrl);
    }

    @Test
    public void testSearchSiteMediaTitles() {
        final long testSiteId = 628;
        final int testPoolSize = 10;
        final String[] testTitles = new String[testPoolSize];

        String baseString = "Base String";
        for (int i = 0; i < testPoolSize; ++i) {
            testTitles[i] = baseString;
            MediaModel testMedia = generateMedia(baseString, null, null, null);
            testMedia.setBlogId(testSiteId);
            testMedia.setMediaId(i);
            assertTrue(insertMediaIntoDatabase(testMedia) == 0);
            baseString += String.valueOf(i);
        }

        for (int i = 0; i < testPoolSize; ++i) {
            List<MediaModel> storeMedia = mMediaStore.searchSiteMediaByTitle(testSiteId, testTitles[i]);
            assertNotNull(storeMedia);
            assertTrue(storeMedia.size() == testPoolSize - i);
        }
    }

    @Test
    public void testGetPostMedia() {
    }

    @Test
    public void testGetNextSiteMediaToDelete() {
    }

    @Test
    public void testHasSiteMediaToDelete() {
    }
}
