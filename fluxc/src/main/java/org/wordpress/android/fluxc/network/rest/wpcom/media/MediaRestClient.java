package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.google.gson.Gson;

import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaFilter;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;

/**
 * MediaRestClient provides an interface for manipulating a WP.com site's media. It provides
 * methods to:
 *
 * <ul>
 *     <li>Fetch existing media from a WP.com site
 *     (via {@link #fetchAllMedia(SiteModel, MediaFilter)} and {@link #fetchMedia(SiteModel, List)}</li>
 *     <li>Push new media to a WP.com site
 *     (via {@link #uploadMedia(SiteModel, MediaModel)})</li>
 *     <li>Push updates to existing media to a WP.com site
 *     (via {@link #pushMedia(SiteModel, List)})</li>
 *     <li>Delete existing media from a WP.com site
 *     (via {@link #deleteMedia(SiteModel, List)})</li>
 * </ul>
 */
public class MediaRestClient extends BaseWPComRestClient implements ProgressListener {
    private OkHttpClient mOkHttpClient;
    private Call mCurrentUploadCall;

    public MediaRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                           OkHttpClient okClient, AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okClient;
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        notifyMediaProgress(media, Math.min(progress, 0.99f), null);
    }

    /**
     * Pushes updates to existing media items on a WP.com site, creating (and uploading) new
     * media files as necessary.
     */
    public void pushMedia(final SiteModel site, final List<MediaModel> mediaToPush) {
        for (final MediaModel media : mediaToPush) {
            String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).getUrlV1_1();
            add(WPComGsonRequest.buildPostRequest(url, getEditRequestParams(media),
                    MediaWPComRestResponse.class, new Listener<MediaWPComRestResponse>() {
                @Override
                public void onResponse(MediaWPComRestResponse response) {
                    MediaModel responseMedia = getMediaFromRestResponse(response, site.getSiteId());
                    if (responseMedia != null) {
                        responseMedia.setSiteId(site.getSiteId());
                        AppLog.v(T.MEDIA, "media pushed to site: " + responseMedia);
                        notifyMediaPushed(MediaAction.PUSH_MEDIA, site, responseMedia, null);
                    } else {
                        AppLog.w(T.MEDIA, "could not parse push media response, ID: " + media.getMediaId());
                        MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                        notifyMediaPushed(MediaAction.PUSH_MEDIA, site, media, error);
                    }
                }
            }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    AppLog.e(T.MEDIA, "error editing remote media: " + error);
                    MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                    notifyMediaPushed(MediaAction.PUSH_MEDIA, site, media, mediaError);
                    // TODO: should we upload it for them here if the error is NOT_FOUND?
                }
            }));
        }
    }

    /**
     * Uploads a single media item to a WP.com site.
     */
    public void uploadMedia(final SiteModel site, final MediaModel mediaToUpload) {
        performUpload(mediaToUpload, site.getSiteId());
    }

    /**
     * Gets a list of all media items on a WP.com site.
     *
     * NOTE: Only media item data is gathered, the actual media file can be downloaded from the URL
     * provided in the response {@link MediaModel}'s (via {@link MediaModel#getUrl()}).
     */
    public void fetchAllMedia(final SiteModel site, final MediaFilter filter) {
        String url = WPCOMREST.sites.site(site.getSiteId()).media.getUrlV1_1();
        Map<String, String> params = getQueryParams(filter);
        add(WPComGsonRequest.buildGetRequest(url, params, MultipleMediaResponse.class,
                new Listener<MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MultipleMediaResponse response) {
                        List<MediaModel> media = getMediaListFromRestResponse(response, site.getSiteId());
                        if (media != null) {
                            AppLog.v(T.MEDIA, "Fetched all media for site");
                            notifyMediaFetched(MediaAction.FETCH_ALL_MEDIA, site, media, null);
                        } else {
                            AppLog.w(T.MEDIA, "could not parse Fetch all media response: " + response);
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            notifyMediaFetched(MediaAction.FETCH_ALL_MEDIA, site, (MediaModel) null, error);
                        }
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.v(T.MEDIA, "VolleyError Fetching media: " + error);
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        notifyMediaFetched(MediaAction.FETCH_ALL_MEDIA, site, (MediaModel) null, mediaError);
                    }
        }));
    }

    /**
     * Gets a list of media items whose media IDs match the provided list.
     */
    public void fetchMedia(final SiteModel site, final List<MediaModel> mediaToFetch) {
        if (mediaToFetch == null || mediaToFetch.isEmpty()) return;

        for (final MediaModel media: mediaToFetch) {
            String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).getUrlV1_1();
            add(WPComGsonRequest.buildGetRequest(url, null, MediaWPComRestResponse.class,
                    new Listener<MediaWPComRestResponse>() {
                        @Override
                        public void onResponse(MediaWPComRestResponse response) {
                            MediaModel responseMedia = getMediaFromRestResponse(response, site.getSiteId());
                            if (responseMedia != null) {
                                AppLog.v(T.MEDIA, "Fetched media with ID: " + media.getMediaId());
                                notifyMediaFetched(MediaAction.FETCH_MEDIA, site, responseMedia, null);
                            } else {
                                AppLog.w(T.MEDIA, "could not parse Fetch media response, ID: " + media.getMediaId());
                                MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                                notifyMediaFetched(MediaAction.FETCH_MEDIA, site, media, error);
                            }
                        }
                    }, new BaseRequest.BaseErrorListener() {
                        @Override
                        public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                            AppLog.v(T.MEDIA, "VolleyError Fetching media: " + error);
                            MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                            notifyMediaFetched(MediaAction.FETCH_MEDIA, site, media, mediaError);
                        }
            }));
        }
    }

    /**
     * Deletes media from a WP.com site whose media ID is in the provided list.
     */
    public void deleteMedia(final SiteModel site, final List<MediaModel> mediaToDelete) {
        if (mediaToDelete == null || mediaToDelete.isEmpty()) return;

        for (final MediaModel media : mediaToDelete) {
            String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).delete.getUrlV1_1();
            add(WPComGsonRequest.buildPostRequest(url, null, MediaWPComRestResponse.class,
                    new Listener<MediaWPComRestResponse>() {
                        @Override
                        public void onResponse(MediaWPComRestResponse response) {
                            MediaModel deletedMedia = getMediaFromRestResponse(response, site.getSiteId());
                            if (deletedMedia != null) {
                                AppLog.v(T.MEDIA, "deleted media with ID: " + media.getMediaId());
                                notifyMediaDeleted(MediaAction.DELETE_MEDIA, site, deletedMedia, null);
                            } else {
                                AppLog.w(T.MEDIA, "could not parse delete media response, ID: " + media.getMediaId());
                                MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                                notifyMediaDeleted(MediaAction.FETCH_ALL_MEDIA, site, media, error);
                            }
                        }
                    }, new BaseRequest.BaseErrorListener() {
                        @Override
                        public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                            AppLog.v(T.MEDIA, "VolleyError deleting media (ID=" + media.getMediaId() + "): " + error);
                            MediaErrorType mediaError = MediaErrorType.fromBaseNetworkError(error);
                            if (mediaError == MediaErrorType.MEDIA_NOT_FOUND) {
                                AppLog.i(T.MEDIA, "Attempted to delete media that does not exist remotely.");
                                notifyMediaDeleted(MediaAction.DELETE_MEDIA, site, media, null);
                            } else {
                                notifyMediaDeleted(MediaAction.FETCH_MEDIA, site, media, new MediaError(mediaError));
                            }
                        }
            }));
        }
    }

    public void cancelUpload(final MediaModel media) {
        if (media == null) {
            MediaStore.MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyMediaUploaded(null, error);
            return;
        }

        // cancel in-progress upload if necessary
        if (mCurrentUploadCall != null && mCurrentUploadCall.isExecuted() && !mCurrentUploadCall.isCanceled()) {
            AppLog.d(T.MEDIA, "Canceled in-progress upload: " + media.getFileName());
            mCurrentUploadCall.cancel();
            mCurrentUploadCall = null;
        }
        // always report without error
        notifyMediaUploadCanceled(media);
    }

    private void performUpload(final MediaModel media, final long siteId) {
        if (!MediaUtils.canReadFile(media.getFilePath())) {
            MediaStore.MediaError error = new MediaError(MediaErrorType.FS_READ_PERMISSION_DENIED);
            notifyMediaUploaded(media, error);
            return;
        }

        String url = WPCOMREST.sites.site(siteId).media.new_.getUrlV1_1();
        RestUploadRequestBody body = new RestUploadRequestBody(media, getEditRequestParams(media), this);
        String authHeader = String.format(WPComGsonRequest.REST_AUTHORIZATION_FORMAT, getAccessToken().get());

        okhttp3.Request request = new okhttp3.Request.Builder()
                .addHeader(WPComGsonRequest.REST_AUTHORIZATION_HEADER, authHeader)
                .addHeader("User-Agent", mUserAgent.toString())
                .url(url)
                .post(body)
                .build();

        mCurrentUploadCall = mOkHttpClient.newCall(request);
        mCurrentUploadCall.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    AppLog.d(T.MEDIA, "media upload successful: " + response);
                    String jsonBody = response.body().string();
                    MultipleMediaResponse mediaResponse =
                            new Gson().fromJson(jsonBody, MultipleMediaResponse.class);
                    List<MediaModel> responseMedia = getMediaListFromRestResponse(mediaResponse, siteId);
                    if (responseMedia != null && !responseMedia.isEmpty()) {
                        notifyMediaUploaded(responseMedia.get(0), null);
                    }
                } else {
                    AppLog.w(T.MEDIA, "error uploading media: " + response);
                    MediaStore.MediaError error = new MediaError(MediaErrorType.fromHttpStatusCode(response.code()));
                    notifyMediaUploaded(media, error);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                AppLog.w(T.MEDIA, "media upload failed: " + e);
                MediaStore.MediaError error = new MediaError(MediaErrorType.GENERIC_ERROR);
                notifyMediaUploaded(media, error);
            }
        });
    }

    //
    // Helper methods to dispatch media actions
    //

    private void notifyMediaFetched(MediaAction cause, SiteModel site, MediaModel media, MediaError error) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(cause, site, mediaList);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaAction(payload));
    }

    private void notifyMediaFetched(MediaAction cause, SiteModel site, List<MediaModel> mediaList, MediaError error) {
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(cause, site, mediaList);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaAction(payload));
    }

    private void notifyMediaPushed(MediaAction cause, SiteModel site, MediaModel media, MediaError error) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(cause, site, mediaList);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newPushedMediaAction(payload));
    }

    private void notifyMediaDeleted(MediaAction cause, SiteModel site, MediaModel media, MediaError error) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(cause, site, mediaList);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newDeletedMediaAction(payload));
    }

    private void notifyMediaProgress(MediaModel media, float progress, MediaError error) {
        AppLog.v(AppLog.T.MEDIA, "Progress update on upload of " + media.getFilePath() + ": " + progress);
        MediaStore.ProgressPayload payload = new MediaStore.ProgressPayload(media, progress, false);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaUploaded(MediaModel media, MediaError error) {
        if (media != null) {
            AppLog.v(AppLog.T.MEDIA, "Notify media uploaded: " + media.getFilePath());
        }
        MediaStore.ProgressPayload payload = new MediaStore.ProgressPayload(media, 1.f, error == null);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaUploadCanceled(MediaModel media) {
        MediaStore.ProgressPayload payload = new MediaStore.ProgressPayload(media, -1.f, false);
        mDispatcher.dispatch(MediaActionBuilder.newCanceledMediaUploadAction(payload));
    }

    //
    // Utility methods
    //

    // parameters for WP.com REST queries
    private static final String FIELDS_FILTER_KEY    = "fields";
    private static final String NUMBER_FILTER_KEY    = "number";
    private static final String OFFSET_FILTER_KEY    = "offset";
    private static final String PAGE_FILTER_KEY      = "page";
    private static final String ORDER_FILTER_KEY     = "order";
    private static final String ORDER_BY_FILTER_KEY  = "order_by";
    private static final String SEARCH_FILTER_KEY    = "search";
    private static final String PARENT_FILTER_KEY    = "post_ID";
    private static final String MIME_TYPE_FILTER_KEY = "mime_type";
    private static final String AFTER_FILTER_KEY     = "after";
    private static final String BEFORE_FILTER_KEY    = "before";

    // parameters for REST request to edit media
    private static final String PARENT_EDIT_KEY      = "parent_id";
    private static final String TITLE_EDIT_KEY       = "title";
    private static final String CAPTION_EDIT_KEY     = "caption";
    private static final String DESCRIPTION_EDIT_KEY = "description";
    private static final String ALT_EDIT_KEY         = "alt";

    // values for sort order parameter
    private static final String DESCENDING_SORT = "DESC";
    private static final String ASCENDING_SORT  = "ASC";

    // values for sort field parameter
    private static final String ORDER_BY_DATE  = "date";
    private static final String ORDER_BY_TITLE = "title";
    private static final String ORDER_BY_ID    = "ID";

    /**
     * Creates a {@link MediaModel} list from a WP.com REST response to a request for all media.
     */
    private List<MediaModel> getMediaListFromRestResponse(final MultipleMediaResponse from, long siteId) {
        if (from == null || from.media == null) return null;

        final List<MediaModel> media = new ArrayList<>();
        for (int i = 0; i < from.media.size(); ++i) {
            media.add(i, getMediaFromRestResponse(from.media.get(i), siteId));
        }
        return media;
    }

    /**
     * Creates a {@link MediaModel} from a WP.com REST response to a fetch request.
     */
    private MediaModel getMediaFromRestResponse(final MediaWPComRestResponse from, long siteId) {
        if (from == null) return null;

        final MediaModel media = new MediaModel();
        media.setSiteId(siteId);
        media.setMediaId(from.ID);
        media.setUploadDate(from.date);
        media.setPostId(from.post_ID);
        media.setAuthorId(from.author_ID);
        media.setUrl(from.URL);
        media.setGuid(from.guid);
        media.setFileName(from.file);
        media.setFileExtension(from.extension);
        media.setMimeType(from.mime_type);
        media.setTitle(from.title);
        media.setCaption(from.caption);
        media.setDescription(from.description);
        media.setAlt(from.alt);
        media.setThumbnailUrl(from.thumbnails.thumbnail);
        media.setHeight(from.height);
        media.setWidth(from.width);
        media.setLength(from.length);
        media.setVideoPressGuid(from.videopress_guid);
        media.setVideoPressProcessingDone(from.videopress_processing_done);
        media.setDeleted(MediaWPComRestResponse.DELETED_STATUS.equals(from.status));
        return media;
    }

    /**
     * The current REST API call (v1.1) accepts 'title', 'description', 'caption', 'alt',
     * and 'parent_id' for all media. Audio media also accepts 'artist' and 'album' attributes.
     *
     * ref https://developer.wordpress.com/docs/api/1.1/post/sites/%24site/media/new/
     */
    private Map<String, Object> getEditRequestParams(final MediaModel media) {
        if (media == null) return null;

        final Map<String, Object> params = new HashMap<>();
        if (!TextUtils.isEmpty(media.getTitle())) {
            params.put(TITLE_EDIT_KEY, media.getTitle());
        }
        if (!TextUtils.isEmpty(media.getDescription())) {
            params.put(DESCRIPTION_EDIT_KEY, media.getDescription());
        }
        if (!TextUtils.isEmpty(media.getCaption())) {
            params.put(CAPTION_EDIT_KEY, media.getCaption());
        }
        if (!TextUtils.isEmpty(media.getAlt())) {
            params.put(ALT_EDIT_KEY, media.getAlt());
        }
        if (media.getPostId() > 0) {
            params.put(PARENT_EDIT_KEY, String.valueOf(media.getPostId()));
        }
        return params;
    }

    /**
     * Query parameters are used by WP.com REST API endpoints to filter media.
     */
    private Map<String, String> getQueryParams(final MediaFilter filter) {
        if (filter == null) return null;

        final Map<String, String> params = new HashMap<>();
        if (filter.fields != null && !filter.fields.isEmpty()) {
            params.put(FIELDS_FILTER_KEY, TextUtils.join(",", filter.fields));
        }
        if (filter.number > 0) {
            params.put(NUMBER_FILTER_KEY, String.valueOf(filter.number));
        }
        if (filter.offset > 0) {
            params.put(OFFSET_FILTER_KEY, String.valueOf(filter.offset));
        }
        if (filter.page > 0) {
            params.put(PAGE_FILTER_KEY, String.valueOf(filter.page));
        }
        if (!TextUtils.isEmpty(filter.searchQuery)) {
            params.put(SEARCH_FILTER_KEY, filter.searchQuery);
        }
        if (filter.postId > 0) {
            params.put(PARENT_FILTER_KEY, String.valueOf(filter.postId));
        }
        if (!TextUtils.isEmpty(filter.mimeType)) {
            params.put(MIME_TYPE_FILTER_KEY, filter.mimeType);
        }
        if (!TextUtils.isEmpty(filter.after)) {
            params.put(AFTER_FILTER_KEY, filter.after);
        }
        if (!TextUtils.isEmpty(filter.before)) {
            params.put(BEFORE_FILTER_KEY, filter.before);
        }
        if (filter.sortOrder != null) {
            switch (filter.sortOrder) {
                case ASCENDING:
                    params.put(ORDER_FILTER_KEY, ASCENDING_SORT);
                    break;
                case DESCENDING:
                default:
                    params.put(ORDER_FILTER_KEY, DESCENDING_SORT);
                    break;
            }
        }
        if (filter.sortField != null) {
            switch (filter.sortField) {
                case TITLE:
                    params.put(ORDER_BY_FILTER_KEY, ORDER_BY_TITLE);
                    break;
                case ID:
                    params.put(ORDER_BY_FILTER_KEY, ORDER_BY_ID);
                    break;
                case DATE:
                default:
                    params.put(ORDER_BY_FILTER_KEY, ORDER_BY_DATE);
                    break;
            }
        }
        return params;
    }
}
