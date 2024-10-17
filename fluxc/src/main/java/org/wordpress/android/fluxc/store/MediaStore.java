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
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration;
import org.wordpress.android.fluxc.network.rest.wpapi.media.ApplicationPasswordsMediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2.WPComV2MediaRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type;
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

    public static final List<String> NOT_DELETED_STATES = new ArrayList<>();

    static {
        NOT_DELETED_STATES.add(MediaUploadState.DELETING.toString());
        NOT_DELETED_STATES.add(MediaUploadState.FAILED.toString());
        NOT_DELETED_STATES.add(MediaUploadState.QUEUED.toString());
        NOT_DELETED_STATES.add(MediaUploadState.UPLOADED.toString());
        NOT_DELETED_STATES.add(MediaUploadState.UPLOADING.toString());
    }

    //
    // Payloads
    //

    /**
     * Actions: FETCH(ED)_MEDIA, PUSH(ED)_MEDIA, UPLOADED_MEDIA, DELETE(D)_MEDIA, UPDATE_MEDIA, and REMOVE_MEDIA
     */
    public static class MediaPayload extends Payload<MediaError> {
        @NonNull public SiteModel site;
        @Nullable public MediaModel media;

        public MediaPayload(@NonNull SiteModel site, @NonNull MediaModel media) {
            this(site, media, null);
        }

        public MediaPayload(@NonNull SiteModel site, @Nullable MediaModel media, @Nullable MediaError error) {
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

        public UploadMediaPayload(
                @NonNull SiteModel site,
                @Nullable MediaModel media,
                boolean stripLocation) {
            super(site, media, null);
            this.stripLocation = stripLocation;
        }

        public UploadMediaPayload(
                @NonNull SiteModel site,
                @Nullable MediaModel media,
                @Nullable MediaError error,
                boolean stripLocation) {
            super(site, media, error);
            this.stripLocation = stripLocation;
        }
    }

    /**
     * Actions: FETCH_MEDIA_LIST
     */
    public static class FetchMediaListPayload extends Payload<BaseNetworkError> {
        @NonNull public SiteModel site;
        public boolean loadMore;
        @Nullable public MimeType.Type mimeType;
        public int number = DEFAULT_NUM_MEDIA_PER_FETCH;

        @SuppressWarnings("unused")
        public FetchMediaListPayload(@NonNull SiteModel site) {
            this.site = site;
        }

        public FetchMediaListPayload(
                @NonNull SiteModel site,
                int number,
                boolean loadMore) {
            this.site = site;
            this.loadMore = loadMore;
            this.number = number;
        }

        public FetchMediaListPayload(
                @NonNull SiteModel site,
                int number,
                boolean loadMore,
                @NonNull MimeType.Type mimeType) {
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
        @NonNull public SiteModel site;
        @NonNull public List<MediaModel> mediaList;
        public boolean loadedMore;
        public boolean canLoadMore;
        @Nullable public MimeType.Type mimeType;

        public FetchMediaListResponsePayload(
                @NonNull SiteModel site,
                @NonNull List<MediaModel> mediaList,
                boolean loadedMore,
                boolean canLoadMore,
                @Nullable MimeType.Type mimeType) {
            this.site = site;
            this.mediaList = mediaList;
            this.loadedMore = loadedMore;
            this.canLoadMore = canLoadMore;
            this.mimeType = mimeType;
        }

        public FetchMediaListResponsePayload(
                @NonNull SiteModel site,
                @NonNull MediaError error,
                @Nullable MimeType.Type mimeType) {
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
        @Nullable public MediaModel media;
        public float progress;
        public boolean completed;
        public boolean canceled;

        public ProgressPayload(
                @NonNull MediaModel media,
                float progress,
                boolean completed,
                boolean canceled) {
            this(media, progress, completed, null);
            this.canceled = canceled;
        }

        public ProgressPayload(
                @Nullable MediaModel media,
                float progress,
                boolean completed,
                @Nullable MediaError error) {
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
        @NonNull public SiteModel site;
        @NonNull public MediaModel media;
        public boolean delete;

        public CancelMediaPayload(@NonNull SiteModel site, @NonNull MediaModel media) {
            this(site, media, true);
        }

        public CancelMediaPayload(@NonNull SiteModel site, @NonNull MediaModel media, boolean delete) {
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
        @NonNull public MediaErrorType type;
        @Nullable public MediaErrorSubType mErrorSubType;
        @Nullable public String message;
        public int statusCode;
        @Nullable public String logMessage;

        public MediaError(@NonNull MediaErrorType type) {
            this.type = type;
        }

        public MediaError(@NonNull MediaErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }

        public MediaError(
                @NonNull MediaErrorType type,
                @Nullable String message,
                @NonNull MediaErrorSubType errorSubType) {
            this.type = type;
            this.message = message;
            this.mErrorSubType = errorSubType;
        }

        @NonNull
        public static MediaError fromIOException(@NonNull IOException e) {
            MediaError mediaError = new MediaError(MediaErrorType.GENERIC_ERROR);
            mediaError.message = e.getLocalizedMessage();
            mediaError.logMessage = e.getMessage();

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

        @NonNull
        public String getApiUserMessageIfAvailable() {
            if (TextUtils.isEmpty(message)) {
                return "";
            }

            if (type == MediaErrorType.BAD_REQUEST) {
                String[] splitMsg = message.split("\\|", 2);

                if (splitMsg.length > 1) {
                    String userMessage = splitMsg[1];

                    if (TextUtils.isEmpty(userMessage)) {
                        return message;
                    }

                    // NOTE: It seems the backend is sending a final " Back" string in the message
                    // Note that the real string depends on current locale; this is not optimal and we thought to
                    // try to filter it out in the client app but at the end it can be not reliable so we are
                    // keeping it. We can try to get it filtered on the backend side.

                    return userMessage;
                } else {
                    return message;
                }
            } else {
                return message;
            }
        }
    }

    public static class UploadStockMediaError implements OnChangedError {
        @NonNull public UploadStockMediaErrorType type;
        @Nullable public String message;

        public UploadStockMediaError(
                @NonNull UploadStockMediaErrorType type,
                @Nullable String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class OnMediaChanged extends OnChanged<MediaError> {
        @NonNull public MediaAction cause;
        @NonNull public List<MediaModel> mediaList;

        public OnMediaChanged(@NonNull MediaAction cause) {
            this(cause, new ArrayList<>(), null);
        }

        public OnMediaChanged(
                @NonNull MediaAction cause,
                @NonNull List<MediaModel> mediaList) {
            this(cause, mediaList, null);
        }

        public OnMediaChanged(
                @NonNull MediaAction cause,
                @Nullable MediaError error) {
            this(cause, new ArrayList<>(), error);
        }

        public OnMediaChanged(
                @NonNull MediaAction cause,
                @NonNull List<MediaModel> mediaList,
                @Nullable MediaError error) {
            this.cause = cause;
            this.mediaList = mediaList;
            this.error = error;
        }
    }

    public static class OnMediaListFetched extends OnChanged<MediaError> {
        @NonNull public SiteModel site;
        public boolean canLoadMore;
        @Nullable public MimeType.Type mimeType;

        public OnMediaListFetched(
                @NonNull SiteModel site,
                boolean canLoadMore,
                @Nullable MimeType.Type mimeType) {
            this.site = site;
            this.canLoadMore = canLoadMore;
            this.mimeType = mimeType;
        }

        public OnMediaListFetched(
                @NonNull SiteModel site,
                @Nullable MediaError error,
                @Nullable MimeType.Type mimeType) {
            this.site = site;
            this.error = error;
            this.mimeType = mimeType;
        }
    }

    public static class OnMediaUploaded extends OnChanged<MediaError> {
        @Nullable public MediaModel media;
        public float progress;
        public boolean completed;
        public boolean canceled;

        public OnMediaUploaded(
                @Nullable MediaModel media,
                float progress,
                boolean completed,
                boolean canceled) {
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
        BAD_REQUEST,
        XMLRPC_OPERATION_NOT_ALLOWED,
        XMLRPC_UPLOAD_ERROR,

        // logic constraints errors
        INVALID_ID,

        // unknown/unspecified
        GENERIC_ERROR;

        @NonNull
        public static MediaErrorType fromBaseNetworkError(@NonNull BaseNetworkError baseError) {
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
                case NO_CONNECTION:
                case NETWORK_ERROR:
                case CENSORED:
                case INVALID_SSL_CERTIFICATE:
                case HTTP_AUTH_ERROR:
                case INVALID_RESPONSE:
                case UNKNOWN:
                default:
                    return MediaErrorType.GENERIC_ERROR;
            }
        }

        @NonNull
        public static MediaErrorType fromHttpStatusCode(int code) {
            switch (code) {
                case 400:
                    return MediaErrorType.BAD_REQUEST;
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

        @NonNull
        public static MediaErrorType fromString(@Nullable String string) {
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

        @NonNull
        public static UploadStockMediaErrorType fromNetworkError(@NonNull WPComGsonNetworkError wpError) {
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
    private final WPComV2MediaRestClient mWPComV2MediaRestClient;
    private final ApplicationPasswordsMediaRestClient mApplicationPasswordsMediaRestClient;

    private final ApplicationPasswordsConfiguration mApplicationPasswordsConfiguration;

    // Ensures that the UploadStore is initialized whenever the MediaStore is,
    // to ensure actions are shadowed and repeated by the UploadStore
    @SuppressWarnings("unused")
    @Inject UploadStore mUploadStore;

    @Inject public MediaStore(
            Dispatcher dispatcher,
            MediaRestClient restClient,
            MediaXMLRPCClient xmlrpcClient,
            WPComV2MediaRestClient wpv2MediaRestClient,
            ApplicationPasswordsMediaRestClient applicationPasswordsMediaRestClient,
            ApplicationPasswordsConfiguration applicationPasswordsConfiguration) {
        super(dispatcher);
        mMediaRestClient = restClient;
        mMediaXmlrpcClient = xmlrpcClient;
        mWPComV2MediaRestClient = wpv2MediaRestClient;
        mApplicationPasswordsMediaRestClient = applicationPasswordsMediaRestClient;
        mApplicationPasswordsConfiguration = applicationPasswordsConfiguration;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    @SuppressWarnings("rawtypes")
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

    @Nullable
    public MediaModel instantiateMediaModel(@NonNull MediaModel media) {
        MediaModel insertedMedia = MediaSqlUtils.insertMediaForResult(media);

        if (insertedMedia.getId() == -1) {
            return null;
        }

        return insertedMedia;
    }

    @NonNull
    public List<MediaModel> getAllSiteMedia(@NonNull SiteModel siteModel) {
        return MediaSqlUtils.getAllSiteMedia(siteModel);
    }

    public int getSiteMediaCount(@NonNull SiteModel siteModel) {
        return getAllSiteMedia(siteModel).size();
    }

    public boolean hasSiteMediaWithId(@NonNull SiteModel siteModel, long mediaId) {
        return getSiteMediaWithId(siteModel, mediaId) != null;
    }

    @Nullable
    public MediaModel getSiteMediaWithId(@NonNull SiteModel siteModel, long mediaId) {
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(siteModel, mediaId);
        return media.size() > 0 ? media.get(0) : null;
    }

    @Nullable
    public MediaModel getMediaWithLocalId(int localMediaId) {
        return MediaSqlUtils.getMediaWithLocalId(localMediaId);
    }

    @NonNull
    public List<MediaModel> getSiteMediaWithIds(
            @NonNull SiteModel siteModel,
            @NonNull List<Long> mediaIds) {
        return MediaSqlUtils.getSiteMediaWithIds(siteModel, mediaIds);
    }

    @NonNull
    public List<MediaModel> getSiteImages(@NonNull SiteModel siteModel) {
        return MediaSqlUtils.getSiteImages(siteModel);
    }

    @NonNull
    @SuppressWarnings("unused")
    public List<MediaModel> getSiteVideos(@NonNull SiteModel siteModel) {
        return MediaSqlUtils.getSiteVideos(siteModel);
    }

    @NonNull
    @SuppressWarnings("unused")
    public List<MediaModel> getSiteAudio(@NonNull SiteModel siteModel) {
        return MediaSqlUtils.getSiteAudio(siteModel);
    }

    @NonNull
    @SuppressWarnings("unused")
    public List<MediaModel> getSiteDocuments(@NonNull SiteModel siteModel) {
        return MediaSqlUtils.getSiteDocuments(siteModel);
    }

    @NonNull
    public List<MediaModel> getSiteImagesExcludingIds(
            @NonNull SiteModel siteModel,
            @NonNull List<Long> filter) {
        return MediaSqlUtils.getSiteImagesExcluding(siteModel, filter);
    }

    @NonNull
    public List<MediaModel> getUnattachedSiteMedia(@NonNull SiteModel siteModel) {
        return MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.POST_ID, 0);
    }

    @NonNull
    public List<MediaModel> getLocalSiteMedia(@NonNull SiteModel siteModel) {
        MediaUploadState expectedState = MediaUploadState.UPLOADED;
        return MediaSqlUtils.getSiteMediaExcluding(siteModel, MediaModelTable.UPLOAD_STATE, expectedState);
    }

    @NonNull
    public List<MediaModel> getSiteMediaWithState(
            @NonNull SiteModel siteModel,
            @NonNull MediaUploadState expectedState) {
        return MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.UPLOAD_STATE, expectedState);
    }

    @Nullable
    public String getUrlForSiteVideoWithVideoPressGuid(
            @NonNull SiteModel siteModel,
            @NonNull String videoPressGuid) {
        List<MediaModel> media =
                MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.VIDEO_PRESS_GUID, videoPressGuid);
        return media.size() > 0 ? media.get(0).getUrl() : null;
    }

    @Nullable
    public String getThumbnailUrlForSiteMediaWithId(@NonNull SiteModel siteModel, long mediaId) {
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(siteModel, mediaId);
        return media.size() > 0 ? media.get(0).getThumbnailUrl() : null;
    }

    @NonNull
    public List<MediaModel> searchSiteMedia(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return MediaSqlUtils.searchSiteMedia(siteModel, searchTerm);
    }

    @NonNull
    public List<MediaModel> searchSiteImages(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return MediaSqlUtils.searchSiteImages(siteModel, searchTerm);
    }

    @NonNull
    public List<MediaModel> searchSiteVideos(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return MediaSqlUtils.searchSiteVideos(siteModel, searchTerm);
    }

    @NonNull
    public List<MediaModel> searchSiteAudio(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return MediaSqlUtils.searchSiteAudio(siteModel, searchTerm);
    }

    @NonNull
    public List<MediaModel> searchSiteDocuments(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return MediaSqlUtils.searchSiteDocuments(siteModel, searchTerm);
    }

    @Nullable
    public MediaModel getMediaForPostWithPath(
            @NonNull PostImmutableModel postModel,
            @NonNull String filePath) {
        List<MediaModel> media = MediaSqlUtils.matchPostMedia(postModel.getId(), MediaModelTable.FILE_PATH, filePath);
        return media.size() > 0 ? media.get(0) : null;
    }

    @NonNull
    public List<MediaModel> getMediaForPost(@NonNull PostImmutableModel postModel) {
        return MediaSqlUtils.matchPostMedia(postModel.getId());
    }

    @NonNull
    @SuppressWarnings("unused")
    public List<MediaModel> getMediaForPostWithState(
            @NonNull PostImmutableModel postModel,
            @NonNull MediaUploadState expectedState) {
        return MediaSqlUtils.matchPostMedia(postModel.getId(), MediaModelTable.UPLOAD_STATE, expectedState);
    }

    @Nullable
    public MediaModel getNextSiteMediaToDelete(@NonNull SiteModel siteModel) {
        List<MediaModel> media = MediaSqlUtils.matchSiteMedia(siteModel,
                MediaModelTable.UPLOAD_STATE, MediaUploadState.DELETING.toString());
        return media.size() > 0 ? media.get(0) : null;
    }

    public boolean hasSiteMediaToDelete(@NonNull SiteModel siteModel) {
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

    void updateMedia(@Nullable MediaModel media, boolean emit) {
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

    private void removeMedia(@Nullable MediaModel media) {
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

    private void performPushMedia(@NonNull MediaPayload payload) {
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

    @SuppressWarnings("SameParameterValue")
    private void notifyMediaUploadError(
            @NonNull MediaErrorType errorType,
            @Nullable String errorMessage,
            @Nullable MediaModel media,
            @NonNull String logMessage,
            @NonNull MalformedMediaArgSubType argErrorType) {
        OnMediaUploaded onMediaUploaded = new OnMediaUploaded(media, 1, false, false);
        MediaError mediaError = new MediaError(errorType, errorMessage, argErrorType);
        mediaError.logMessage = logMessage;
        onMediaUploaded.error = mediaError;
        emitChange(onMediaUploaded);
    }

    private void performUploadMedia(@NonNull UploadMediaPayload payload) {
        if (payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.UPLOAD_MEDIA, null);
            return;
        }

        MalformedMediaArgSubType argError = MediaUtils.getMediaValidationErrorType(payload.media);

        if (argError.getType() != Type.NO_ERROR) {
            String message = "Media doesn't have required data: " + argError.getType().getErrorLogDescription();
            AppLog.e(AppLog.T.MEDIA, message);
            payload.media.setUploadState(MediaUploadState.FAILED);
            MediaSqlUtils.insertOrUpdateMedia(payload.media);
            notifyMediaUploadError(
                    MediaErrorType.MALFORMED_MEDIA_ARG,
                    argError.getType().getErrorLogDescription(),
                    payload.media,
                    message,
                    argError);
            return;
        }

        payload.media.setUploadState(MediaUploadState.UPLOADING);
        MediaSqlUtils.insertOrUpdateMedia(payload.media);

        if (payload.stripLocation) {
            MediaUtils.stripLocation(payload.media.getFilePath());
        }

        if (payload.site.isUsingWpComRestApi()) {
            mMediaRestClient.uploadMedia(payload.site, payload.media);
        } else if (payload.site.isJetpackCPConnected()) {
            mWPComV2MediaRestClient.uploadMedia(payload.site, payload.media);
        } else if (payload.site.getOrigin() == SiteModel.ORIGIN_WPAPI
                   && mApplicationPasswordsConfiguration.isEnabled()) {
            mApplicationPasswordsMediaRestClient.uploadMedia(payload.site, payload.media);
        } else {
            mMediaXmlrpcClient.uploadMedia(payload.site, payload.media);
        }
    }

    private void performFetchMediaList(@NonNull FetchMediaListPayload payload) {
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
        } else if (payload.site.isJetpackCPConnected()) {
            mWPComV2MediaRestClient.fetchMediaList(payload.site, payload.number, offset, payload.mimeType);
        } else if (payload.site.getOrigin() == SiteModel.ORIGIN_WPAPI
                   && mApplicationPasswordsConfiguration.isEnabled()) {
            mApplicationPasswordsMediaRestClient.fetchMediaList(payload.site, payload.number, offset, payload.mimeType);
        } else {
            mMediaXmlrpcClient.fetchMediaList(payload.site, payload.number, offset, payload.mimeType);
        }
    }

    private void performFetchMedia(@NonNull MediaPayload payload) {
        if (payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.FETCH_MEDIA, null);
            return;
        }

        if (payload.site.isUsingWpComRestApi()) {
            mMediaRestClient.fetchMedia(payload.site, payload.media);
        } else if (payload.site.isJetpackCPConnected()) {
            mWPComV2MediaRestClient.fetchMedia(payload.site, payload.media);
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
        MediaModel media = payload.media;
        if (payload.delete) {
            MediaSqlUtils.deleteMedia(media);
        } else {
            media.setUploadState(MediaUploadState.FAILED);
            MediaSqlUtils.insertOrUpdateMedia(media);
        }

        if (payload.site.isUsingWpComRestApi()) {
            mMediaRestClient.cancelUpload(media);
        } else if (payload.site.isJetpackCPConnected()) {
            mWPComV2MediaRestClient.cancelUpload(media);
        } else if (payload.site.getOrigin() == SiteModel.ORIGIN_WPAPI
                   && mApplicationPasswordsConfiguration.isEnabled()) {
            mApplicationPasswordsMediaRestClient.cancelUpload(media);
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
        OnMediaUploaded onMediaUploaded = new OnMediaUploaded(
                payload.media,
                payload.progress,
                payload.completed,
                payload.canceled
        );
        onMediaUploaded.error = payload.error;
        emitChange(onMediaUploaded);
    }

    private void handleMediaCanceled(@NonNull ProgressPayload payload) {
        OnMediaUploaded onMediaUploaded = new OnMediaUploaded(
                payload.media,
                payload.progress,
                payload.completed,
                payload.canceled
        );
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
        MediaSqlUtils.deleteUploadedSiteMediaNotInList(payload.site, existingMediaList, mimeTypeValue);

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

    private void notifyMediaError(
            @NonNull MediaErrorType errorType,
            @NonNull MediaAction cause,
            @Nullable MediaModel media) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        OnMediaChanged mediaChange = new OnMediaChanged(cause, mediaList);
        mediaChange.error = new MediaError(errorType, null);
        emitChange(mediaChange);
    }

    private void performUploadStockMedia(@NonNull UploadStockMediaPayload payload) {
        mMediaRestClient.uploadStockMedia(payload.site, payload.stockMediaList);
    }

    private void handleStockMediaUploaded(@NonNull UploadedStockMediaPayload payload) {
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
