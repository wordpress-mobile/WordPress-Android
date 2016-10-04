package org.wordpress.android.fluxc.persistence;

import com.wellsql.generated.MediaModelTable;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.WellCursor;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.List;

public class MediaSqlUtils {
    public static List<MediaModel> getAllSiteMedia(SiteModel siteModel) {
        return getAllSiteMediaQuery(siteModel).getAsModel();
    }

    public static WellCursor<MediaModel> getAllSiteMediaAsCursor(SiteModel siteModel) {
        return getAllSiteMediaQuery(siteModel).getAsCursor();
    }

    private static SelectQuery<MediaModel> getAllSiteMediaQuery(SiteModel siteModel) {
        return WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.SITE_ID, siteModel.getSiteId()).endWhere();
    }

    public static List<MediaModel> getSiteMediaWithId(SiteModel siteModel, long mediaId) {
        return WellSql.select(MediaModel.class).where().beginGroup()
                .equals(MediaModelTable.SITE_ID, siteModel.getSiteId())
                .equals(MediaModelTable.MEDIA_ID, mediaId)
                .endGroup().endWhere().getAsModel();
    }

    public static List<MediaModel> getSiteMediaWithIds(SiteModel siteModel, List<Long> mediaIds) {
        return getSiteMediaWithIdsQuery(siteModel, mediaIds).getAsModel();
    }

    public static WellCursor<MediaModel> getSiteMediaWithIdsAsCursor(SiteModel siteModel, List<Long> mediaIds) {
        return getSiteMediaWithIdsQuery(siteModel, mediaIds).getAsCursor();
    }

    private static SelectQuery<MediaModel> getSiteMediaWithIdsQuery(SiteModel siteModel, List<Long> mediaIds) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.SITE_ID, siteModel.getSiteId())
                .isIn(MediaModelTable.MEDIA_ID, mediaIds)
                .endGroup().endWhere();
    }

    public static List<MediaModel> searchSiteMedia(SiteModel siteModel, String column, String searchTerm) {
        return searchSiteMediaQuery(siteModel, column, searchTerm).getAsModel();
    }

    public static WellCursor<MediaModel> searchSiteMediaAsCursor(SiteModel siteModel, String column, String searchTerm) {
        return searchSiteMediaQuery(siteModel, column, searchTerm).getAsCursor();
    }

    private static SelectQuery<MediaModel> searchSiteMediaQuery(SiteModel siteModel, String column, String searchTerm) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.SITE_ID, siteModel.getSiteId())
                .contains(column, searchTerm)
                .endGroup().endWhere();
    }

    public static List<MediaModel> getSiteImages(SiteModel siteModel) {
        return getSiteImagesQuery(siteModel).getAsModel();
    }

    public static WellCursor<MediaModel> getSiteImagesAsCursor(SiteModel siteModel) {
        return getSiteImagesQuery(siteModel).getAsCursor();
    }

    private static SelectQuery<MediaModel> getSiteImagesQuery(SiteModel siteModel) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.SITE_ID, siteModel.getSiteId())
                .contains(MediaModelTable.MIME_TYPE, MediaUtils.MIME_TYPE_IMAGE)
                .endGroup().endWhere();
    }

    public static List<MediaModel> getSiteImagesExcluding(SiteModel siteModel, List<Long> filter) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.SITE_ID, siteModel.getSiteId())
                .contains(MediaModelTable.MIME_TYPE, MediaUtils.MIME_TYPE_IMAGE)
                .isNotIn(MediaModelTable.MEDIA_ID, filter)
                .endGroup().endWhere()
                .getAsModel();
    }

    public static List<MediaModel> getSiteMediaExcluding(SiteModel siteModel, String column, Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.SITE_ID, siteModel.getSiteId())
                .notContains(column, value)
                .endGroup().endWhere().getAsModel();
    }

    public static List<MediaModel> matchSiteMedia(SiteModel siteModel, String column, Object value) {
        return matchSiteMediaQuery(siteModel, column, value).getAsModel();
    }

    public static WellCursor<MediaModel> matchSiteMediaAsCursor(SiteModel siteModel, String column, Object value) {
        return matchSiteMediaQuery(siteModel, column, value).getAsCursor();
    }

    private static SelectQuery<MediaModel> matchSiteMediaQuery(SiteModel siteModel, String column, Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.SITE_ID, siteModel.getSiteId())
                .equals(column, value)
                .endGroup().endWhere();
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
                .where().beginGroup()
                .equals(MediaModelTable.SITE_ID, media.getSiteId())
                .equals(MediaModelTable.MEDIA_ID, media.getMediaId())
                .endGroup().endWhere().getAsModel();

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

    public static int deleteMatchingSiteMedia(SiteModel siteModel, String column, Object value) {
        return WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.SITE_ID, siteModel.getSiteId())
                .equals(column, value)
                .endGroup().endWhere().execute();
    }
}
