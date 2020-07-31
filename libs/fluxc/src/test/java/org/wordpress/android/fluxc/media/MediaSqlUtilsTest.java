package org.wordpress.android.fluxc.media;

import android.content.Context;
import android.database.Cursor;

import com.wellsql.generated.MediaModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.utils.MimeType.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wordpress.android.fluxc.store.MediaStore.NOT_DELETED_STATES;

@RunWith(RobolectricTestRunner.class)
public class MediaSqlUtilsTest {
    private static final int TEST_LOCAL_SITE_ID = 42;
    private static final int SMALL_TEST_POOL = 10;

    private Random mRandom = new Random(System.currentTimeMillis());

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, MediaModel.class);
        WellSql.init(config);
        config.reset();
    }

    // Attempts to insert null then verifies there is no media
    @Test
    public void testInsertNullMedia() {
        assertThat(0).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(null));
        assertThat(MediaSqlUtils.getAllSiteMedia(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID))).isEmpty();
    }

    // Inserts a media item with various known fields then retrieves and validates those fields
    @Test
    public void testInsertMedia() {
        long testId = Math.abs(mRandom.nextLong());
        String testTitle = getTestString();
        String testDescription = getTestString();
        String testCaption = getTestString();
        MediaModel testMedia = getTestMedia(testId, testTitle, testDescription, testCaption);
        assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(testMedia));
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), testId);
        assertThat(media).hasSize(1);
        assertThat(media.get(0)).isNotNull();
        assertThat(media.get(0).getMediaId()).isEqualTo(testId);
        assertThat(media.get(0).getTitle()).isEqualTo(testTitle);
        assertThat(media.get(0).getDescription()).isEqualTo(testDescription);
        assertThat(media.get(0).getCaption()).isEqualTo(testCaption);
    }

    // Inserts 10 items with known IDs then retrieves all media and validates IDs
    @Test
    public void testGetAllSiteMedia() {
        long[] testIds = insertBasicTestItems(SMALL_TEST_POOL);
        List<MediaModel> storedMedia = MediaSqlUtils.getAllSiteMedia(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID));
        assertThat(storedMedia).hasSize(testIds.length);
        for (int i = 0; i < testIds.length; ++i) {
            assertThat(storedMedia.get(i)).isNotNull();
            assertThat(storedMedia.get(i).getMediaId()).isEqualTo(testIds[i]);
        }
    }

    // Inserts a media item, verifies it's in the DB, deletes the item, verifies it's not in the DB
    @Test
    public void testDeleteMedia() {
        long testId = Math.abs(mRandom.nextLong());
        MediaModel testMedia = getTestMedia(testId);
        assertThat(MediaSqlUtils.insertOrUpdateMedia(testMedia)).isEqualTo(1);
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), testId);
        assertThat(media).hasSize(1);
        assertThat(media.get(0)).isNotNull();
        assertThat(media.get(0).getMediaId()).isEqualTo(testId);
        assertThat(MediaSqlUtils.deleteMedia(testMedia)).isEqualTo(1);
        media = MediaSqlUtils.getAllSiteMedia(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID));
        assertThat(media.isEmpty()).isTrue();
    }

    // Inserts local media items and ensures they're deletable, and that they're recognized as unique by deleteMedia()
    @Test
    public void testDeleteLocalMedia() {
        MediaModel testLocalMedia = getTestMedia(0);
        MediaModel testLocalMedia2 = getTestMedia(0);

        assertThat(MediaSqlUtils.insertOrUpdateMedia(testLocalMedia)).isEqualTo(1);
        assertThat(MediaSqlUtils.insertOrUpdateMedia(testLocalMedia2)).isEqualTo(1);

        List<MediaModel> media = MediaSqlUtils.getAllSiteMedia(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID));
        assertThat(media).hasSize(2);

        assertThat(MediaSqlUtils.deleteMedia(media.get(0))).isEqualTo(1);
    }

    // Inserts many media items then retrieves only some items and validates based on ID
    @Test
    public void testGetSpecifiedMedia() {
        long[] testIds = insertBasicTestItems(SMALL_TEST_POOL);
        List<Long> mediaIds = new ArrayList<>();
        for (int i = 0; i < SMALL_TEST_POOL; i += 2) {
            mediaIds.add(testIds[i]);
        }
        List<MediaModel> media = MediaSqlUtils.
                getSiteMediaWithIds(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), mediaIds);
        assertThat(media).hasSize(SMALL_TEST_POOL / 2);
        for (int i = 0; i < media.size(); ++i) {
            assertThat(media.get(i).getMediaId()).isEqualTo(testIds[i * 2]);
        }
    }

    // Inserts media of multiple MIME types then retrieves only images and verifies
    @Test
    public void testGetSiteImages() {
        List<Long> imageIds = new ArrayList<>(SMALL_TEST_POOL);
        List<Long> videoIds = new ArrayList<>(SMALL_TEST_POOL);
        for (int i = 0; i < imageIds.size(); ++i) {
            imageIds.add(mRandom.nextLong());
            videoIds.add(mRandom.nextLong());
            MediaModel image = getTestMedia(imageIds.get(i));
            image.setMimeType("image/jpg");
            MediaModel video = getTestMedia(videoIds.get(i));
            video.setMimeType("video/mp4");
            assertThat(MediaSqlUtils.insertOrUpdateMedia(image)).isEqualTo(0);
            assertThat(MediaSqlUtils.insertOrUpdateMedia(video)).isEqualTo(0);
        }
        List<MediaModel> images = MediaSqlUtils.getSiteImages(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID));
        assertThat(imageIds.size()).isEqualTo(images.size());
        for (int i = 0; i < imageIds.size(); ++i) {
            assertThat(images.get(0).getMimeType().contains(Type.IMAGE.getValue())).isTrue();
            assertThat(imageIds).contains(images.get(i).getMediaId());
        }
    }

    // Inserts many images then retrieves all images with a supplied exclusion filter
    @Test
    public void testGetSiteImagesExclusionFilter() {
        long[] imageIds = insertImageTestItems(SMALL_TEST_POOL);
        List<Long> exclusion = new ArrayList<>();
        for (int i = 0; i < SMALL_TEST_POOL; i += 2) {
            exclusion.add(imageIds[i]);
        }
        List<MediaModel> includedImages = MediaSqlUtils
                .getSiteImagesExcluding(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), exclusion);
        assertThat(includedImages).hasSize(SMALL_TEST_POOL - exclusion.size());
        for (int i = 0; i < includedImages.size(); ++i) {
            assertThat(exclusion).doesNotContain(includedImages.get(i).getMediaId());
        }
    }

    // Inserts many media with compounding titles, verifies search field narrows as terms are added
    @Test
    public void testMediaTitleSearch() {
        String[] testTitles = new String[SMALL_TEST_POOL];
        testTitles[0] = "Base String";
        assertThat(MediaSqlUtils.insertOrUpdateMedia(getTestMedia(0, testTitles[0], "", ""))).isEqualTo(1);
        for (int i = 1; i < testTitles.length; ++i) {
            testTitles[i] = testTitles[i - 1] + i;
            assertThat(MediaSqlUtils.insertOrUpdateMedia(getTestMedia(i, testTitles[i], "", ""))).isEqualTo(1);
        }
        for (int i = 0; i < testTitles.length; ++i) {
            List<MediaModel> mediaModels = MediaSqlUtils
                    .searchSiteMedia(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), testTitles[i]);
            assertThat(mediaModels).hasSize(SMALL_TEST_POOL - i);
        }
    }

    // Inserts many media with compounding titles, gets media with exact title and verifies
    @Test
    public void testMatchSiteMediaColumn() {
        String[] testTitles = new String[SMALL_TEST_POOL];
        testTitles[0] = "Base String";
        assertThat(MediaSqlUtils.insertOrUpdateMedia(getTestMedia(0, testTitles[0], "", ""))).isEqualTo(1);
        for (int i = 1; i < testTitles.length; ++i) {
            testTitles[i] = testTitles[i - 1] + i;
            assertThat(MediaSqlUtils.insertOrUpdateMedia(getTestMedia(i, testTitles[i], "", ""))).isEqualTo(1);
        }
        for (String testTitle : testTitles) {
            List<MediaModel> mediaModels = MediaSqlUtils
                    .matchSiteMedia(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), MediaModelTable.TITLE, testTitle);
            assertThat(mediaModels).hasSize(1);
            assertThat(mediaModels.get(0).getTitle()).isEqualTo(testTitle);
        }
    }

    // Adds media with known post ID and title, deletes via postId and title, verifies
    @Test
    public void testMatchPostMedia() {
        MediaModel testMedia = getTestMedia(1);
    }

    // Adds a media item with known fields, updates all fields, retrieves and verifies
    @Test
    public void testUpdateExistingMedia() {
        long testId = 1;
        long testPostId = 10;
        long testAuthorId = 100;
        String testGuid = "testGuid";
        int testLocalSiteId = 1000;
        String testUploadDate = "testUploadDate";
        String testTitle = "testTitle";
        String testDescription = "testDescription";
        String testCaption = "testCaption";
        String testUrl = "testUrl";
        String testThumbnailUrl = "testThumbnailUrl";
        String testPath = "testPath";
        String testFileName = "testFileName";
        String testFileExt = "testFileExt";
        String testMimeType = "video/mp4";
        String testAlt = "testAlt";
        int testWidth = 1024;
        int testHeight = 768;
        int testLength = 60;
        String testVideoPressGuid = "testVideoPressGuid";
        boolean testVideoPressProcessing = false;
        String testUploadState = MediaUploadState.UPLOADING.toString();
        int testHorizontalAlign = 500;
        boolean testVerticalAlign = false;
        boolean testFeatured = false;
        boolean testFeaturedInPost = false;
        boolean testMarkedLocallyAsFeatured = false;
        MediaModel testModel = new MediaModel();
        testModel.setMediaId(testId);
        testModel.setPostId(testPostId);
        testModel.setAuthorId(testAuthorId);
        testModel.setGuid(testGuid);
        testModel.setUploadDate(testUploadDate);
        testModel.setUrl(testUrl);
        testModel.setThumbnailUrl(testThumbnailUrl);
        testModel.setFileName(testFileName);
        testModel.setFilePath(testPath);
        testModel.setFileExtension(testFileExt);
        testModel.setMimeType(testMimeType);
        testModel.setTitle(testTitle);
        testModel.setCaption(testCaption);
        testModel.setDescription(testDescription);
        testModel.setAlt(testAlt);
        testModel.setWidth(testWidth);
        testModel.setHeight(testHeight);
        testModel.setLength(testLength);
        testModel.setVideoPressGuid(testVideoPressGuid);
        testModel.setVideoPressProcessingDone(testVideoPressProcessing);
        testModel.setUploadState(testUploadState);
        testModel.setLocalSiteId(testLocalSiteId);
        testModel.setHorizontalAlignment(testHorizontalAlign);
        testModel.setVerticalAlignment(testVerticalAlign);
        testModel.setFeatured(testFeatured);
        testModel.setFeaturedInPost(testFeaturedInPost);
        testModel.setMarkedLocallyAsFeatured(testMarkedLocallyAsFeatured);
        assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(testModel));
        testModel.setPostId(testPostId + 1);
        testModel.setAuthorId(testAuthorId + 1);
        testModel.setGuid(testGuid + 1);
        testModel.setUploadDate(testUploadDate + 1);
        testModel.setUrl(testUrl + 1);
        testModel.setThumbnailUrl(testThumbnailUrl + 1);
        testModel.setFileName(testFileName + 1);
        testModel.setFilePath(testPath + 1);
        testModel.setFileExtension(testFileExt + 1);
        testModel.setMimeType(testMimeType + 1);
        testModel.setTitle(testTitle + 1);
        testModel.setCaption(testCaption + 1);
        testModel.setDescription(testDescription + 1);
        testModel.setAlt(testAlt + 1);
        testModel.setWidth(testWidth + 1);
        testModel.setHeight(testHeight + 1);
        testModel.setLength(testLength + 1);
        testModel.setVideoPressGuid(testVideoPressGuid + 1);
        testModel.setVideoPressProcessingDone(!testVideoPressProcessing);
        testModel.setUploadState(testUploadState + 1);
        testModel.setHorizontalAlignment(testHorizontalAlign + 1);
        testModel.setVerticalAlignment(!testVerticalAlign);
        testModel.setFeatured(!testFeatured);
        testModel.setFeaturedInPost(!testFeaturedInPost);
        testModel.setMarkedLocallyAsFeatured(!testMarkedLocallyAsFeatured);
        assertThat(MediaSqlUtils.insertOrUpdateMedia(testModel)).isEqualTo(1);
        List<MediaModel> media = MediaSqlUtils.getAllSiteMedia(getTestSiteWithLocalId(testLocalSiteId));
        assertThat(media).hasSize(1);
        MediaModel testMedia = media.get(0);
        assertThat(testMedia.getMediaId()).isEqualTo(testId);
        assertThat(testMedia.getPostId()).isEqualTo(testPostId + 1);
        assertThat(testMedia.getAuthorId()).isEqualTo(testAuthorId + 1);
        assertThat(testMedia.getGuid()).isEqualTo(testGuid + 1);
        assertThat(testMedia.getUploadDate()).isEqualTo(testUploadDate + 1);
        assertThat(testMedia.getTitle()).isEqualTo(testTitle + 1);
        assertThat(testMedia.getDescription()).isEqualTo(testDescription + 1);
        assertThat(testMedia.getCaption()).isEqualTo(testCaption + 1);
        assertThat(testMedia.getUrl()).isEqualTo(testUrl + 1);
        assertThat(testMedia.getThumbnailUrl()).isEqualTo(testThumbnailUrl + 1);
        assertThat(testMedia.getFilePath()).isEqualTo(testPath + 1);
        assertThat(testMedia.getFileName()).isEqualTo(testFileName + 1);
        assertThat(testMedia.getFileExtension()).isEqualTo(testFileExt + 1);
        assertThat(testMedia.getMimeType()).isEqualTo(testMimeType + 1);
        assertThat(testMedia.getAlt()).isEqualTo(testAlt + 1);
        assertThat(testMedia.getWidth()).isEqualTo(testWidth + 1);
        assertThat(testMedia.getHeight()).isEqualTo(testHeight + 1);
        assertThat(testMedia.getLength()).isEqualTo(testLength + 1);
        assertThat(testMedia.getVideoPressGuid()).isEqualTo(testVideoPressGuid + 1);
        assertThat(testMedia.getVideoPressProcessingDone()).isEqualTo(!testVideoPressProcessing);
        assertThat(testMedia.getUploadState()).isEqualTo(testUploadState + 1);
        assertThat(testMedia.getHorizontalAlignment()).isEqualTo(testHorizontalAlign + 1);
        assertThat(testMedia.getVerticalAlignment()).isEqualTo(!testVerticalAlign);
        assertThat(testMedia.getFeatured()).isEqualTo(!testFeatured);
        assertThat(testMedia.getFeaturedInPost()).isEqualTo(!testFeaturedInPost);
        assertThat(testMedia.getMarkedLocallyAsFeatured()).isEqualTo(!testMarkedLocallyAsFeatured);
    }

    // Inserts many items with matching titles, deletes all media with the title, verifies
    @Test
    public void testDeleteMatchingSiteMedia() {
        MediaSqlUtils.insertOrUpdateMedia(getTestMedia(SMALL_TEST_POOL + 1, "Not the same title", "", ""));
        String testTitle = "Test Title";
        for (int i = 0; i < SMALL_TEST_POOL; ++i) {
            assertThat(MediaSqlUtils.insertOrUpdateMedia(getTestMedia(i, testTitle, "", ""))).isEqualTo(1);
        }
        assertThat(MediaSqlUtils
                .deleteMatchingSiteMedia(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), MediaModelTable.TITLE, testTitle))
                .isEqualTo(SMALL_TEST_POOL);
        List<MediaModel> media = MediaSqlUtils.getAllSiteMedia(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID));
        assertThat(1).isEqualTo(media.size());
        assertThat(media.get(0).getMediaId()).isEqualTo(SMALL_TEST_POOL + 1);
    }

    @Test
    public void testGetNotDeletedUnattachedMediaAsCursor() {
        SiteModel site = getTestSiteWithLocalId(TEST_LOCAL_SITE_ID);

        // Insert media
        insertBasicTestItems(SMALL_TEST_POOL);

        // Insert one deleted media
        MediaModel image = getTestMedia(42);
        image.setUploadState(MediaUploadState.DELETED);
        assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(image));

        assertThat(MediaSqlUtils.getMediaWithStatesAsCursor(site, NOT_DELETED_STATES).getCount())
                .isEqualTo(SMALL_TEST_POOL);
    }

    @Test
    public void testGetNotDeletedSiteMediaAsCursor() {
        SiteModel site = getTestSiteWithLocalId(TEST_LOCAL_SITE_ID);

        // Insert media
        insertBasicTestItems(SMALL_TEST_POOL);

        // Insert one detached but deleted media
        MediaModel media = getTestMedia(42);
        media.setUploadState(MediaUploadState.DELETED);
        assertThat(MediaSqlUtils.insertOrUpdateMedia(media)).isEqualTo(1);

        // Insert one attached media
        media = getTestMedia(43);
        media.setUploadState(MediaUploadState.UPLOADED);
        media.setPostId(42);

        assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(media));
        Cursor c = MediaSqlUtils.getUnattachedMediaWithStates(site, NOT_DELETED_STATES);
        assertThat(c.getCount()).isEqualTo(SMALL_TEST_POOL);
    }

    @Test
    public void testGetNotDeletedSiteImagesAsCursor() {
        SiteModel site = getTestSiteWithLocalId(TEST_LOCAL_SITE_ID);

        // Insert images
        insertImageTestItems(SMALL_TEST_POOL);

        // Insert one deleted image
        MediaModel image = getTestMedia(42);
        image.setMimeType("image/jpg");
        image.setUploadState(MediaUploadState.DELETED);
        assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(image));

        assertThat(MediaSqlUtils.getImagesWithStatesAsCursor(site, NOT_DELETED_STATES).getCount())
                .isEqualTo(SMALL_TEST_POOL);
        assertThat(MediaSqlUtils.getMediaWithStatesAsCursor(site, NOT_DELETED_STATES).getCount())
                .isEqualTo(SMALL_TEST_POOL);
    }

    @Test
    public void testPushAndFetchCollision() throws InterruptedException {
        // Test uploading media, fetching remote media and updating the db from the fetch first

        MediaModel mediaModel = getTestMedia(0);
        MediaSqlUtils.insertMediaForResult(mediaModel);

        SiteModel site = getTestSiteWithLocalId(TEST_LOCAL_SITE_ID);

        // The media item after uploading, updated with the remote media ID, about to be saved locally
        MediaModel mediaFromUploadResponse = MediaSqlUtils.getAllSiteMedia(site).get(0);
        mediaFromUploadResponse.setUploadState(MediaUploadState.UPLOADED);
        mediaFromUploadResponse.setMediaId(42);

        // The same media, but fetched from the server from FETCH_MEDIA_LIST (so no local ID until insertion)
        final MediaModel mediaFromMediaListFetch = MediaSqlUtils.getAllSiteMedia(site).get(0);
        mediaFromMediaListFetch.setUploadState(MediaUploadState.UPLOADED);
        mediaFromMediaListFetch.setMediaId(42);
        mediaFromMediaListFetch.setId(0);

        MediaSqlUtils.insertOrUpdateMedia(mediaFromMediaListFetch);
        MediaSqlUtils.insertOrUpdateMedia(mediaFromUploadResponse);

        assertThat(MediaSqlUtils.getAllSiteMedia(site).size()).isEqualTo(1);

        MediaModel finalMedia = MediaSqlUtils.getAllSiteMedia(site).get(0);
        assertThat(finalMedia.getMediaId()).isEqualTo(42);
        assertThat(mediaModel.getLocalSiteId()).isEqualTo(finalMedia.getLocalSiteId());
    }

    // Utilities

    private long[] insertBasicTestItems(int num) {
        long[] testItemIds = new long[num];
        for (int i = 0; i < num; ++i) {
            testItemIds[i] = mRandom.nextLong();
            MediaModel media = getTestMedia(testItemIds[i]);
            media.setUploadState(MediaUploadState.UPLOADED);
            assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(media));
        }
        return testItemIds;
    }

    private long[] insertImageTestItems(int num) {
        long[] testItemIds = new long[num];
        for (int i = 0; i < num; ++i) {
            testItemIds[i] = Math.abs(mRandom.nextInt());
            MediaModel image = getTestMedia(testItemIds[i]);
            image.setMimeType("image/jpg");
            image.setUploadState(MediaUploadState.UPLOADED);
            assertThat(1).isEqualTo(MediaSqlUtils.insertOrUpdateMedia(image));
        }
        return testItemIds;
    }

    private MediaModel getTestMedia(long mediaId) {
        MediaModel media = new MediaModel();
        media.setLocalSiteId(TEST_LOCAL_SITE_ID);
        media.setMediaId(mediaId);
        return media;
    }

    private MediaModel getTestMedia(long mediaId, String title, String description, String caption) {
        MediaModel media = new MediaModel();
        media.setLocalSiteId(TEST_LOCAL_SITE_ID);
        media.setMediaId(mediaId);
        media.setTitle(title);
        media.setDescription(description);
        media.setCaption(caption);
        return media;
    }

    private String getTestString() {
        return "BaseTestString-" + mRandom.nextInt();
    }

    private SiteModel getTestSiteWithLocalId(int localSiteId) {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(localSiteId);
        return siteModel;
    }
}
