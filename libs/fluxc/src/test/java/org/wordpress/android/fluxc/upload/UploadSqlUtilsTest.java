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
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
public class UploadSqlUtilsTest {
    private static final int TEST_LOCAL_SITE_ID = 42;

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
        MediaModel testMedia = getTestMedia(testId);
        Assert.assertEquals(1, MediaSqlUtils.insertOrUpdateMedia(testMedia));
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), testId);
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

        media = MediaSqlUtils.getSiteMediaWithId(getTestSiteWithLocalId(TEST_LOCAL_SITE_ID), testId);
        Assert.assertTrue(media.isEmpty());

        mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(testMedia.getId());
        Assert.assertNull(mediaUploadModel);
    }

    private MediaModel getTestMedia(long mediaId) {
        MediaModel media = new MediaModel();
        media.setLocalSiteId(TEST_LOCAL_SITE_ID);
        media.setMediaId(mediaId);
        return media;
    }

    private SiteModel getTestSiteWithLocalId(int localSiteId) {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(localSiteId);
        return siteModel;
    }
}
