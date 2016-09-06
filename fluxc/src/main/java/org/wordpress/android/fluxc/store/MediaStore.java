package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import com.wellsql.generated.MediaModelTable;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;
import org.wordpress.android.fluxc.network.MediaNetworkListener;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStore extends Store implements MediaNetworkListener {
    //
    // Payloads
    //

    /**
     * Used for FETCH_ALL_MEDIA and FETCH_MEDIA actions
     */
    public static class FetchMediaPayload extends Payload {
        public SiteModel site;
        public List<MediaModel> media;
        public FetchMediaPayload(SiteModel site, List<MediaModel> media) {
            this.site = site;
            this.media = media;
        }
    }

    /**
     * Used for DELETE_MEDIA, REMOVE_MEDIA, PUSH_MEDIA, and UPDATE_MEDIA actions
     */
    public static class ChangeMediaPayload extends Payload {
        public SiteModel site;
        public List<MediaModel> media;
        public ChangeMediaPayload(SiteModel site, List<MediaModel> media) {
            this.site = site;
            this.media = media;
        }
    }

    /**
     * Used for UPLOAD_MEDIA action
     */
    public static class UploadMediaPayload extends Payload {
        public SiteModel site;
        public MediaModel media;
        public UploadMediaPayload(SiteModel site, MediaModel media) {
            this.site = site;
            this.media = media;
        }
    }

    //
    // Errors
    //

    public enum MediaErrorType {
        NONE,
        NULL_MEDIA_ARG,
        MALFORMED_MEDIA_ARG,
        MEDIA_NOT_FOUND,
        GENERIC_ERROR
    }

    public static class MediaError implements OnChangedError {
        public MediaErrorType type;
        public MediaError(MediaErrorType type) {
            this.type = type;
        }
    }

    //
    // OnChanged events
    //

    public class OnMediaChanged extends OnChanged<MediaError> {
        public MediaAction cause;
        public List<MediaModel> media;
        public OnMediaChanged(MediaAction cause, List<MediaModel> media) {
            this.cause = cause;
            this.media = media;
        }
    }

    public class OnMediaUploaded extends OnChanged<MediaError> {
        public MediaModel media;
        public float progress;
        public OnMediaUploaded(MediaModel media, float progress) {
            this.media = media;
            this.progress = progress;
        }
    }

    private MediaRestClient mMediaRestClient;
    private MediaXMLRPCClient mMediaXmlrpcClient;

    @Inject
    public MediaStore(Dispatcher dispatcher, MediaRestClient restClient, MediaXMLRPCClient xmlrpcClient) {
        super(dispatcher);
        mMediaRestClient = restClient;
        mMediaRestClient.setListener(this);
        mMediaXmlrpcClient = xmlrpcClient;
        mMediaXmlrpcClient.setListener(this);
    }

    @Subscribe
    @Override
    public void onAction(Action action) {
        if (action.getType() == MediaAction.PUSH_MEDIA) {
            performPushMedia((ChangeMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.UPLOAD_MEDIA) {
            performUploadMedia((UploadMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.FETCH_ALL_MEDIA) {
            performFetchAllMedia((FetchMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.FETCH_MEDIA) {
            performFetchMedia((FetchMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.DELETE_MEDIA) {
            performDeleteMedia((ChangeMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.UPDATE_MEDIA) {
            ChangeMediaPayload payload = (ChangeMediaPayload) action.getPayload();
            updateMedia(payload.media, true);
        } else if (action.getType() == MediaAction.REMOVE_MEDIA) {
            ChangeMediaPayload payload = (ChangeMediaPayload) action.getPayload();
            removeMedia(payload.media);
        }
    }

    @Override
    public void onRegister() {
    }

    @Override
    public void onMediaError(MediaAction cause, MediaModel media, MediaNetworkError error) {
        AppLog.d(AppLog.T.MEDIA, cause + " caused exception: " + error);

        if (error == MediaNetworkError.MEDIA_NOT_FOUND) {
            notifyMediaError(MediaErrorType.MEDIA_NOT_FOUND, cause, media);
        } else {
            notifyMediaError(MediaErrorType.GENERIC_ERROR, cause, media);
        }
    }

    @Override
    public void onMediaFetched(MediaAction cause, List<MediaModel> fetchedMedia) {
        if (cause == MediaAction.FETCH_ALL_MEDIA || cause == MediaAction.FETCH_MEDIA) {
            updateMedia(fetchedMedia, false);
            emitChange(new OnMediaChanged(cause, fetchedMedia));
        }
    }

    @Override
    public void onMediaPushed(MediaAction cause, List<MediaModel> pushedMedia) {
        updateMedia(pushedMedia, false);
        emitChange(new OnMediaChanged(cause, pushedMedia));
    }

    @Override
    public void onMediaDeleted(MediaAction cause, List<MediaModel> deletedMedia) {
        if (cause == MediaAction.DELETE_MEDIA) {
            emitChange(new OnMediaChanged(cause, deletedMedia));
        }
    }

    @Override
    public void onMediaUploadProgress(MediaAction cause, MediaModel media, float progress) {
        AppLog.v(AppLog.T.MEDIA, "Progress update on upload of " + media.getTitle() + ": " + progress);
        emitChange(new OnMediaUploaded(media, progress));
    }

    public List<MediaModel> getAllSiteMedia(long siteId) {
        return MediaSqlUtils.getAllSiteMedia(siteId);
    }

    public int getSiteMediaCount(long siteId) {
        return getAllSiteMedia(siteId).size();
    }

    public boolean hasSiteMediaWithId(long siteId, long mediaId) {
        return getSiteMediaWithId(siteId, mediaId) != null;
    }

    public MediaModel getSiteMediaWithId(long siteId, long mediaId) {
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(siteId, mediaId);
        return media.size() > 0 ? media.get(0) : null;
    }

    public List<MediaModel> getSiteMediaWithIds(long siteId, List<Long> mediaIds) {
        return MediaSqlUtils.getSiteMediaWithIds(siteId, mediaIds);
    }

    public List<MediaModel> getSiteImages(long siteId) {
        return MediaSqlUtils.getSiteImages(siteId);
    }

    public int getSiteImageCount(long siteId) {
        return getSiteImages(siteId).size();
    }

    public List<MediaModel> getSiteImagesExcludingIds(long siteId, List<Long> filter) {
        return MediaSqlUtils.getSiteImagesExcluding(siteId, filter);
    }

    public List<MediaModel> getUnattachedSiteMedia(long siteId) {
        return MediaSqlUtils.matchSiteMedia(siteId, MediaModelTable.POST_ID, 0);
    }

    public int getUnattachedSiteMediaCount(long siteId) {
        return getUnattachedSiteMedia(siteId).size();
    }

    public List<MediaModel> getLocalSiteMedia(long siteId) {
        MediaModel.UploadState expectedState = MediaModel.UploadState.UPLOADED;
        return MediaSqlUtils.getSiteMediaExcluding(siteId, MediaModelTable.UPLOAD_STATE, expectedState);
    }

    public String getUrlForSiteVideoWithVideoPressGuid(long siteId, String videoPressGuid) {
        List<MediaModel> media =
                MediaSqlUtils.matchSiteMedia(siteId, MediaModelTable.VIDEO_PRESS_GUID, videoPressGuid);
        return media.size() > 0 ? media.get(0).getUrl() : null;
    }

    public String getThumbnailUrlForSiteMediaWithId(long siteId, long mediaId) {
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(siteId, mediaId);
        return media.size() > 0 ? media.get(0).getThumbnailUrl() : null;
    }

    public List<MediaModel> searchSiteMediaByTitle(long siteId, String titleSearch) {
        return MediaSqlUtils.searchSiteMedia(siteId, MediaModelTable.TITLE, titleSearch);
    }

    public MediaModel getPostMediaWithPath(long postId, String filePath) {
        List<MediaModel> media = MediaSqlUtils.matchPostMedia(postId, MediaModelTable.FILE_PATH, filePath);
        return media.size() > 0 ? media.get(0) : null;
    }

    public MediaModel getNextSiteMediaToDelete(long siteId) {
        List<MediaModel> media = MediaSqlUtils.matchSiteMedia(siteId,
                MediaModelTable.UPLOAD_STATE, MediaModel.UploadState.DELETE.toString());
        return media.size() > 0 ? media.get(0) : null;
    }

    public boolean hasSiteMediaToDelete(long siteId) {
        return getNextSiteMediaToDelete(siteId) != null;
    }

    //
    // Action implementations
    //

    private void updateMedia(List<MediaModel> media, boolean emit) {
        if (media == null || media.isEmpty()) return;

        OnMediaChanged event = new OnMediaChanged(MediaAction.UPDATE_MEDIA, new ArrayList<MediaModel>());
        for (MediaModel mediaItem : media) {
            if (MediaSqlUtils.insertOrUpdateMedia(mediaItem) > 0) {
                event.media.add(mediaItem);
            }
        }
        if (emit) emitChange(event);
    }

    private void removeMedia(List<MediaModel> media) {
        if (media == null || media.isEmpty()) return;

        OnMediaChanged event = new OnMediaChanged(MediaAction.REMOVE_MEDIA, new ArrayList<MediaModel>());
        for (MediaModel mediaItem : media) {
            if (MediaSqlUtils.deleteMedia(mediaItem) > 0) {
                event.media.add(mediaItem);
            }
        }
        emitChange(event);
    }

    //
    // Helper methods that choose the appropriate network client to perform an action
    //

    private void performPushMedia(ChangeMediaPayload payload) {
        if (payload.media == null || payload.media.isEmpty() || payload.media.contains(null)) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.PUSH_MEDIA, payload.media);
            return;
        }

        if (payload.site.isWPCom()) {
            mMediaRestClient.pushMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.pushMedia(payload.site, payload.media);
        }
    }

    private void performUploadMedia(UploadMediaPayload payload) {
        if (payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.UPLOAD_MEDIA, payload.media);
            return;
        } else if (!isWellFormedForUpload(payload.media)) {
            // list contained media items with insufficient data
            notifyMediaError(MediaErrorType.MALFORMED_MEDIA_ARG, MediaAction.UPLOAD_MEDIA, payload.media);
            return;
        }

        if (payload.site.isWPCom()) {
            mMediaRestClient.uploadMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.uploadMedia(payload.site, payload.media);
        }
    }

    private void performFetchAllMedia(FetchMediaPayload payload) {
        if (payload.site.isWPCom()) {
            mMediaRestClient.fetchAllMedia(payload.site);
        } else {
            mMediaXmlrpcClient.fetchAllMedia(payload.site);
        }
    }

    private void performFetchMedia(FetchMediaPayload payload) {
        if (payload.media == null || payload.media.isEmpty() || payload.media.contains(null)) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.FETCH_MEDIA, payload.media);
            return;
        }

        if (payload.site.isWPCom()) {
            mMediaRestClient.fetchMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.fetchMedia(payload.site, payload.media);
        }
    }

    private void performDeleteMedia(ChangeMediaPayload payload) {
        if (payload.media == null || payload.media.isEmpty() || payload.media.contains(null)) {
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.DELETE_MEDIA, payload.media);
            return;
        }

        if (payload.site.isWPCom()) {
            mMediaRestClient.deleteMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.deleteMedia(payload.site, payload.media);
        }
    }

    private boolean isWellFormedForUpload(@NonNull MediaModel media) {
        return BaseUploadRequestBody.hasRequiredData(media) == null;
    }

    private void notifyMediaError(MediaErrorType errorType, MediaAction cause, List<MediaModel> media) {
        OnMediaChanged mediaChange = new OnMediaChanged(cause, media);
        mediaChange.error = new MediaError(errorType);
        emitChange(mediaChange);
    }

    private void notifyMediaError(MediaErrorType errorType, MediaAction cause, MediaModel media) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        notifyMediaError(errorType, cause, mediaList);
    }
}