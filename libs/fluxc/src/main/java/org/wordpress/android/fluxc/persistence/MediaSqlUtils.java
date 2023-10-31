package org.wordpress.android.fluxc.persistence;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    @NonNull
    public static List<MediaModel> getAllSiteMedia(@NonNull SiteModel siteModel) {
        return getAllSiteMediaQuery(siteModel).getAsModel();
    }

    @NonNull
    public static List<MediaModel> getMediaWithStates(
            @NonNull SiteModel site,
            @NonNull List<String> uploadStates) {
        return getMediaWithStatesQuery(site, uploadStates).getAsModel();
    }

    @NonNull
    public static WellCursor<MediaModel> getMediaWithStatesAsCursor(
            @NonNull SiteModel site,
            @NonNull List<String> uploadStates) {
        return getMediaWithStatesQuery(site, uploadStates).getAsCursor();
    }

    @NonNull
    public static List<MediaModel> getMediaWithStatesAndMimeType(
            @NonNull SiteModel site,
            @NonNull List<String> uploadStates,
            @NonNull String mimeType) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .contains(MediaModelTable.MIME_TYPE, mimeType)
                .isIn(MediaModelTable.UPLOAD_STATE, uploadStates)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    @NonNull
    public static WellCursor<MediaModel> getImagesWithStatesAsCursor(
            @NonNull SiteModel site,
            @NonNull List<String> uploadStates) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .contains(MediaModelTable.MIME_TYPE, Type.IMAGE.getValue())
                .isIn(MediaModelTable.UPLOAD_STATE, uploadStates)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsCursor();
    }

    @NonNull
    public static WellCursor<MediaModel> getUnattachedMediaWithStates(
            @NonNull SiteModel site,
            @NonNull List<String> uploadStates) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .equals(MediaModelTable.POST_ID, 0)
                .isIn(MediaModelTable.UPLOAD_STATE, uploadStates)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsCursor();
    }

    @NonNull
    private static SelectQuery<MediaModel> getAllSiteMediaQuery(@NonNull SiteModel siteModel) {
        return WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId()).endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    @NonNull
    private static SelectQuery<MediaModel> getMediaWithStatesQuery(
            @NonNull SiteModel site,
            @NonNull List<String> uploadStates) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .isIn(MediaModelTable.UPLOAD_STATE, uploadStates)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    @NonNull
    public static List<MediaModel> getSiteMediaWithId(@NonNull SiteModel siteModel, long mediaId) {
        return WellSql.select(MediaModel.class).where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(MediaModelTable.MEDIA_ID, mediaId)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    @NonNull
    public static List<MediaModel> getSiteMediaWithIds(
            @NonNull SiteModel siteModel,
            @NonNull List<Long> mediaIds) {
        return getSiteMediaWithIdsQuery(siteModel, mediaIds).getAsModel();
    }

    @NonNull
    private static SelectQuery<MediaModel> getSiteMediaWithIdsQuery(
            @NonNull SiteModel siteModel,
            @NonNull List<Long> mediaIds) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .isIn(MediaModelTable.MEDIA_ID, mediaIds)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    @Nullable
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

    @NonNull
    public static List<MediaModel> searchSiteMedia(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return searchSiteMediaQuery(siteModel, searchTerm).getAsModel();
    }

    @NonNull
    public static List<MediaModel> searchSiteImages(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return searchSiteMediaByMimeTypeQuery(siteModel, searchTerm, Type.IMAGE.getValue()).getAsModel();
    }

    @NonNull
    public static List<MediaModel> searchSiteAudio(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return searchSiteMediaByMimeTypeQuery(siteModel, searchTerm, Type.AUDIO.getValue()).getAsModel();
    }

    @NonNull
    public static List<MediaModel> searchSiteVideos(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return searchSiteMediaByMimeTypeQuery(siteModel, searchTerm, Type.VIDEO.getValue()).getAsModel();
    }

    @NonNull
    public static List<MediaModel> searchSiteDocuments(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return searchSiteMediaByMimeTypeQuery(siteModel, searchTerm, Type.APPLICATION.getValue()).getAsModel();
    }

    @NonNull
    private static SelectQuery<MediaModel> searchSiteMediaQuery(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
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

    @NonNull
    private static SelectQuery<MediaModel> searchSiteMediaByMimeTypeQuery(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm,
            @NonNull String mimeTypePrefix) {
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

    @NonNull
    public static List<MediaModel> getSiteImages(@NonNull SiteModel siteModel) {
        return getSiteImagesQuery(siteModel).getAsModel();
    }

    @NonNull
    private static SelectQuery<MediaModel> getSiteImagesQuery(@NonNull SiteModel siteModel) {
        return getSiteMediaByMimeTypeQuery(siteModel, Type.IMAGE.getValue());
    }

    @NonNull
    public static List<MediaModel> getSiteImagesExcluding(
            @NonNull SiteModel siteModel,
            @NonNull List<Long> filter) {
        return getSiteImagesExcludingQuery(siteModel, filter).getAsModel();
    }

    @NonNull
    public static List<MediaModel> getSiteVideos(@NonNull SiteModel siteModel) {
        return getSiteVideosQuery(siteModel).getAsModel();
    }

    @NonNull
    public static List<MediaModel> getSiteDocuments(@NonNull SiteModel siteModel) {
        return getSiteDocumentsQuery(siteModel).getAsModel();
    }

    @NonNull
    public static List<MediaModel> getSiteAudio(@NonNull SiteModel siteModel) {
        return getSiteAudioQuery(siteModel).getAsModel();
    }

    @NonNull
    public static SelectQuery<MediaModel> getSiteImagesExcludingQuery(
            @NonNull SiteModel siteModel,
            @NonNull List<Long> filter) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .contains(MediaModelTable.MIME_TYPE, Type.IMAGE.getValue())
                .isNotIn(MediaModelTable.MEDIA_ID, filter)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    @NonNull
    private static SelectQuery<MediaModel> getSiteVideosQuery(@NonNull SiteModel siteModel) {
        return getSiteMediaByMimeTypeQuery(siteModel, Type.VIDEO.getValue());
    }

    @NonNull
    private static SelectQuery<MediaModel> getSiteAudioQuery(@NonNull SiteModel siteModel) {
        return getSiteMediaByMimeTypeQuery(siteModel, Type.AUDIO.getValue());
    }

    @NonNull
    private static SelectQuery<MediaModel> getSiteDocumentsQuery(@NonNull SiteModel siteModel) {
        return getSiteMediaByMimeTypeQuery(siteModel, Type.APPLICATION.getValue());
    }

    @NonNull
    private static SelectQuery<MediaModel> getSiteMediaByMimeTypeQuery(
            @NonNull SiteModel siteModel,
            @NonNull String mimeTypePrefix) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .contains(MediaModelTable.MIME_TYPE, mimeTypePrefix)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    @NonNull
    public static List<MediaModel> getSiteMediaExcluding(
            @NonNull SiteModel site,
            @NonNull String column,
            @NonNull Object value) {
        return getSiteMediaExcludingQuery(site, column, value).getAsModel();
    }

    @NonNull
    public static List<MediaModel> matchSiteMedia(
            @NonNull SiteModel siteModel,
            @NonNull String column,
            @NonNull Object value) {
        return matchSiteMediaQuery(siteModel, column, value).getAsModel();
    }

    @NonNull
    private static SelectQuery<MediaModel> matchSiteMediaQuery(
            @NonNull SiteModel siteModel,
            @NonNull String column,
            @NonNull Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(column, value)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }

    @NonNull
    public static List<MediaModel> matchPostMedia(
            int localPostId,
            @NonNull String column,
            @NonNull Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_POST_ID, localPostId)
                .equals(column, value)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    @NonNull
    public static List<MediaModel> matchPostMedia(int localPostId) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_POST_ID, localPostId)
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING)
                .getAsModel();
    }

    public static int insertOrUpdateMedia(@Nullable MediaModel media) {
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

    @NonNull
    public static MediaModel insertMediaForResult(@NonNull MediaModel media) {
        WellSql.insert(media).asSingleTransaction(true).execute();
        return media;
    }

    public static int deleteMedia(@Nullable MediaModel media) {
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

    public static int deleteMatchingSiteMedia(
            @NonNull SiteModel siteModel,
            @NonNull String column,
            @NonNull Object value) {
        return WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(column, value)
                .endGroup().endWhere().execute();
    }

    @SuppressWarnings("unused")
    public static int deleteAllSiteMedia(@NonNull SiteModel site) {
        return WellSql.delete(MediaModel.class)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .endGroup().endWhere().execute();
    }

    public static void deleteAllUploadedSiteMedia(@NonNull SiteModel siteModel) {
        WellSql.delete(MediaModel.class)
               .where().beginGroup()
               .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
               .equals(MediaModelTable.UPLOAD_STATE, MediaUploadState.UPLOADED.toString())
               .endGroup().endWhere().execute();
    }

    public static void deleteAllUploadedSiteMediaWithMimeType(
            @NonNull SiteModel siteModel,
            @NonNull String mimeType) {
        WellSql.delete(MediaModel.class)
               .where().beginGroup()
               .equals(MediaModelTable.LOCAL_SITE_ID, siteModel.getId())
               .equals(MediaModelTable.UPLOAD_STATE, MediaUploadState.UPLOADED.toString())
               .contains(MediaModelTable.MIME_TYPE, mimeType)
               .endGroup().endWhere().execute();
    }

    public static void deleteAllMedia() {
        WellSql.delete(MediaModel.class).execute();
    }

    public static void deleteUploadedSiteMediaNotInList(
            @NonNull SiteModel site,
            @NonNull List<MediaModel> mediaList,
            @NonNull String mimeType) {
        if (mediaList.isEmpty()) {
            if (!TextUtils.isEmpty(mimeType)) {
                MediaSqlUtils.deleteAllUploadedSiteMediaWithMimeType(site, mimeType);
            } else {
                MediaSqlUtils.deleteAllUploadedSiteMedia(site);
            }
            return;
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

        builder.endGroup().endWhere().execute();
    }

    @NonNull
    private static SelectQuery<MediaModel> getSiteMediaExcludingQuery(
            @NonNull SiteModel site,
            @NonNull String column,
            @NonNull Object value) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .not().equals(column, value)
                .equals(MediaModelTable.LOCAL_SITE_ID, site.getId())
                .endGroup().endWhere()
                .orderBy(MediaModelTable.UPLOAD_DATE, SelectQuery.ORDER_DESCENDING);
    }
}
