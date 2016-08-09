package org.wordpress.android.fluxc.persistence;

import com.wellsql.generated.MediaModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.List;

public class MediaSqlUtils {
    public static List<MediaModel> getAllSiteMedia(long siteId) {
        return WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.BLOG_ID, siteId).endWhere()
                .getAsModel();
    }

    public static List<MediaModel> getSiteMediaWithId(long siteId, long mediaId) {
        return WellSql.select(MediaModel.class).where().beginGroup()
                .equals(MediaModelTable.BLOG_ID, siteId)
                .equals(MediaModelTable.MEDIA_ID, mediaId)
                .endGroup().endWhere().getAsModel();
    }

    public static List<MediaModel> getSiteMediaWithIds(long siteId, List<Long> mediaIds) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.BLOG_ID, siteId)
                .isIn(MediaModelTable.MEDIA_ID, mediaIds)
                .endGroup().endWhere().getAsModel();
    }

    public static List<MediaModel> searchSiteMedia(long siteId, String column, String searchTerm) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.BLOG_ID, siteId)
                .contains(column, searchTerm)
                .endGroup().endWhere().getAsModel();
    }

    public static List<MediaModel> getSiteImages(long siteId) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.BLOG_ID, siteId)
                .contains(MediaModelTable.MIME_TYPE, MediaUtils.MIME_TYPE_IMAGE)
                .endGroup().endWhere()
                .getAsModel();
    }

    public static List<MediaModel> getSiteImagesExcluding(long siteId, List<Long> filter) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.BLOG_ID, siteId)
                .contains(MediaModelTable.MIME_TYPE, MediaUtils.MIME_TYPE_IMAGE)
                .isNotIn(MediaModelTable.MEDIA_ID, filter)
                .endGroup().endWhere()
                .getAsModel();
    }

    public static List<MediaModel> matchSiteMedia(long siteId, String column, Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.BLOG_ID, siteId)
                .equals(column, value)
                .endGroup().endWhere().getAsModel();
    }

    public static List<MediaModel> matchPostMedia(long postId, String column, Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.POST_ID, postId)
                .equals(column, value)
                .endGroup().endWhere().getAsModel();
    }

    public static int insertOrUpdateMedia(MediaModel media) {
        if (media == null) return 0;

        List<MediaModel> existingMedia = WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.MEDIA_ID, media.getMediaId()).endWhere()
                .getAsModel();

        if (existingMedia.isEmpty()) {
            // insert, media item does not exist
            WellSql.insert(media).asSingleTransaction(true).execute();
            return 0;
        } else {
            // update, media item already exists
            int oldId = existingMedia.get(0).getId();
            return WellSql.update(MediaModel.class).whereId(oldId)
                    .put(media, new UpdateAllExceptId<MediaModel>()).execute();
        }
    }

    public static int deleteMedia(MediaModel media) {
        if (media == null) return 0;
        return WellSql.delete(MediaModel.class).whereId(media.getId());
    }

    public static int deleteMatchingSiteMedia(long siteId, String column, Object value) {
        return WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.BLOG_ID, siteId)
                .equals(column, value)
                .endGroup().endWhere().execute();
    }
}
