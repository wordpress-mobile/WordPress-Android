package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import com.wellsql.generated.MediaModelTable;
import com.yarolegovich.wellsql.WellCursor;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStore extends Store {
    public static class MediaFilter {
        public static final int MAX_NUMBER          = 100;
        public static final long UNATTACHED_POST_ID = 0;

        public enum SortOrder {
            DESCENDING, ASCENDING
        }

        public enum SortField {
            DATE, TITLE, ID
        }

        public List<String> fields;
        public int number;
        public long postId;
        public int offset;
        public int page;
        public SortOrder sortOrder;
        public SortField sortField;
        public String searchQuery;
        public String after;
        public String before;
        public String mimeType;
    }

    //
    // Payloads
    //

    /**
     * Actions: FETCH(ED)_MEDIA, PUSH(ED)_MEDIA, UPLOAD_MEDIA, DELETE(D)_MEDIA, UPDATE_MEDIA, and REMOVE_MEDIA
     */
    public static class MediaPayload extends Payload {
        public SiteModel site;
        public MediaError error;
        public MediaModel media;
        public MediaPayload(SiteModel site, MediaModel media) {
            this(site, media, null);
        }
        public MediaPayload(SiteModel site, MediaModel media, MediaError error) {
            this.site = site;
            this.media = media;
            this.error = error;
        }
    }

    /**
     * Actions: FETCH(ED)_ALL_MEDIA, PUSH(ED)_MEDIA, DELETE(D)_MEDIA, UPDATE_MEDIA, and REMOVE_MEDIA
     */
    public static class MediaListPayload extends Payload {
        public SiteModel site;
        public MediaError error;
        public List<MediaModel> media;
        public MediaFilter filter;
        public MediaListPayload(SiteModel site, List<MediaModel> media, MediaFilter filter) {
            this(site, media, null, filter);
        }
        public MediaListPayload(SiteModel site, List<MediaModel> media, MediaError error, MediaFilter filter) {
            this.site = site;
            this.media = media;
            this.error = error;
            this.filter = filter;
        }

        @Override
        public boolean isError() {
            return error != null;
        }
    }

    /**
     * Actions: UPLOADED_MEDIA
     */
    public static class ProgressPayload extends Payload {
        public MediaModel media;
        public float progress;
        public boolean completed;
        public MediaError error;
        public ProgressPayload(MediaModel media, float progress, boolean completed) {
            this(media, progress, completed, null);
        }
        public ProgressPayload(MediaModel media, float progress, boolean completed, MediaError error) {
            this.media = media;
            this.progress = progress;
            this.completed = completed;
            this.error = error;
        }

        @Override
        public boolean isError() {
            return error != null;
        }
    }

    //
    // OnChanged events
    //

    public static class MediaError implements OnChangedError {
        public MediaErrorType type;
        public String message;
        public MediaError(MediaErrorType type) {
            this.type = type;
        }
        public MediaError(MediaErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public class OnMediaChanged extends OnChanged<MediaError> {
        public MediaAction cause;
        public List<MediaModel> media;
        public OnMediaChanged(MediaAction cause, List<MediaModel> media) {
            this(cause, media, null);
        }
        public OnMediaChanged(MediaAction cause, List<MediaModel> media, MediaError error) {
            this.cause = cause;
            this.media = media;
            this.error = error;
        }
    }

    public class OnMediaUploaded extends OnChanged<MediaError> {
        public MediaModel media;
        public float progress;
        public boolean completed;
        public OnMediaUploaded(MediaModel media, float progress, boolean completed) {
            this.media = media;
            this.progress = progress;
            this.completed = completed;
        }
    }

    //
    // Errors
    //

    public enum MediaErrorType {
        // local errors, occur before sending network requests
        FS_READ_PERMISSION_DENIED,
        NULL_MEDIA_ARG,
        MALFORMED_MEDIA_ARG,

        // network errors, occur in response to network requests
        MEDIA_NOT_FOUND,
        UNAUTHORIZED,
        PARSE_ERROR,

        // unknown/unspecified
        GENERIC_ERROR;

        public static MediaErrorType fromBaseNetworkError(BaseRequest.BaseNetworkError baseError) {
            switch (baseError.type) {
                case NOT_FOUND:
                    return MediaErrorType.MEDIA_NOT_FOUND;
                case AUTHORIZATION_REQUIRED:
                    return MediaErrorType.UNAUTHORIZED;
                case PARSE_ERROR:
                    return MediaErrorType.PARSE_ERROR;
                default:
                    return MediaErrorType.GENERIC_ERROR;
            }
        }

        public static MediaErrorType fromHttpStatusCode(int code) {
            switch (code) {
                case 404:
                    return MediaErrorType.MEDIA_NOT_FOUND;
                case 403:
                    return MediaErrorType.UNAUTHORIZED;
                default:
                    return MediaErrorType.GENERIC_ERROR;
            }
        }
    }

    private MediaRestClient mMediaRestClient;
    private MediaXMLRPCClient mMediaXmlrpcClient;

    @Inject
    public MediaStore(Dispatcher dispatcher, MediaRestClient restClient, MediaXMLRPCClient xmlrpcClient) {
        super(dispatcher);
        mMediaRestClient = restClient;
        mMediaXmlrpcClient = xmlrpcClient;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof MediaAction)) {
            return;
        }

        switch ((MediaAction) actionType) {
            case PUSH_MEDIA:
                performPushMedia((MediaPayload) action.getPayload());
                break;
            case UPLOAD_MEDIA:
                performUploadMedia((MediaPayload) action.getPayload());
                break;
            case FETCH_ALL_MEDIA:
                performFetchAllMedia((MediaListPayload) action.getPayload());
                break;
            case FETCH_MEDIA:
                performFetchMedia((MediaPayload) action.getPayload());
                break;
            case DELETE_MEDIA:
                performDeleteMedia((MediaPayload) action.getPayload());
                break;
            case CANCEL_MEDIA_UPLOAD:
                performCancelUpload((MediaPayload) action.getPayload());
                break;
            case PUSHED_MEDIA:
                handleMediaPushed((MediaPayload) action.getPayload());
                break;
            case UPLOADED_MEDIA:
                handleMediaUploaded((ProgressPayload) action.getPayload());
                break;
            case FETCHED_ALL_MEDIA:
                handleAllMediaFetched((MediaListPayload) action.getPayload());
                break;
            case FETCHED_MEDIA:
                handleMediaFetched((MediaPayload) action.getPayload());
                break;
            case DELETED_MEDIA:
                handleMediaDeleted((MediaPayload) action.getPayload());
                break;
            case CANCELED_MEDIA_UPLOAD:
                handleMediaUploaded((ProgressPayload) action.getPayload());
                break;
            case UPDATE_MEDIA:
                updateMedia(((MediaModel) action.getPayload()), true);
                break;
            case REMOVE_MEDIA:
                removeMedia(((MediaModel) action.getPayload()));
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.MEDIA, "MediaStore onRegister");
    }

    //
    // Getters
    //

    public List<MediaModel> getAllSiteMedia(SiteModel siteModel) {
        return MediaSqlUtils.getAllSiteMedia(siteModel);
    }

    public WellCursor<MediaModel> getAllSiteMediaAsCursor(SiteModel siteModel) {
        return MediaSqlUtils.getAllSiteMediaAsCursor(siteModel);
    }

    public int getSiteMediaCount(SiteModel siteModel) {
        return getAllSiteMedia(siteModel).size();
    }

    public boolean hasSiteMediaWithId(SiteModel siteModel, long mediaId) {
        return getSiteMediaWithId(siteModel, mediaId) != null;
    }

    public MediaModel getSiteMediaWithId(SiteModel siteModel, long mediaId) {
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(siteModel, mediaId);
        return media.size() > 0 ? media.get(0) : null;
    }

    public List<MediaModel> getSiteMediaWithIds(SiteModel siteModel, List<Long> mediaIds) {
        return MediaSqlUtils.getSiteMediaWithIds(siteModel, mediaIds);
    }

    public WellCursor<MediaModel> getSiteMediaWithIdsAsCursor(SiteModel siteModel, List<Long> mediaIds) {
        return MediaSqlUtils.getSiteMediaWithIdsAsCursor(siteModel, mediaIds);
    }

    public List<MediaModel> getSiteImages(SiteModel siteModel) {
        return MediaSqlUtils.getSiteImages(siteModel);
    }

    public WellCursor<MediaModel> getSiteImagesAsCursor(SiteModel siteModel) {
        return MediaSqlUtils.getSiteImagesAsCursor(siteModel);
    }

    public int getSiteImageCount(SiteModel siteModel) {
        return getSiteImages(siteModel).size();
    }

    public List<MediaModel> getSiteImagesExcludingIds(SiteModel siteModel, List<Long> filter) {
        return MediaSqlUtils.getSiteImagesExcluding(siteModel, filter);
    }

    public WellCursor<MediaModel> getSiteImagesExcludingIdsAsCursor(SiteModel siteModel, List<Long> filter) {
        return MediaSqlUtils.getSiteImagesExcludingAsCursor(siteModel, filter);
    }

    public List<MediaModel> getUnattachedSiteMedia(SiteModel siteModel) {
        return MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.POST_ID, 0);
    }

    public WellCursor<MediaModel> getUnattachedSiteMediaAsCursor(SiteModel siteModel) {
        return MediaSqlUtils.matchSiteMediaAsCursor(siteModel, MediaModelTable.POST_ID, 0);
    }

    public int getUnattachedSiteMediaCount(SiteModel siteModel) {
        return getUnattachedSiteMedia(siteModel).size();
    }

    public List<MediaModel> getLocalSiteMedia(SiteModel siteModel) {
        MediaModel.UploadState expectedState = MediaModel.UploadState.UPLOADED;
        return MediaSqlUtils.getSiteMediaExcluding(siteModel, MediaModelTable.UPLOAD_STATE, expectedState);
    }

    public String getUrlForSiteVideoWithVideoPressGuid(SiteModel siteModel, String videoPressGuid) {
        List<MediaModel> media =
                MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.VIDEO_PRESS_GUID, videoPressGuid);
        return media.size() > 0 ? media.get(0).getUrl() : null;
    }

    public String getThumbnailUrlForSiteMediaWithId(SiteModel siteModel, long mediaId) {
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(siteModel, mediaId);
        return media.size() > 0 ? media.get(0).getThumbnailUrl() : null;
    }

    public List<MediaModel> searchSiteMediaByTitle(SiteModel siteModel, String titleSearch) {
        return MediaSqlUtils.searchSiteMedia(siteModel, MediaModelTable.TITLE, titleSearch);
    }

    public WellCursor<MediaModel> searchSiteMediaByTitleAsCursor(SiteModel siteModel, String titleSearch) {
        return MediaSqlUtils.searchSiteMediaAsCursor(siteModel, MediaModelTable.TITLE, titleSearch);
    }

    public MediaModel getPostMediaWithPath(long postId, String filePath) {
        List<MediaModel> media = MediaSqlUtils.matchPostMedia(postId, MediaModelTable.FILE_PATH, filePath);
        return media.size() > 0 ? media.get(0) : null;
    }

    public MediaModel getNextSiteMediaToDelete(SiteModel siteModel) {
        List<MediaModel> media = MediaSqlUtils.matchSiteMedia(siteModel,
                MediaModelTable.UPLOAD_STATE, MediaModel.UploadState.DELETE.toString());
        return media.size() > 0 ? media.get(0) : null;
    }

    public boolean hasSiteMediaToDelete(SiteModel siteModel) {
        return getNextSiteMediaToDelete(siteModel) != null;
    }

    //
    // Action implementations
    //

    private void updateMedia(MediaModel media, boolean emit) {
        OnMediaChanged event = new OnMediaChanged(MediaAction.UPDATE_MEDIA, new ArrayList<MediaModel>());

        if (media == null) {
            if (emit) {
                event.error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
                emitChange(event);
            }
            return;
        }

        if (MediaSqlUtils.insertOrUpdateMedia(media) > 0) {
            event.media.add(media);
        }
        if (emit) emitChange(event);
    }

    private void removeMedia(MediaModel media) {
        OnMediaChanged event = new OnMediaChanged(MediaAction.REMOVE_MEDIA, new ArrayList<MediaModel>());

        if (media == null) {
            event.error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            emitChange(event);
            return;
        }

        if (MediaSqlUtils.deleteMedia(media) > 0) {
            event.media.add(media);
        }
        emitChange(event);
    }

    //
    // Helper methods that choose the appropriate network client to perform an action
    //

    private void performPushMedia(MediaPayload payload) {
        if (payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.PUSH_MEDIA, null);
            return;
        } else if (payload.media.getMediaId() < 0) {
            // need media ID to push changes
            notifyMediaError(MediaErrorType.MALFORMED_MEDIA_ARG, MediaAction.PUSH_MEDIA, payload.media);
            return;
        }

        if (payload.site.isWPCom()) {
            mMediaRestClient.pushMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.pushMedia(payload.site, payload.media);
        }
    }

    private void performUploadMedia(MediaPayload payload) {
        if (payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.UPLOAD_MEDIA, null);
            return;
        } else {
            String errorMessage = isWellFormedForUpload(payload.media);
            if (errorMessage != null) {
                notifyMediaError(MediaErrorType.MALFORMED_MEDIA_ARG, errorMessage, MediaAction.UPLOAD_MEDIA,
                        payload.media);
                return;
            }
        }

        if (payload.site.isWPCom()) {
            mMediaRestClient.uploadMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.uploadMedia(payload.site, payload.media);
        }
    }

    private void performFetchAllMedia(MediaListPayload payload) {
        if (payload.site.isWPCom()) {
            mMediaRestClient.fetchAllMedia(payload.site, payload.filter);
        } else {
            mMediaXmlrpcClient.fetchAllMedia(payload.site, payload.filter);
        }
    }

    private void performFetchMedia(MediaPayload payload) {
        if (payload.site == null || payload.media == null) {
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

    private void performDeleteMedia(@NonNull MediaPayload payload) {
        if (payload.media == null) {
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.DELETE_MEDIA, null);
            return;
        }

        if (payload.site.isWPCom()) {
            mMediaRestClient.deleteMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.deleteMedia(payload.site, payload.media);
        }
    }

    private void performCancelUpload(@NonNull MediaPayload payload) {
        if (payload.media != null) {
            if (payload.site.isWPCom()) {
                mMediaRestClient.cancelUpload(payload.media);
            } else {
                mMediaXmlrpcClient.cancelUpload(payload.media);
            }
        }
    }

    private void handleMediaPushed(@NonNull MediaPayload payload) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(payload.media);
        OnMediaChanged onMediaChanged = new OnMediaChanged(MediaAction.PUSH_MEDIA, mediaList);
        if (payload.isError()) {
            onMediaChanged.error = payload.error;
        } else {
            updateMedia(payload.media, false);
        }
        emitChange(onMediaChanged);
    }

    private void handleMediaUploaded(@NonNull ProgressPayload payload) {
        if (!payload.isError() && payload.completed) {
            updateMedia(payload.media, false);
        }
        OnMediaUploaded onMediaUploaded = new OnMediaUploaded(payload.media, payload.progress, payload.completed);
        onMediaUploaded.error = payload.error;
        emitChange(onMediaUploaded);
    }

    private void handleAllMediaFetched(@NonNull MediaListPayload payload) {
        OnMediaChanged onMediaChanged = new OnMediaChanged(MediaAction.FETCH_ALL_MEDIA, payload.media);

        if (!payload.isError() && !payload.media.isEmpty()) {
            for (MediaModel media : payload.media) {
                updateMedia(media, false);
            }
        }

        emitChange(onMediaChanged);
    }

    private void handleMediaFetched(@NonNull MediaPayload payload) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(payload.media);
        OnMediaChanged onMediaChanged = new OnMediaChanged(MediaAction.FETCH_MEDIA, mediaList);

        if (payload.isError()) {
            onMediaChanged.error = payload.error;
        } else if (payload.media != null) {
            MediaSqlUtils.insertOrUpdateMedia(payload.media);
        }

        emitChange(onMediaChanged);
    }

    private void handleMediaDeleted(@NonNull MediaPayload payload) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(payload.media);
        OnMediaChanged onMediaChanged = new OnMediaChanged(MediaAction.DELETE_MEDIA, mediaList);

        if (payload.isError()) {
            onMediaChanged.error = payload.error;
        } else if (payload.media != null) {
            MediaSqlUtils.deleteMedia(payload.media);
        }

        emitChange(onMediaChanged);
    }

    private String isWellFormedForUpload(@NonNull MediaModel media) {
        String error = BaseUploadRequestBody.hasRequiredData(media);
        if (error != null) {
            AppLog.e(AppLog.T.MEDIA, "Media doesn't have required data: " + error);
        }
        return error;
    }

    private void notifyMediaError(MediaErrorType errorType, String errorMessage, MediaAction cause,
                                  List<MediaModel> media) {
        OnMediaChanged mediaChange = new OnMediaChanged(cause, media);
        mediaChange.error = new MediaError(errorType, errorMessage);
        emitChange(mediaChange);
    }

    private void notifyMediaError(MediaErrorType errorType, MediaAction cause, MediaModel media) {
        notifyMediaError(errorType, null, cause, media);
    }

    private void notifyMediaError(MediaErrorType errorType, String errorMessage, MediaAction cause, MediaModel media) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        notifyMediaError(errorType, errorMessage, cause, mediaList);
    }
}
