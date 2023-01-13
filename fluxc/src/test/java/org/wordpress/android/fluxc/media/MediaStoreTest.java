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
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration;
import org.wordpress.android.fluxc.network.rest.wpapi.media.ApplicationPasswordsMediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2.WPComV2MediaRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.wordpress.android.fluxc.media.MediaTestUtils.generateMedia;
import static org.wordpress.android.fluxc.media.MediaTestUtils.generateMediaFromPath;
import static org.wordpress.android.fluxc.media.MediaTestUtils.generateRandomizedMedia;
import static org.wordpress.android.fluxc.media.MediaTestUtils.generateRandomizedMediaList;
import static org.wordpress.android.fluxc.media.MediaTestUtils.insertMediaIntoDatabase;
import static org.wordpress.android.fluxc.media.MediaTestUtils.insertRandomMediaIntoDatabase;

@RunWith(RobolectricTestRunner.class)
public class MediaStoreTest {
    private MediaStore mMediaStore = new MediaStore(new Dispatcher(),
            Mockito.mock(MediaRestClient.class),
            Mockito.mock(MediaXMLRPCClient.class),
            Mockito.mock(WPComV2MediaRestClient.class),
            Mockito.mock(ApplicationPasswordsMediaRestClient.class),
            Mockito.mock(ApplicationPasswordsConfiguration.class));

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(context, MediaModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testGetAllMedia() {
        final int testSiteId = 2;
        final List<MediaModel> testMedia = insertRandomMediaIntoDatabase(testSiteId, 5);

        // get all media via MediaStore
        List<MediaModel> storeMedia = mMediaStore.getAllSiteMedia(getTestSiteWithLocalId(testSiteId));
        assertNotNull(storeMedia);
        assertEquals(testMedia.size(), storeMedia.size());

        // verify media
        for (MediaModel media : storeMedia) {
            assertEquals(testSiteId, media.getLocalSiteId());
            assertTrue(testMedia.contains(media));
        }
    }

    @Test
    public void testMediaCount() {
        final int testSiteId = 2;
        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        assertTrue(mMediaStore.getSiteMediaCount(testSite) == 0);

        // count after insertion
        insertRandomMediaIntoDatabase(testSiteId, 5);
        assertTrue(mMediaStore.getSiteMediaCount(testSite) == 5);

        // count after inserting with different site ID
        final int wrongSiteId = testSiteId + 1;
        SiteModel wrongSite = getTestSiteWithLocalId(wrongSiteId);
        assertTrue(mMediaStore.getSiteMediaCount(wrongSite) == 0);
        insertRandomMediaIntoDatabase(wrongSiteId, 1);
        assertTrue(mMediaStore.getSiteMediaCount(wrongSite) == 1);
        assertTrue(mMediaStore.getSiteMediaCount(testSite) == 5);
    }

    @Test
    public void testHasSiteMediaWithId() {
        final int testSiteId = 24;
        final long testMediaId = 22;
        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        assertTrue(mMediaStore.getSiteMediaCount(testSite) == 0);
        assertFalse(mMediaStore.hasSiteMediaWithId(testSite, testMediaId));

        // add test media
        MediaModel testMedia = getBasicMedia();
        testMedia.setLocalSiteId(testSiteId);
        testMedia.setMediaId(testMediaId);
        assertTrue(insertMediaIntoDatabase(testMedia) == 1);

        // verify store has inserted media
        assertTrue(mMediaStore.getSiteMediaCount(testSite) == 1);
        assertTrue(mMediaStore.hasSiteMediaWithId(testSite, testMediaId));
    }

    @Test
    public void testGetSpecificSiteMedia() {
        final int testSiteId = 25;
        final long testMediaId = 11;
        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        assertFalse(mMediaStore.hasSiteMediaWithId(testSite, testMediaId));

        // add test media
        MediaModel testMedia = getBasicMedia();
        testMedia.setLocalSiteId(testSiteId);
        testMedia.setMediaId(testMediaId);
        assertTrue(insertMediaIntoDatabase(testMedia) == 1);

        // cannot get media with incorrect site ID
        final int wrongSiteId = testSiteId + 1;
        SiteModel wrongSite = getTestSiteWithLocalId(wrongSiteId);
        assertNull(mMediaStore.getSiteMediaWithId(wrongSite, testMediaId));

        // verify stored media
        final MediaModel storeMedia = mMediaStore.getSiteMediaWithId(testSite, testMediaId);
        assertNotNull(storeMedia);
        assertEquals(testMedia, storeMedia);
    }

    @Test
    public void testGetListOfSiteMedia() {
        // insert list of media
        final int testListSize = 10;
        final int testSiteId = 55;
        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        List<MediaModel> insertedMedia = insertRandomMediaIntoDatabase(testSiteId, testListSize);
        assertTrue(mMediaStore.getSiteMediaCount(testSite) == testListSize);

        // create whitelist
        List<Long> whitelist = new ArrayList<>(testListSize / 2);
        for (int i = 0; i < testListSize; i += 2) {
            whitelist.add(insertedMedia.get(i).getMediaId());
        }

        final List<MediaModel> storeMedia = mMediaStore.getSiteMediaWithIds(testSite, whitelist);
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
        final int testSiteId = 55;
        final long testVideoId = 987;
        final long testImageId = 654;

        // insert media of different types
        MediaModel videoMedia = generateMediaFromPath(testSiteId, testVideoId, testVideoPath);
        assertTrue(MediaUtils.isVideoMimeType(videoMedia.getMimeType()));
        MediaModel imageMedia = generateMediaFromPath(testSiteId, testImageId, testImagePath);
        assertTrue(MediaUtils.isImageMimeType(imageMedia.getMimeType()));
        insertMediaIntoDatabase(videoMedia);
        insertMediaIntoDatabase(imageMedia);

        final List<MediaModel> storeImages = mMediaStore.getSiteImages(getTestSiteWithLocalId(testSiteId));
        assertNotNull(storeImages);
        assertTrue(storeImages.size() == 1);
        assertEquals(testImageId, storeImages.get(0).getMediaId());
        assertTrue(MediaUtils.isImageMimeType(storeImages.get(0).getMimeType()));
    }

    @Test
    public void testGetSiteImageCount() {
        final int testSiteId = 9001;
        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        assertTrue(mMediaStore.getSiteImageCount(testSite) == 0);

        // insert both images and videos
        final int testListSize = 10;
        final List<MediaModel> testImages = new ArrayList<>(testListSize);
        final List<MediaModel> testVideos = new ArrayList<>(testListSize);
        final String testVideoPath = "/test/test_video%d.mp4";
        final String testImagePath = "/test/test_image%d.png";
        for (int i = 0; i < testListSize; ++i) {
            MediaModel testImage = generateMediaFromPath(testSiteId, i, String.format(testImagePath, i));
            MediaModel testVideo = generateMediaFromPath(testSiteId, i + testListSize, String.format(testVideoPath, i));
            assertTrue(insertMediaIntoDatabase(testImage) == 1);
            assertTrue(insertMediaIntoDatabase(testVideo) == 1);
            testImages.add(testImage);
            testVideos.add(testVideo);
        }

        assertTrue(mMediaStore.getSiteMediaCount(testSite) == testImages.size() + testVideos.size());
        assertTrue(mMediaStore.getSiteImageCount(testSite) == testImages.size());
    }

    @Test
    public void testGetSiteImagesBlacklist() {
        final int testSiteId = 3;
        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        assertTrue(mMediaStore.getSiteImageCount(testSite) == 0);

        final int testListSize = 10;
        final List<MediaModel> testImages = new ArrayList<>(testListSize);
        final String testImagePath = "/test/test_image%d.png";
        for (int i = 0; i < testListSize; ++i) {
            MediaModel image = generateMediaFromPath(testSiteId, i, String.format(testImagePath, i));
            assertTrue(insertMediaIntoDatabase(image) == 1);
            testImages.add(image);
        }
        assertTrue(mMediaStore.getSiteImageCount(testSite) == testListSize);

        // create blacklist
        List<Long> blacklist = new ArrayList<>(testListSize / 2);
        for (int i = 0; i < testListSize; i += 2) {
            blacklist.add(testImages.get(i).getMediaId());
        }

        final List<MediaModel> storeMedia = mMediaStore.getSiteImagesExcludingIds(testSite, blacklist);
        assertNotNull(storeMedia);
        assertEquals(testListSize - blacklist.size(), storeMedia.size());
        for (MediaModel media : storeMedia) {
            assertFalse(blacklist.contains(media.getMediaId()));
        }
    }

    @Test
    public void testGetUnattachedSiteMedia() {
        final int testSiteId = 10001;
        final int testPoolSize = 10;
        final List<MediaModel> unattachedMedia = new ArrayList<>(testPoolSize);
        for (int i = 0; i < testPoolSize; ++i) {
            MediaModel attached = generateRandomizedMedia(testSiteId);
            MediaModel unattached = generateRandomizedMedia(testSiteId);
            attached.setMediaId(i);
            unattached.setMediaId(i + testPoolSize);
            attached.setPostId(i + testPoolSize);
            unattached.setPostId(0);
            insertMediaIntoDatabase(attached);
            insertMediaIntoDatabase(unattached);
            unattachedMedia.add(unattached);
        }

        final List<MediaModel> storeMedia = mMediaStore.getUnattachedSiteMedia(getTestSiteWithLocalId(testSiteId));
        assertNotNull(storeMedia);
        assertTrue(storeMedia.size() == unattachedMedia.size());
        for (int i = 0; i < storeMedia.size(); ++i) {
            assertTrue(storeMedia.contains(unattachedMedia.get(i)));
        }
    }

    @Test
    public void testGetUnattachedSiteMediaCount() {
        final int testSiteId = 10001;
        final int testPoolSize = 10;
        for (int i = 0; i < testPoolSize; ++i) {
            MediaModel attached = generateRandomizedMedia(testSiteId);
            MediaModel unattached = generateRandomizedMedia(testSiteId);
            attached.setMediaId(i);
            unattached.setMediaId(i + testPoolSize);
            attached.setPostId(i + testPoolSize);
            unattached.setPostId(0);
            insertMediaIntoDatabase(attached);
            insertMediaIntoDatabase(unattached);
        }
        assertTrue(mMediaStore.getUnattachedSiteMediaCount(getTestSiteWithLocalId(testSiteId)) == testPoolSize);
    }

    @Test
    public void testGetLocalSiteMedia() {
        final int testSiteId = 9;
        final long localMediaId = 2468;
        final long remoteMediaId = 1357;

        // add local media to site
        final MediaModel localMedia = getBasicMedia();
        localMedia.setLocalSiteId(testSiteId);
        localMedia.setMediaId(localMediaId);
        localMedia.setUploadState(MediaUploadState.UPLOADING);
        insertMediaIntoDatabase(localMedia);

        // add remote media
        final MediaModel remoteMedia = getBasicMedia();
        remoteMedia.setLocalSiteId(testSiteId);
        remoteMedia.setMediaId(remoteMediaId);
        // remote media has a defined upload date, simulated here
        remoteMedia.setUploadState(MediaUploadState.UPLOADED);
        insertMediaIntoDatabase(remoteMedia);

        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        assertEquals(2, mMediaStore.getSiteMediaCount(testSite));

        // verify local store media
        final List<MediaModel> localSiteMedia = mMediaStore.getLocalSiteMedia(testSite);
        assertNotNull(localSiteMedia);
        assertEquals(1, localSiteMedia.size());
        assertNotNull(localSiteMedia.get(0));
        assertEquals(localMediaId, localSiteMedia.get(0).getMediaId());

        // verify uploaded store media
        final List<MediaModel> uploadedSiteMedia = mMediaStore.getSiteMediaWithState(testSite,
                MediaUploadState.UPLOADED);
        assertNotNull(uploadedSiteMedia);
        assertEquals(1, uploadedSiteMedia.size());
        assertNotNull(uploadedSiteMedia.get(0));
        assertEquals(remoteMediaId, uploadedSiteMedia.get(0).getMediaId());
    }

    @Test
    public void testGetUrlForVideoWithVideoPressGuid() {
        // insert video
        final int testSiteId = 13;
        final long testMediaId = 42;
        final String testVideoPath = "/test/test_video.mp4";
        final MediaModel testVideo = generateMediaFromPath(testSiteId, testMediaId, testVideoPath);
        final String testUrl = "http://notarealurl.testfluxc.org/not/a/real/resource/path.mp4";
        final String testVideoPressGuid = "thisisonlyatest";
        testVideo.setUrl(testUrl);
        testVideo.setVideoPressGuid(testVideoPressGuid);
        assertTrue(insertMediaIntoDatabase(testVideo) == 1);

        // retrieve video and verify
        final String storeUrl = mMediaStore
                .getUrlForSiteVideoWithVideoPressGuid(getTestSiteWithLocalId(testSiteId), testVideoPressGuid);
        assertNotNull(storeUrl);
        assertEquals(testUrl, storeUrl);
    }

    @Test
    public void testGetThumbnailUrl() {
        // create and insert media with defined thumbnail URL
        final int testSiteId = 180;
        final long testMediaId = 360;
        final MediaModel testMedia = generateRandomizedMedia(testSiteId);
        final String testUrl = "http://notarealurl.testfluxc.org/not/a/real/resource/path.mp4";
        testMedia.setThumbnailUrl(testUrl);
        testMedia.setMediaId(testMediaId);
        assertTrue(insertMediaIntoDatabase(testMedia) == 1);

        // retrieve media and verify
        final String storeUrl = mMediaStore
                .getThumbnailUrlForSiteMediaWithId(getTestSiteWithLocalId(testSiteId), testMediaId);
        assertNotNull(storeUrl);
        assertEquals(testUrl, storeUrl);
    }

    @Test
    public void testSearchSiteMediaTitles() {
        final int testSiteId = 628;
        final int testPoolSize = 10;
        final String[] testTitles = new String[testPoolSize];

        String baseString = "Base String";
        for (int i = 0; i < testPoolSize; ++i) {
            testTitles[i] = baseString;
            MediaModel testMedia = generateMedia(baseString, null, null, null);
            testMedia.setLocalSiteId(testSiteId);
            testMedia.setMediaId(i);
            assertTrue(insertMediaIntoDatabase(testMedia) == 1);
            baseString += String.valueOf(i);
        }

        for (int i = 0; i < testPoolSize; ++i) {
            List<MediaModel> storeMedia = mMediaStore
                    .searchSiteMedia(getTestSiteWithLocalId(testSiteId), testTitles[i]);
            assertNotNull(storeMedia);
            assertTrue(storeMedia.size() == testPoolSize - i);
        }
    }

    @Test
    public void testSearchSiteImages() {
        final String testImagePath = "/test/test_image.jpg";
        final String testVideoPath = "/test/test_video.mp4";
        final String testAudioPath = "/test/test_audio.mp3";

        final int testSiteId = 55;
        final long testImageId = 654;
        final long testVideoId = 987;
        final long testAudioId = 540;

        // generate media of different types
        MediaModel imageMedia = generateMediaFromPath(testSiteId, testImageId, testImagePath);
        imageMedia.setTitle("Awesome Image");
        imageMedia.setDescription("This is an image test");
        assertTrue(MediaUtils.isImageMimeType(imageMedia.getMimeType()));

        MediaModel videoMedia = generateMediaFromPath(testSiteId, testVideoId, testVideoPath);
        videoMedia.setTitle("Video Title");
        videoMedia.setCaption("Test Caption");
        assertTrue(MediaUtils.isVideoMimeType(videoMedia.getMimeType()));

        MediaModel audioMedia = generateMediaFromPath(testSiteId, testAudioId, testAudioPath);
        audioMedia.setDescription("This is an audio test");
        assertTrue(MediaUtils.isAudioMimeType(audioMedia.getMimeType()));

        // insert media of different types
        insertMediaIntoDatabase(videoMedia);
        insertMediaIntoDatabase(imageMedia);
        insertMediaIntoDatabase(audioMedia);

        // verify the correct media is returned
        final List<MediaModel> storeImages = mMediaStore
                .searchSiteImages(getTestSiteWithLocalId(testSiteId), "test");

        assertNotNull(storeImages);
        assertTrue(storeImages.size() == 1);
        assertEquals(testImageId, storeImages.get(0).getMediaId());
        assertTrue(MediaUtils.isImageMimeType(storeImages.get(0).getMimeType()));
        assertEquals(testSiteId, storeImages.get(0).getLocalSiteId());
    }

    @Test
    public void testSearchSiteVideos() {
        final String testVideoPath1 = "/test/video_1.mp4";
        final String testVideoPath2 = "/test/video_2.mp4";
        final String testDocumentPath = "/test/test_document.pdf";

        final int testSiteId = 423;
        final long testVideoId1 = 675;
        final long testVideoId2 = 1432;
        final long testDocumentId = 125;

        // generate media of different types
        MediaModel videoMedia1 = generateMediaFromPath(testSiteId, testVideoId1, testVideoPath1);
        videoMedia1.setTitle("My trip title");
        assertTrue(MediaUtils.isVideoMimeType(videoMedia1.getMimeType()));

        MediaModel videoMedia2 = generateMediaFromPath(testSiteId, testVideoId2, testVideoPath2);
        videoMedia2.setTitle("Test video title");
        assertTrue(MediaUtils.isVideoMimeType(videoMedia2.getMimeType()));

        MediaModel documentMedia = generateMediaFromPath(testSiteId, testDocumentId, testDocumentPath);
        documentMedia.setTitle("My first test");
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia.getMimeType()));

        // insert media of different types
        insertMediaIntoDatabase(videoMedia1);
        insertMediaIntoDatabase(videoMedia2);
        insertMediaIntoDatabase(documentMedia);

        // verify the correct media is returned
        final List<MediaModel> storeVideos = mMediaStore
                .searchSiteVideos(getTestSiteWithLocalId(testSiteId), "test");
        assertNotNull(storeVideos);
        assertTrue(storeVideos.size() == 1);
        assertEquals(testVideoId2, storeVideos.get(0).getMediaId());
        assertTrue(MediaUtils.isVideoMimeType(storeVideos.get(0).getMimeType()));
        assertEquals(testSiteId, storeVideos.get(0).getLocalSiteId());
    }

    @Test
    public void testSearchSiteAudio() {
        final String testImagePath = "/test/test_image.jpg";
        final String testAudioPath1 = "/test/my_audio.mp3";
        final String testAudioPath2 = "/test/awesome_2018.mp3";
        final String testDocumentPath = "/test/test_document.pdf";

        final int testSiteId = 8765;
        final long testImageId = 34;
        final long testAudioId1 = 100;
        final long testAudioId2 = 99;
        final long testDocumentId = 43;

        // generate media of different types
        MediaModel imageMedia = generateMediaFromPath(testSiteId, testImageId, testImagePath);
        imageMedia.setTitle("Title test");
        assertTrue(MediaUtils.isImageMimeType(imageMedia.getMimeType()));

        MediaModel audioMedia1 = generateMediaFromPath(testSiteId, testAudioId1, testAudioPath1);
        audioMedia1.setTitle("The big one");
        audioMedia1.setDescription("Test for the World");
        assertTrue(MediaUtils.isAudioMimeType(audioMedia1.getMimeType()));

        MediaModel audioMedia2 = generateMediaFromPath(testSiteId, testAudioId2, testAudioPath2);
        audioMedia2.setTitle("The test!");
        audioMedia2.setDescription("Without description");
        assertTrue(MediaUtils.isAudioMimeType(audioMedia2.getMimeType()));

        MediaModel documentMedia = generateMediaFromPath(testSiteId, testDocumentId, testDocumentPath);
        documentMedia.setTitle("Document with every test of the app");
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia.getMimeType()));

        // insert media of different types
        insertMediaIntoDatabase(imageMedia);
        insertMediaIntoDatabase(audioMedia1);
        insertMediaIntoDatabase(audioMedia2);
        insertMediaIntoDatabase(documentMedia);

        // verify the correct media is returned (just audio)
        final List<MediaModel> storeAudio = mMediaStore
                .searchSiteAudio(getTestSiteWithLocalId(testSiteId), "test");
        assertNotNull(storeAudio);
        assertTrue(storeAudio.size() == 2);
        assertEquals(testAudioId1, storeAudio.get(0).getMediaId());
        assertEquals(testAudioId2, storeAudio.get(1).getMediaId());

        assertTrue(MediaUtils.isAudioMimeType(storeAudio.get(0).getMimeType()));
        assertTrue(MediaUtils.isAudioMimeType(storeAudio.get(1).getMimeType()));

        assertEquals(testSiteId, storeAudio.get(0).getLocalSiteId());
        assertEquals(testSiteId, storeAudio.get(1).getLocalSiteId());
    }

    @Test
    public void testSearchSiteDocuments() {
        final String testAudioPath = "/test/test_audio.mp3";
        final String testDocumentPath1 = "/test/document.pdf";
        final String testDocumentPath2 = "/test/document.doc";
        final String testDocumentPath3 = "/test/document.xls";
        final String testDocumentPath4 = "/test/document.pps";

        final int testSiteId = 865234;
        final long testAudioId = 78;
        final long testDocumentId1 = 234;
        final long testDocumentId2 = 657;
        final long testDocumentId3 = 98;
        final long testDocumentId4 = 543;

        // generate media of different types
        MediaModel audioMedia = generateMediaFromPath(testSiteId, testAudioId, testAudioPath);
        audioMedia.setTitle("My first test");
        audioMedia.setDescription("This is a description test");
        audioMedia.setCaption("Caption test");
        assertTrue(MediaUtils.isAudioMimeType(audioMedia.getMimeType()));

        MediaModel documentMedia1 = generateMediaFromPath(testSiteId, testDocumentId1, testDocumentPath1);
        documentMedia1.setTitle("The Document");
        documentMedia1.setDescription("short description");
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia1.getMimeType()));

        MediaModel documentMedia2 = generateMediaFromPath(testSiteId, testDocumentId2, testDocumentPath2);
        documentMedia2.setTitle("Document to Test");
        documentMedia2.setDescription("medium description");
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia2.getMimeType()));

        MediaModel documentMedia3 = generateMediaFromPath(testSiteId, testDocumentId3, testDocumentPath3);
        documentMedia3.setTitle("Document");
        documentMedia3.setDescription("Large description with a test");
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia3.getMimeType()));

        MediaModel documentMedia4 = generateMediaFromPath(testSiteId, testDocumentId4, testDocumentPath4);
        documentMedia4.setTitle("Document Title");
        documentMedia4.setDescription("description");
        assertTrue(MediaUtils.isApplicationMimeType(documentMedia4.getMimeType()));

        // insert media of different types
        insertMediaIntoDatabase(audioMedia);
        insertMediaIntoDatabase(documentMedia1);
        insertMediaIntoDatabase(documentMedia2);
        insertMediaIntoDatabase(documentMedia3);
        insertMediaIntoDatabase(documentMedia4);

        // verify the correct media is returned (just documents)
        final List<MediaModel> storeDocuments = mMediaStore
                .searchSiteDocuments(getTestSiteWithLocalId(testSiteId), "test");
        assertNotNull(storeDocuments);
        assertTrue(storeDocuments.size() == 2);
        assertEquals(testDocumentId2, storeDocuments.get(0).getMediaId());
        assertEquals(testDocumentId3, storeDocuments.get(1).getMediaId());

        assertTrue(MediaUtils.isApplicationMimeType(storeDocuments.get(0).getMimeType()));
        assertTrue(MediaUtils.isApplicationMimeType(storeDocuments.get(1).getMimeType()));

        assertEquals(testSiteId, storeDocuments.get(0).getLocalSiteId());
        assertEquals(testSiteId, storeDocuments.get(1).getLocalSiteId());
    }

    @Test
    public void testGetPostMedia() {
        final int testSiteId = 11235813;
        final int testLocalPostId = 213253;
        final long postMediaId = 13;
        final long unattachedMediaId = 57;
        final long otherMediaId = 911;
        final String testPath = "this/is/only/a/test.png";

        // add post media with test path
        final MediaModel postMedia = getBasicMedia();
        postMedia.setLocalSiteId(testSiteId);
        postMedia.setLocalPostId(testLocalPostId);
        postMedia.setMediaId(postMediaId);
        postMedia.setFilePath(testPath);
        insertMediaIntoDatabase(postMedia);

        // add unattached media with test path
        final MediaModel unattachedMedia = getBasicMedia();
        unattachedMedia.setLocalSiteId(testSiteId);
        unattachedMedia.setLocalPostId(testLocalPostId);
        unattachedMedia.setFilePath(testPath);
        unattachedMedia.setMediaId(unattachedMediaId);
        insertMediaIntoDatabase(unattachedMedia);

        // add post media with different file path
        final MediaModel otherPathMedia = getBasicMedia();
        otherPathMedia.setLocalSiteId(testSiteId);
        otherPathMedia.setLocalPostId(testLocalPostId);
        otherPathMedia.setMediaId(otherMediaId);
        otherPathMedia.setFilePath("appended/" + testPath);
        insertMediaIntoDatabase(otherPathMedia);

        // verify the correct media is in the store
        PostModel post = new PostModel();
        post.setId(testLocalPostId);
        final MediaModel storeMedia = mMediaStore.getMediaForPostWithPath(post, testPath);
        assertNotNull(storeMedia);
        assertEquals(testPath, storeMedia.getFilePath());
        assertEquals(postMediaId, storeMedia.getMediaId());
        assertEquals(3, mMediaStore.getSiteMediaCount(getTestSiteWithLocalId(testSiteId)));

        // verify the correct media is in the store
        List<MediaModel> mediaModelList = mMediaStore.getMediaForPost(post);
        assertNotNull(mediaModelList);
        assertEquals(3, mediaModelList.size());
        for (MediaModel media : mediaModelList) {
            assertNotNull(media);
            assertEquals(post.getId(), media.getLocalPostId());
        }
    }

    @Test
    public void testGetNextSiteMediaToDelete() {
        final int testSiteId = 30984;
        final int count = 10;

        // add media with varying upload states
        final List<MediaModel> pendingDelete = generateRandomizedMediaList(count, testSiteId);
        final List<MediaModel> other = generateRandomizedMediaList(count, testSiteId);
        for (int i = 0; i < count; ++i) {
            pendingDelete.get(i).setUploadState(MediaUploadState.DELETING);
            pendingDelete.get(i).setMediaId(i + (count * 2));
            other.get(i).setUploadState(MediaUploadState.UPLOADED);
            other.get(i).setMediaId(i + count);
            insertMediaIntoDatabase(pendingDelete.get(i));
            insertMediaIntoDatabase(other.get(i));
        }

        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        assertEquals(count * 2, mMediaStore.getSiteMediaCount(testSite));

        // verify store media updates as media is deleted
        for (int i = 0; i < count; ++i) {
            MediaModel next = mMediaStore.getNextSiteMediaToDelete(testSite);
            assertNotNull(next);
            assertEquals(MediaUploadState.DELETING, MediaUploadState.fromString(next.getUploadState()));
            assertTrue(pendingDelete.contains(next));
            MediaSqlUtils.deleteMedia(next);
            assertEquals(count * 2 - i - 1, mMediaStore.getSiteMediaCount(testSite));
            pendingDelete.remove(next);
        }
    }

    @Test
    public void testHasSiteMediaToDelete() {
        final int testSiteId = 30984;
        final int count = 10;

        // add media with varying upload states
        final List<MediaModel> pendingDelete = generateRandomizedMediaList(count, testSiteId);
        final List<MediaModel> other = generateRandomizedMediaList(count, testSiteId);
        for (int i = 0; i < count; ++i) {
            pendingDelete.get(i).setUploadState(MediaUploadState.DELETING);
            pendingDelete.get(i).setMediaId(i + (count * 2));
            other.get(i).setUploadState(MediaUploadState.DELETED);
            other.get(i).setMediaId(i + count);
            insertMediaIntoDatabase(pendingDelete.get(i));
            insertMediaIntoDatabase(other.get(i));
        }

        SiteModel testSite = getTestSiteWithLocalId(testSiteId);
        assertEquals(count * 2, mMediaStore.getSiteMediaCount(testSite));

        // verify store still has media to delete after deleting one
        assertTrue(mMediaStore.hasSiteMediaToDelete(testSite));
        MediaModel next = mMediaStore.getNextSiteMediaToDelete(testSite);
        assertNotNull(next);
        assertTrue(pendingDelete.contains(next));
        MediaSqlUtils.deleteMedia(next);
        pendingDelete.remove(next);
        assertEquals(count * 2 - 1, mMediaStore.getSiteMediaCount(testSite));
        assertTrue(mMediaStore.hasSiteMediaToDelete(testSite));

        // verify store has no media to delete after removing all
        for (MediaModel pending : pendingDelete) {
            MediaSqlUtils.deleteMedia(pending);
        }
        assertEquals(count, mMediaStore.getSiteMediaCount(testSite));
        assertFalse(mMediaStore.hasSiteMediaToDelete(testSite));
    }

    @Test
    public void testRemoveAllMedia() {
        SiteModel testSite1 = getTestSiteWithLocalId(1);
        insertRandomMediaIntoDatabase(testSite1.getId(), 5);
        assertTrue(mMediaStore.getSiteMediaCount(testSite1) == 5);

        SiteModel testSite2 = getTestSiteWithLocalId(2);
        insertRandomMediaIntoDatabase(testSite2.getId(), 7);
        assertTrue(mMediaStore.getSiteMediaCount(testSite2) == 7);

        MediaSqlUtils.deleteAllMedia();

        assertTrue(mMediaStore.getSiteMediaCount(testSite1) == 0);
        assertTrue(mMediaStore.getSiteMediaCount(testSite2) == 0);
    }

    private MediaModel getBasicMedia() {
        return generateMedia("Test Title", "Test Description", "Test Caption", "Test Alt");
    }

    private SiteModel getTestSiteWithLocalId(int localSiteId) {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(localSiteId);
        return siteModel;
    }
}
