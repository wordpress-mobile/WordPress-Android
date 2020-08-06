package org.wordpress.android.fluxc.store;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wellsql.generated.MediaModelTable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.MimeType;
import org.wordpress.android.util.AppLog;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStore extends Store {
    public static final int DEFAULT_NUM_MEDIA_PER_FETCH = 50;

    //
    // Payloads
    //

    /**
     * Actions: FETCH(ED)_MEDIA, PUSH(ED)_MEDIA, UPLOADED_MEDIA, DELETE(D)_MEDIA, UPDATE_MEDIA, and REMOVE_MEDIA
     */
    public static class MediaPayload extends Payload<MediaError> {
        public SiteModel site;
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
     * Action: UPLOAD_MEDIA
     */
    public static class UploadMediaPayload extends MediaPayload {
        public final boolean stripLocation;

        public UploadMediaPayload(SiteModel site, MediaModel media, boolean stripLocation) {
            super(site, media, null);
            this.stripLocation = stripLocation;
        }

        public UploadMediaPayload(SiteModel site, MediaModel media, MediaError error, boolean stripLocation) {
            super(site, media, error);
            this.stripLocation = stripLocation;
        }
    }

    /**
     * Actions: FETCH_MEDIA_LIST
     */
    public static class FetchMediaListPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public boolean loadMore;
        public MimeType.Type mimeType;
        public int number = DEFAULT_NUM_MEDIA_PER_FETCH;

        public FetchMediaListPayload(SiteModel site) {
            this.site = site;
        }

        public FetchMediaListPayload(SiteModel site, int number, boolean loadMore) {
            this.site = site;
            this.loadMore = loadMore;
            this.number = number;
        }

        public FetchMediaListPayload(SiteModel site, int number, boolean loadMore, MimeType.Type mimeType) {
            this.site = site;
            this.loadMore = loadMore;
            this.mimeType = mimeType;
            this.number = number;
        }
    }

    /**
     * Actions: FETCHED_MEDIA_LIST
     */
    public static class FetchMediaListResponsePayload extends Payload<MediaError> {
        public SiteModel site;
        public List<MediaModel> mediaList;
        public boolean loadedMore;
        public boolean canLoadMore;
        public MimeType.Type mimeType;
        public FetchMediaListResponsePayload(SiteModel site,
                                             @NonNull List<MediaModel> mediaList,
                                             boolean loadedMore,
                                             boolean canLoadMore,
                                             MimeType.Type mimeType) {
            this.site = site;
            this.mediaList = mediaList;
            this.loadedMore = loadedMore;
            this.canLoadMore = canLoadMore;
            this.mimeType = mimeType;
        }

        public FetchMediaListResponsePayload(SiteModel site, MediaError error, MimeType.Type mimeType) {
            this.mediaList = new ArrayList<>();
            this.site = site;
            this.error = error;
            this.mimeType = mimeType;
        }
    }

    /**
     * Actions: UPLOADED_MEDIA, CANCELED_MEDIA_UPLOAD
     */
    public static class ProgressPayload extends Payload<MediaError> {
        public MediaModel media;
        public float progress;
        public boolean completed;
        public boolean canceled;
        public ProgressPayload(MediaModel media, float progress, boolean completed, boolean canceled) {
            this(media, progress, completed, null);
            this.canceled = canceled;
        }
        public ProgressPayload(MediaModel media, float progress, boolean completed, MediaError error) {
            this.media = media;
            this.progress = progress;
            this.completed = completed;
            this.error = error;
        }
    }

    /**
     * Actions: CANCEL_MEDIA_UPLOAD
     */
    public static class CancelMediaPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public MediaModel media;
        public boolean delete;

        public CancelMediaPayload(SiteModel site, MediaModel media) {
            this(site, media, true);
        }

        public CancelMediaPayload(SiteModel site, MediaModel media, boolean delete) {
            this.site = site;
            this.media = media;
            this.delete = delete;
        }
    }

    /**
     * Actions: UPLOAD_STOCK_MEDIA
     */
    @SuppressWarnings("WeakerAccess")
    public static class UploadStockMediaPayload extends Payload<BaseNetworkError> {
        public @NonNull List<StockMediaModel> stockMediaList;
        public @NonNull SiteModel site;

        public UploadStockMediaPayload(@NonNull SiteModel site, @NonNull List<StockMediaModel> stockMediaList) {
            this.stockMediaList = stockMediaList;
            this.site = site;
        }
    }

    /**
     * Actions: UPLOADED_STOCK_MEDIA
     */
    @SuppressWarnings("WeakerAccess")
    public static class UploadedStockMediaPayload extends Payload<UploadStockMediaError> {
        @NonNull public List<MediaModel> mediaList;
        @NonNull public SiteModel site;

        public UploadedStockMediaPayload(@NonNull SiteModel site, @NonNull List<MediaModel> mediaList) {
            this.site = site;
            this.mediaList = mediaList;
        }

        public UploadedStockMediaPayload(@NonNull SiteModel site, @NonNull UploadStockMediaError error) {
            this.site = site;
            this.error = error;
            this.mediaList = new ArrayList<>();
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

        public static MediaError fromIOException(IOException e) {
            MediaError mediaError = new MediaError(MediaErrorType.GENERIC_ERROR);
            mediaError.message = e.getLocalizedMessage();

            if (e instanceof SocketTimeoutException) {
                mediaError.type = MediaErrorType.TIMEOUT;
            }

            if (e instanceof ConnectException || e instanceof UnknownHostException) {
                mediaError.type = MediaErrorType.CONNECTION_ERROR;
            }

            String errorMessage = e.getMessage();
            if (TextUtils.isEmpty(errorMessage)) {
                return mediaError;
            }

            errorMessage = errorMessage.toLowerCase(Locale.US);
            if (errorMessage.contains("broken pipe") || errorMessage.contains("epipe")) {
                // do not use the real error message.
                mediaError.message = "";
            }

            return mediaError;
        }
    }

    public static class UploadStockMediaError implements OnChangedError {
        public UploadStockMediaErrorType type;
        public String message;
        public UploadStockMediaError(UploadStockMediaErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class OnMediaChanged extends OnChanged<MediaError> {
        public MediaAction cause;
        public List<MediaModel> mediaList;
        public OnMediaChanged(MediaAction cause) {
            this(cause, new ArrayList<MediaModel>(), null);
        }
        public OnMediaChanged(MediaAction cause, @NonNull List<MediaModel> mediaList) {
            this(cause, mediaList, null);
        }
        public OnMediaChanged(MediaAction cause, MediaError error) {
            this(cause, new ArrayList<MediaModel>(), error);
        }
        public OnMediaChanged(MediaAction cause, @NonNull List<MediaModel> mediaList, MediaError error) {
            this.cause = cause;
            this.mediaList = mediaList;
            this.error = error;
        }
    }

    public static class OnMediaListFetched extends OnChanged<MediaError> {
        public SiteModel site;
        public boolean canLoadMore;
        public MimeType.Type mimeType;
        public OnMediaListFetched(SiteModel site, boolean canLoadMore, MimeType.Type mimeType) {
            this.site = site;
            this.canLoadMore = canLoadMore;
            this.mimeType = mimeType;
        }
        public OnMediaListFetched(SiteModel site, MediaError error, MimeType.Type mimeType) {
            this.site = site;
            this.error = error;
            this.mimeType = mimeType;
        }
    }

    public static class OnMediaUploaded extends OnChanged<MediaError> {
        public MediaModel media;
        public float progress;
        public boolean completed;
        public boolean canceled;
        public OnMediaUploaded(MediaModel media, float progress, boolean completed, boolean canceled) {
            this.media = media;
            this.progress = progress;
            this.completed = completed;
            this.canceled = canceled;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnStockMediaUploaded extends OnChanged<UploadStockMediaError> {
        @NonNull public List<MediaModel> mediaList;
        @Nullable public SiteModel site;

        public OnStockMediaUploaded(@NonNull SiteModel site, @NonNull List<MediaModel> mediaList) {
            this.site = site;
            this.mediaList = mediaList;
        }
        public OnStockMediaUploaded(@NonNull SiteModel site, @NonNull UploadStockMediaError error) {
            this.site = site;
            this.error = error;
            this.mediaList = new ArrayList<>();
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
        DB_QUERY_FAILURE,
        EXCEEDS_FILESIZE_LIMIT,
        EXCEEDS_MEMORY_LIMIT,
        EXCEEDS_SITE_SPACE_QUOTA_LIMIT,

        // network errors, occur in response to network requests
        AUTHORIZATION_REQUIRED,
        CONNECTION_ERROR,
        NOT_AUTHENTICATED,
        NOT_FOUND,
        PARSE_ERROR,
        REQUEST_TOO_LARGE,
        SERVER_ERROR, // this is also returned when PHP max_execution_time or memory_limit is reached
        TIMEOUT,

        // logic constraints errors
        INVALID_ID,

        // unknown/unspecified
        GENERIC_ERROR;

        public static MediaErrorType fromBaseNetworkError(BaseNetworkError baseError) {
            switch (baseError.type) {
                case NOT_FOUND:
                    return MediaErrorType.NOT_FOUND;
                case NOT_AUTHENTICATED:
                    return MediaErrorType.NOT_AUTHENTICATED;
                case AUTHORIZATION_REQUIRED:
                    return MediaErrorType.AUTHORIZATION_REQUIRED;
                case PARSE_ERROR:
                    return MediaErrorType.PARSE_ERROR;
                case SERVER_ERROR:
                    return MediaErrorType.SERVER_ERROR;
                case TIMEOUT:
                    return MediaErrorType.TIMEOUT;
                default:
                    return MediaErrorType.GENERIC_ERROR;
            }
        }

        public static MediaErrorType fromHttpStatusCode(int code) {
            switch (code) {
                case 404:
                    return MediaErrorType.NOT_FOUND;
                case 403:
                    return MediaErrorType.NOT_AUTHENTICATED;
                case 413:
                    return MediaErrorType.REQUEST_TOO_LARGE;
                case 500:
                    return MediaErrorType.SERVER_ERROR;
                default:
                    return MediaErrorType.GENERIC_ERROR;
            }
        }

        public static MediaErrorType fromString(String string) {
            if (string != null) {
                for (MediaErrorType v : MediaErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum UploadStockMediaErrorType {
        INVALID_INPUT,
        UNKNOWN,
        GENERIC_ERROR;

        public static UploadStockMediaErrorType fromNetworkError(WPComGsonNetworkError wpError) {
            // invalid upload request
            if (wpError.apiError.equalsIgnoreCase("invalid_input")) {
                return INVALID_INPUT;
            }
            // can happen if invalid pexels image url is passed
            if (wpError.type == BaseRequest.GenericErrorType.UNKNOWN) {
                return UNKNOWN;
            }
            return GENERIC_ERROR;
        }
    }

    private final MediaRestClient mMediaRestClient;
    private final MediaXMLRPCClient mMediaXmlrpcClient;
    // Ensures that the UploadStore is initialized whenever the MediaStore is,
    // to ensure actions are shadowed and repeated by the UploadStore
    @SuppressWarnings("unused")
    @Inject UploadStore mUploadStore;

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
                performUploadMedia((UploadMediaPayload) action.getPayload());
                break;
            case FETCH_MEDIA_LIST:
                performFetchMediaList((FetchMediaListPayload) action.getPayload());
                break;
            case FETCH_MEDIA:
                performFetchMedia((MediaPayload) action.getPayload());
                break;
            case DELETE_MEDIA:
                performDeleteMedia((MediaPayload) action.getPayload());
                break;
            case CANCEL_MEDIA_UPLOAD:
                performCancelUpload((CancelMediaPayload) action.getPayload());
                break;
            case PUSHED_MEDIA:
                handleMediaPushed((MediaPayload) action.getPayload());
                break;
            case UPLOADED_MEDIA:
                handleMediaUploaded((ProgressPayload) action.getPayload());
                break;
            case FETCHED_MEDIA_LIST:
                handleMediaListFetched((FetchMediaListResponsePayload) action.getPayload());
                break;
            case FETCHED_MEDIA:
                handleMediaFetched((MediaPayload) action.getPayload());
                break;
            case DELETED_MEDIA:
                handleMediaDeleted((MediaPayload) action.getPayload());
                break;
            case CANCELED_MEDIA_UPLOAD:
                handleMediaCanceled((ProgressPayload) action.getPayload());
                break;
            case UPDATE_MEDIA:
                updateMedia(((MediaModel) action.getPayload()), true);
                break;
            case REMOVE_MEDIA:
                removeMedia(((MediaModel) action.getPayload()));
                break;
            case REMOVE_ALL_MEDIA:
                removeAllMedia();
                break;
            case UPLOAD_STOCK_MEDIA:
                performUploadStockMedia((UploadStockMediaPayload) action.getPayload());
                break;
            case UPLOADED_STOCK_MEDIA:
                handleStockMediaUploaded(((UploadedStockMediaPayload) action.getPayload()));
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

    public MediaModel instantiateMediaModel() {
        MediaModel media = new MediaModel();

        media = MediaSqlUtils.insertMediaForResult(media);

        if (media.getId() == -1) {
            media = null;
        }

        return media;
    }

    public List<MediaModel> getAllSiteMedia(SiteModel siteModel) {
        return MediaSqlUtils.getAllSiteMedia(siteModel);
    }

    public static final List<String> NOT_DELETED_STATES = new ArrayList<>();
    static {
        NOT_DELETED_STATES.add(MediaUploadState.DELETING.toString());
        NOT_DELETED_STATES.add(MediaUploadState.FAILED.toString());
        NOT_DELETED_STATES.add(MediaUploadState.QUEUED.toString());
        NOT_DELETED_STATES.add(MediaUploadState.UPLOADED.toString());
        NOT_DELETED_STATES.add(MediaUploadState.UPLOADING.toString());
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

    public MediaModel getMediaWithLocalId(int localMediaId) {
        return MediaSqlUtils.getMediaWithLocalId(localMediaId);
    }

    public List<MediaModel> getSiteMediaWithIds(SiteModel siteModel, List<Long> mediaIds) {
        return MediaSqlUtils.getSiteMediaWithIds(siteModel, mediaIds);
    }

    public List<MediaModel> getSiteImages(SiteModel siteModel) {
        return MediaSqlUtils.getSiteImages(siteModel);
    }

    public List<MediaModel> getSiteVideos(SiteModel siteModel) {
        return MediaSqlUtils.getSiteVideos(siteModel);
    }

    public List<MediaModel> getSiteAudio(SiteModel siteModel) {
        return MediaSqlUtils.getSiteAudio(siteModel);
    }

    public List<MediaModel> getSiteDocuments(SiteModel siteModel) {
        return MediaSqlUtils.getSiteDocuments(siteModel);
    }

    public int getSiteImageCount(SiteModel siteModel) {
        return getSiteImages(siteModel).size();
    }

    public List<MediaModel> getSiteImagesExcludingIds(SiteModel siteModel, List<Long> filter) {
        return MediaSqlUtils.getSiteImagesExcluding(siteModel, filter);
    }

    public List<MediaModel> getUnattachedSiteMedia(SiteModel siteModel) {
        return MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.POST_ID, 0);
    }

    public int getUnattachedSiteMediaCount(SiteModel siteModel) {
        return getUnattachedSiteMedia(siteModel).size();
    }

    public List<MediaModel> getLocalSiteMedia(SiteModel siteModel) {
        MediaUploadState expectedState = MediaUploadState.UPLOADED;
        return MediaSqlUtils.getSiteMediaExcluding(siteModel, MediaModelTable.UPLOAD_STATE, expectedState);
    }

    public List<MediaModel> getSiteMediaWithState(SiteModel siteModel, MediaUploadState expectedState) {
        return MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.UPLOAD_STATE, expectedState);
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

    public List<MediaModel> searchSiteMedia(SiteModel siteModel, String searchTerm) {
        return MediaSqlUtils.searchSiteMedia(siteModel, searchTerm);
    }

    public List<MediaModel> searchSiteImages(SiteModel siteModel, String searchTerm) {
        return MediaSqlUtils.searchSiteImages(siteModel, searchTerm);
    }

    public List<MediaModel> searchSiteVideos(SiteModel siteModel, String searchTerm) {
        return MediaSqlUtils.searchSiteVideos(siteModel, searchTerm);
    }

    public List<MediaModel> searchSiteAudio(SiteModel siteModel, String searchTerm) {
        return MediaSqlUtils.searchSiteAudio(siteModel, searchTerm);
    }

    public List<MediaModel> searchSiteDocuments(SiteModel siteModel, String searchTerm) {
        return MediaSqlUtils.searchSiteDocuments(siteModel, searchTerm);
    }

    public MediaModel getMediaForPostWithPath(PostImmutableModel postModel, String filePath) {
        List<MediaModel> media = MediaSqlUtils.matchPostMedia(postModel.getId(), MediaModelTable.FILE_PATH, filePath);
        return media.size() > 0 ? media.get(0) : null;
    }

    public List<MediaModel> getMediaForPost(PostImmutableModel postModel) {
        return MediaSqlUtils.matchPostMedia(postModel.getId());
    }

    public List<MediaModel> getMediaForPostWithState(PostImmutableModel postModel, MediaUploadState expectedState) {
        return MediaSqlUtils.matchPostMedia(postModel.getId(), MediaModelTable.UPLOAD_STATE,
                expectedState);
    }

    public MediaModel getNextSiteMediaToDelete(SiteModel siteModel) {
        List<MediaModel> media = MediaSqlUtils.matchSiteMedia(siteModel,
                MediaModelTable.UPLOAD_STATE, MediaUploadState.DELETING.toString());
        return media.size() > 0 ? media.get(0) : null;
    }

    public boolean hasSiteMediaToDelete(SiteModel siteModel) {
        return getNextSiteMediaToDelete(siteModel) != null;
    }

    private void removeAllMedia() {
        MediaSqlUtils.deleteAllMedia();
        OnMediaChanged event = new OnMediaChanged(MediaAction.REMOVE_ALL_MEDIA);
        emitChange(event);
    }

    //
    // Action implementations
    //

    private void updateMedia(MediaModel media, boolean emit) {
        OnMediaChanged event = new OnMediaChanged(MediaAction.UPDATE_MEDIA);

        if (media == null) {
            event.error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
        } else if (MediaSqlUtils.insertOrUpdateMedia(media) > 0) {
            event.mediaList.add(media);
        } else {
            event.error = new MediaError(MediaErrorType.DB_QUERY_FAILURE);
        }

        if (emit) {
            emitChange(event);
        }
    }

    private void removeMedia(MediaModel media) {
        OnMediaChanged event = new OnMediaChanged(MediaAction.REMOVE_MEDIA);

        if (media == null) {
            event.error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
        } else if (MediaSqlUtils.deleteMedia(media) > 0) {
            event.mediaList.add(media);
        } else {
            event.error = new MediaError(MediaErrorType.DB_QUERY_FAILURE);
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
        } else if (payload.media.getMediaId() <= 0) {
            // need media ID to push changes
            notifyMediaError(MediaErrorType.MALFORMED_MEDIA_ARG, MediaAction.PUSH_MEDIA, payload.media);
            return;
        }

        if (payload.site.isUsingWpComRestApi()) {
            mMediaRestClient.pushMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.pushMedia(payload.site, payload.media);
        }
    }

    private void notifyMediaUploadError(MediaErrorType errorType, String errorMessage, MediaModel media) {
        OnMediaUploaded onMediaUploaded = new OnMediaUploaded(media, 1, false, false);
        onMediaUploaded.error = new MediaError(errorType, errorMessage);
        emitChange(onMediaUploaded);
    }

    private void performUploadMedia(UploadMediaPayload payload) {
        String errorMessage = MediaUtils.getMediaValidationError(payload.media);
        if (errorMessage != null) {
            AppLog.e(AppLog.T.MEDIA, "Media doesn't have required data: " + errorMessage);
            payload.media.setUploadState(MediaUploadState.FAILED);
            MediaSqlUtils.insertOrUpdateMedia(payload.media);
            notifyMediaUploadError(MediaErrorType.MALFORMED_MEDIA_ARG, errorMessage, payload.media);
            return;
        }

        payload.media.setUploadState(MediaUploadState.UPLOADING);
        MediaSqlUtils.insertOrUpdateMedia(payload.media);

        if (payload.stripLocation) {
            MediaUtils.stripLocation(payload.media.getFilePath());
        }

        if (payload.site.isUsingWpComRestApi()) {
            mMediaRestClient.uploadMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.uploadMedia(payload.site, payload.media);
        }
    }

    private void performFetchMediaList(FetchMediaListPayload payload) {
        int offset = 0;
        if (payload.loadMore) {
            List<String> list = new ArrayList<>();
            list.add(MediaUploadState.UPLOADED.toString());
            if (payload.mimeType != null) {
                offset = MediaSqlUtils.getMediaWithStatesAndMimeType(payload.site, list, payload.mimeType.getValue())
                                      .size();
            } else {
                offset = MediaSqlUtils.getMediaWithStates(payload.site, list).size();
            }
        }
        if (payload.site.isUsingWpComRestApi()) {
            mMediaRestClient.fetchMediaList(payload.site, payload.number, offset, payload.mimeType);
        } else {
            mMediaXmlrpcClient.fetchMediaList(payload.site, payload.number, offset, payload.mimeType);
        }
    }

    private void performFetchMedia(MediaPayload payload) {
        if (payload.site == null || payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.FETCH_MEDIA, payload.media);
            return;
        }

        if (payload.site.isUsingWpComRestApi()) {
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

        if (payload.site.isUsingWpComRestApi()) {
            mMediaRestClient.deleteMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.deleteMedia(payload.site, payload.media);
        }
    }

    private void performCancelUpload(@NonNull CancelMediaPayload payload) {
        if (payload.media == null) {
            return;
        }

        MediaModel media = payload.media;
        if (payload.delete) {
            MediaSqlUtils.deleteMedia(media);
        } else {
            media.setUploadState(MediaUploadState.FAILED);
            MediaSqlUtils.insertOrUpdateMedia(media);
        }

        if (payload.site.isUsingWpComRestApi()) {
            mMediaRestClient.cancelUpload(media);
        } else {
            mMediaXmlrpcClient.cancelUpload(media);
        }
    }

    private void handleMediaPushed(@NonNull MediaPayload payload) {
        OnMediaChanged onMediaChanged = new OnMediaChanged(MediaAction.PUSH_MEDIA, payload.error);
        if (payload.media != null) {
            updateMedia(payload.media, false);
            onMediaChanged.mediaList = new ArrayList<>();
            onMediaChanged.mediaList.add(payload.media);
        }
        emitChange(onMediaChanged);
    }

    private void handleMediaUploaded(@NonNull ProgressPayload payload) {
        if (payload.isError() || payload.canceled || payload.completed) {
            updateMedia(payload.media, false);
        }
        OnMediaUploaded onMediaUploaded =
                new OnMediaUploaded(payload.media, payload.progress, payload.completed, payload.canceled);
        onMediaUploaded.error = payload.error;
        emitChange(onMediaUploaded);
    }

    private void handleMediaCanceled(@NonNull ProgressPayload payload) {
        OnMediaUploaded onMediaUploaded =
                new OnMediaUploaded(payload.media, payload.progress, payload.completed, payload.canceled);
        onMediaUploaded.error = payload.error;

        emitChange(onMediaUploaded);
    }

    private void updateFetchedMediaList(@NonNull FetchMediaListResponsePayload payload) {
        // if we loaded another page, simply add the fetched media and be done
        if (payload.loadedMore) {
            for (MediaModel media : payload.mediaList) {
                updateMedia(media, false);
            }
            return;
        }

        // build separate lists of existing and new media
        List<MediaModel> existingMediaList = new ArrayList<>();
        List<MediaModel> newMediaList = new ArrayList<>();
        for (MediaModel fetchedMedia : payload.mediaList) {
            MediaModel media = getSiteMediaWithId(payload.site, fetchedMedia.getMediaId());
            if (media != null) {
                // retain the local ID, then update this media item
                fetchedMedia.setId(media.getId());
                existingMediaList.add(fetchedMedia);
                updateMedia(fetchedMedia, false);
            } else {
                newMediaList.add(fetchedMedia);
            }
        }

        // remove media that is NOT in the existing list
        String mimeTypeValue = "";
        if (payload.mimeType != null) {
            mimeTypeValue = payload.mimeType.getValue();
        }
        MediaSqlUtils.deleteUploadedSiteMediaNotInList(
                payload.site, existingMediaList, mimeTypeValue);

        // add new media
        for (MediaModel media : newMediaList) {
            updateMedia(media, false);
        }
    }

    private void handleMediaListFetched(@NonNull FetchMediaListResponsePayload payload) {
        OnMediaListFetched onMediaListFetched;

        if (payload.isError()) {
            onMediaListFetched = new OnMediaListFetched(payload.site, payload.error, payload.mimeType);
        } else {
            updateFetchedMediaList(payload);
            onMediaListFetched = new OnMediaListFetched(payload.site, payload.canLoadMore, payload.mimeType);
        }

        emitChange(onMediaListFetched);
    }

    private void handleMediaFetched(@NonNull MediaPayload payload) {
        OnMediaChanged onMediaChanged = new OnMediaChanged(MediaAction.FETCH_MEDIA, payload.error);
        if (payload.media != null) {
            MediaSqlUtils.insertOrUpdateMedia(payload.media);
            onMediaChanged.mediaList = new ArrayList<>();
            onMediaChanged.mediaList.add(payload.media);
        }
        emitChange(onMediaChanged);
    }

    private void handleMediaDeleted(@NonNull MediaPayload payload) {
        OnMediaChanged onMediaChanged = new OnMediaChanged(MediaAction.DELETE_MEDIA, payload.error);
        if (payload.media != null) {
            MediaSqlUtils.deleteMedia(payload.media);
            onMediaChanged.mediaList = new ArrayList<>();
            onMediaChanged.mediaList.add(payload.media);
        }
        emitChange(onMediaChanged);
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

    private void performUploadStockMedia(UploadStockMediaPayload payload) {
        mMediaRestClient.uploadStockMedia(payload.site, payload.stockMediaList);
    }

    private void handleStockMediaUploaded(UploadedStockMediaPayload payload) {
        OnStockMediaUploaded onStockMediaUploaded;

        if (payload.isError()) {
            onStockMediaUploaded = new OnStockMediaUploaded(payload.site, payload.error);
        } else {
            // add uploaded media to the store
            for (MediaModel media : payload.mediaList) {
                updateMedia(media, false);
            }
            onStockMediaUploaded = new OnStockMediaUploaded(payload.site, payload.mediaList);
        }

        emitChange(onStockMediaUploaded);
    }
}
