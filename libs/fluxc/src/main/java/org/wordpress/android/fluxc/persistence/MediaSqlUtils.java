package org.wordpress.android.fluxc.persistence;

import android.text.TextUtils;

import com.wellsql.generated.MediaModelTable;
import com.yarolegovich.wellsql.ConditionClauseBuilder;
import com.yarolegovich.wellsql.DeleteQuery;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.WellCursor;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.utils.MimeType.Type;

import java.util.ArrayList;
import java.util.List;

public class MediaSqlUtils {
    public static List<MediaModel> getAllSiteMedia(SiteModel siteModel) {
        return getAllSiteMediaQuery(siteModel).getAsModel();
    }

    public static List<MediaModel> getMediaWithStates(SiteModel site, List<String> uploadStates) {
        return getMediaWithStatesQuery(site, uploadStates).getAsModel();
    }

    public static WellCursor<MediaModel> getMediaWithStatesAsCursor(SiteModel site, List<String> uploadStates) {
        return getMediaWithStatesQuery(site, uploadStates).getAsCursor();
    }

    public static List<MediaModel> getMediaWithStatesAndMimeType(SiteModel site,
                                                                 List<String> uploadStates,
                                                                 String mimeType) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .contains(MediaModelTable.MIME_TYPE, mimeType)
                .isIn(MediaModelTable.UPLOAD_STATE, uploadStates)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    public static WellCursor<MediaModel> getImagesWithStatesAsCursor(SiteModel site, List<String> uploadStates) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .contains(MediaModelTable.MIME_TYPE, Type.IMAGE.getValue())
                .isIn(MediaModelTable.UPLOAD_STATE, uploadStates)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsCursor();
    }

    public static WellCursor<MediaModel> getUnattachedMediaWithStates(SiteModel site, List<String> uploadStates) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .equals(MediaModelTable.POST_ID, 0)
                .isIn(MediaModelTable.UPLOAD_STATE, uploadStates)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsCursor();
    }

    private static SelectQuery<MediaModel> getAllSiteMediaQuery(SiteModel siteModel) {
        return WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId()).endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    private static SelectQuery<MediaModel> getMediaWithStatesQuery(SiteModel site, List<String> uploadStates) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .isIn(MediaModelTable.UPLOAD_STATE, uploadStates)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    public static List<MediaModel> getSiteMediaWithId(SiteModel siteModel, long mediaId) {
        return WellSql.select(MediaModel.class).where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(MediaModelTable.MEDIA_ID, mediaId)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    public static List<MediaModel> getSiteMediaWithIds(SiteModel siteModel, List<Long> mediaIds) {
        return getSiteMediaWithIdsQuery(siteModel, mediaIds).getAsModel();
    }

    private static SelectQuery<MediaModel> getSiteMediaWithIdsQuery(SiteModel siteModel, List<Long> mediaIds) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .isIn(MediaModelTable.MEDIA_ID, mediaIds)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    public static MediaModel getMediaWithLocalId(int localMediaId) {
        List<MediaModel> result = WellSql.select(MediaModel.class).where()
                .equals(MediaModelTable.ID, localMediaId)
                .endWhere()
                .getAsModel();
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public static List<MediaModel> searchSiteMedia(SiteModel siteModel, String searchTerm) {
        return searchSiteMediaQuery(siteModel, searchTerm).getAsModel();
    }

    public static List<MediaModel> searchSiteImages(SiteModel siteModel, String searchTerm) {
        return searchSiteMediaByMimeTypeQuery(siteModel, searchTerm, Type.IMAGE.getValue()).getAsModel();
    }

    public static List<MediaModel> searchSiteAudio(SiteModel siteModel, String searchTerm) {
        return searchSiteMediaByMimeTypeQuery(siteModel, searchTerm, Type.AUDIO.getValue()).getAsModel();
    }

    public static List<MediaModel> searchSiteVideos(SiteModel siteModel, String searchTerm) {
        return searchSiteMediaByMimeTypeQuery(siteModel, searchTerm, Type.VIDEO.getValue()).getAsModel();
    }

    public static List<MediaModel> searchSiteDocuments(SiteModel siteModel, String searchTerm) {
        return searchSiteMediaByMimeTypeQuery(siteModel, searchTerm, Type.APPLICATION.getValue()).getAsModel();
    }

    private static SelectQuery<MediaModel> searchSiteMediaQuery(SiteModel siteModel,
                                                                String searchTerm) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .beginGroup()
                    .contains(MediaModelTable.TITLE, searchTerm)
                    .or().contains(MediaModelTable.CAPTION, searchTerm)
                    .or().contains(MediaModelTable.DESCRIPTION, searchTerm)
                    .or().contains(MediaModelTable.MIME_TYPE, searchTerm)
                .endGroup()
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    private static SelectQuery<MediaModel> searchSiteMediaByMimeTypeQuery(SiteModel siteModel,
                                                                          String searchTerm,
                                                                          String mimeTypePrefix) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .contains(MediaModelTable.MIME_TYPE, mimeTypePrefix)
                .beginGroup()
                    .contains(MediaModelTable.TITLE, searchTerm)
                    .or().contains(MediaModelTable.CAPTION, searchTerm)
                    .or().contains(MediaModelTable.DESCRIPTION, searchTerm)
                .endGroup()
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    public static List<MediaModel> getSiteImages(SiteModel siteModel) {
        return getSiteImagesQuery(siteModel).getAsModel();
    }

    private static SelectQuery<MediaModel> getSiteImagesQuery(SiteModel siteModel) {
        return getSiteMediaByMimeTypeQuery(siteModel, Type.IMAGE.getValue());
    }

    public static List<MediaModel> getSiteImagesExcluding(SiteModel siteModel, List<Long> filter) {
        return getSiteImagesExcludingQuery(siteModel, filter).getAsModel();
    }

    public static List<MediaModel> getSiteVideos(SiteModel siteModel) {
        return getSiteVideosQuery(siteModel).getAsModel();
    }

    public static List<MediaModel> getSiteDocuments(SiteModel siteModel) {
        return getSiteDocumentsQuery(siteModel).getAsModel();
    }

    public static List<MediaModel> getSiteAudio(SiteModel siteModel) {
        return getSiteAudioQuery(siteModel).getAsModel();
    }

    public static SelectQuery<MediaModel> getSiteImagesExcludingQuery(SiteModel siteModel, List<Long> filter) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .contains(MediaModelTable.MIME_TYPE, Type.IMAGE.getValue())
                .isNotIn(MediaModelTable.MEDIA_ID, filter)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    private static SelectQuery<MediaModel> getSiteVideosQuery(SiteModel siteModel) {
        return getSiteMediaByMimeTypeQuery(siteModel, Type.VIDEO.getValue());
    }

    private static SelectQuery<MediaModel> getSiteAudioQuery(SiteModel siteModel) {
        return getSiteMediaByMimeTypeQuery(siteModel, Type.AUDIO.getValue());
    }

    private static SelectQuery<MediaModel> getSiteDocumentsQuery(SiteModel siteModel) {
        return getSiteMediaByMimeTypeQuery(siteModel, Type.APPLICATION.getValue());
    }

    private static SelectQuery<MediaModel> getSiteMediaByMimeTypeQuery(SiteModel siteModel, String mimeTypePrefix) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .contains(MediaModelTable.MIME_TYPE, mimeTypePrefix)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    public static List<MediaModel> getSiteMediaExcluding(SiteModel site, String column, Object value) {
        return getSiteMediaExcludingQuery(site, column, value).getAsModel();
    }

    public static List<MediaModel> matchSiteMedia(SiteModel siteModel, String column, Object value) {
        return matchSiteMediaQuery(siteModel, column, value).getAsModel();
    }

    private static SelectQuery<MediaModel> matchSiteMediaQuery(SiteModel siteModel, String column, Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(column, value)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    public static List<MediaModel> matchPostMedia(int localPostId, String column, Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_POST_ID, localPostId)
                .equals(column, value)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    public static List<MediaModel> matchPostMedia(int localPostId) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_POST_ID, localPostId)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    public static int insertOrUpdateMedia(MediaModel media) {
        if (media == null) return 0;

        List<MediaModel> existingMedia;
        if (media.getMediaId() == 0) {
            // If the remote media ID is 0, this is a local media file and we should only match by local ID
            // Otherwise, we'd match all local media files for that site
            existingMedia = WellSql.select(MediaModel.class)
                    .where()
                    .equals(MediaModelTable.ID, media.getId())
                    .endWhere().getAsModel();
        } else {
            // For remote media, we can uniquely identify the media by either its local ID
            // or its remote media ID + its (local) site ID
            existingMedia = WellSql.select(MediaModel.class)
                    .where().beginGroup()
                    .equals(MediaModelTable.ID, media.getId())
                    .or()
                    .beginGroup()
                    .equals(MediaModelTable.LOCAL_SITE_ID, media.getLocalSiteId())
                    .equals(MediaModelTable.MEDIA_ID, media.getMediaId())
                    .endGroup()
                    .endGroup().endWhere().getAsModel();
        }

        if (existingMedia.isEmpty()) {
            // insert, media item does not exist
            WellSql.insert(media).asSingleTransaction(true).execute();
            return 1;
        } else {
            if (existingMedia.size() > 1) {
                // We've ended up with a duplicate entry, probably due to a push/fetch race condition
                // One matches based on local ID (this is the one we're trying to update with a remote media ID)
                // The other matches based on local site ID + remote media ID, and we got it from a fetch
                // Just remove the entry without a remote media ID (the one matching the current media's local ID)
                return WellSql.delete(MediaModel.class).whereId(media.getId());
            }
            // update, media item already exists
            int oldId = existingMedia.get(0).getId();
            return WellSql.update(MediaModel.class).whereId(oldId)
                    .put(media, new UpdateAllExceptId<>(MediaModel.class)).execute();
        }
    }

    public static MediaModel insertMediaForResult(MediaModel media) {
        WellSql.insert(media).asSingleTransaction(true).execute();
        return media;
    }

    public static int deleteMedia(MediaModel media) {
        if (media == null) return 0;
        if (media.getMediaId() == 0) {
            // If the remote media ID is 0, this is a local media file and we should only match by local ID
            // Otherwise, we'd match all local media files for that site
            return WellSql.delete(MediaModel.class)
                    .where().beginGroup()
                    .equals(MediaModelTable.ID, media.getId())
                    .endGroup().endWhere()
                    .execute();
        } else {
            // For remote media, we can uniquely identify the media by either its local ID
            // or its remote media ID + its (local) site ID
            return WellSql.delete(MediaModel.class)
                    .where().beginGroup()
                    .equals(MediaModelTable.ID, media.getId())
                    .or()
                    .beginGroup()
                    .equals(MediaModelTable.LOCAL_SITE_ID, media.getLocalSiteId())
                    .equals(MediaModelTable.MEDIA_ID, media.getMediaId())
                    .endGroup()
                    .endGroup().endWhere()
                    .execute();
        }
    }

    public static int deleteMatchingSiteMedia(SiteModel siteModel, String column, Object value) {
        return WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(column, value)
                .endGroup().endWhere().execute();
    }

    public static int deleteAllSiteMedia(SiteModel site) {
        if (site == null) {
            return 0;
        }

        return WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .endGroup().endWhere().execute();
    }

    public static int deleteAllUploadedSiteMedia(SiteModel siteModel) {
        return WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(MediaModelTable.UPLOAD_STATE, MediaUploadState.UPLOADED.toString())
                .endGroup().endWhere().execute();
    }

    public static int deleteAllUploadedSiteMediaWithMimeType(SiteModel siteModel, String mimeType) {
        return WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(MediaModelTable.UPLOAD_STATE, MediaUploadState.UPLOADED.toString())
                .contains(MediaModelTable.MIME_TYPE, mimeType)
                .endGroup().endWhere().execute();
    }

    public static int deleteAllMedia() {
        return WellSql.delete(MediaModel.class).execute();
    }

    public static int deleteUploadedSiteMediaNotInList(SiteModel site, List<MediaModel> mediaList, String mimeType) {
        if (mediaList.isEmpty()) {
            if (!TextUtils.isEmpty(mimeType)) {
                return MediaSqlUtils.deleteAllUploadedSiteMediaWithMimeType(site, mimeType);
            } else {
                return MediaSqlUtils.deleteAllUploadedSiteMedia(site);
            }
        }

        List<Integer> idList = new ArrayList<>();
        for (MediaModel media : mediaList) {
            idList.add(media.getId());
        }

        ConditionClauseBuilder<DeleteQuery<MediaModel>> builder = WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .isNotIn(MediaModelTable.ID, idList)
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .equals(MediaModelTable.UPLOAD_STATE, MediaUploadState.UPLOADED.toString());

        if (!TextUtils.isEmpty(mimeType)) {
            builder.contains(MediaModelTable.MIME_TYPE, mimeType);
        }

        return builder.endGroup().endWhere().execute();
    }

    private static SelectQuery<MediaModel> getSiteMediaExcludingQuery(SiteModel site, String column, Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .not().equals(column, value)
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }
}
