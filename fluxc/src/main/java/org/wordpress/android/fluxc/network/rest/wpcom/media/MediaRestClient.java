package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;

/**
 * MediaRestClient provides an interface for manipulating a WP.com site's media. It provides
 * methods to:
 *
 * <ul>
 *     <li>Fetch existing media from a WP.com site
 *     (via {@link #fetchMediaList(SiteModel, int)} and {@link #fetchMedia(SiteModel, MediaModel)}</li>
 *     <li>Push new media to a WP.com site
 *     (via {@link #uploadMedia(SiteModel, MediaModel)})</li>
 *     <li>Push updates to existing media to a WP.com site
 *     (via {@link #pushMedia(SiteModel, MediaModel)})</li>
 *     <li>Delete existing media from a WP.com site
 *     (via {@link #deleteMedia(SiteModel, MediaModel)})</li>
 * </ul>
 */
public class MediaRestClient extends BaseWPComRestClient implements ProgressListener {
    private OkHttpClient mOkHttpClient;
    private Call mCurrentUploadCall;

    public MediaRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                           OkHttpClient.Builder okClientBuilder, AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okClientBuilder
                .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        notifyMediaProgress(media, Math.min(progress, 0.99f), null);
    }

    /**
     */
    public void pushMedia(final SiteModel site, final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyMediaPushed(site, media, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).getUrlV1_1();

        add(WPComGsonRequest.buildPostRequest(url, getEditRequestParams(media),
                MediaWPComRestResponse.class, new Listener<MediaWPComRestResponse>() {
            @Override
            public void onResponse(MediaWPComRestResponse response) {
                MediaModel responseMedia = getMediaFromRestResponse(response);
                if (responseMedia != null) {
                    AppLog.v(T.MEDIA, "media changes pushed for " + responseMedia.getTitle());
                    responseMedia.setLocalSiteId(site.getId());
                    notifyMediaPushed(site, responseMedia, null);
                } else {
                    MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                    notifyMediaPushed(site, media, error);
                }
            }
        }, new BaseRequest.BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                AppLog.w(T.MEDIA, "error editing remote media: " + error);
                MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                notifyMediaPushed(site, media, mediaError);
            }
        }));
    }

    /**
     * Uploads a single media item to a WP.com site.
     */
    public void uploadMedia(final SiteModel site, final MediaModel mediaToUpload) {
        performUpload(mediaToUpload, site);
    }

    /**
     * Gets a list of media items given the offset on a WP.com site.
     *
     * NOTE: Only media item data is gathered, the actual media file can be downloaded from the URL
     * provided in the response {@link MediaModel}'s (via {@link MediaModel#getUrl()}).
     */
    public void fetchMediaList(final SiteModel site, final int offset) {
        final Map<String, String> params = new HashMap<>();
        params.put("number", String.valueOf(MediaStore.NUM_MEDIA_PER_FETCH));
        if (offset > 0) {
            params.put("offset", String.valueOf(offset));
        }
        String url = WPCOMREST.sites.site(site.getSiteId()).media.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, params, MultipleMediaResponse.class,
                new Listener<MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MultipleMediaResponse response) {
                        List<MediaModel> mediaList = getMediaListFromRestResponse(response, site.getId());
                        if (mediaList != null) {
                            AppLog.v(T.MEDIA, "Fetched media list for site with size: " + mediaList.size());
                            boolean canLoadMore = mediaList.size() == MediaStore.NUM_MEDIA_PER_FETCH;
                            notifyMediaListFetched(site, mediaList, offset > 0, canLoadMore);
                        } else {
                            AppLog.w(T.MEDIA, "could not parse Fetch all media response: " + response);
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            notifyMediaListFetched(site, error);
                        }
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.v(T.MEDIA, "VolleyError Fetching media: " + error);
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        notifyMediaListFetched(site, mediaError);
                    }
        }));
    }

    /**
     * Gets a list of media items whose media IDs match the provided list.
     */
    public void fetchMedia(final SiteModel site, final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyMediaFetched(site, null, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, MediaWPComRestResponse.class,
                new Listener<MediaWPComRestResponse>() {
                    @Override
                    public void onResponse(MediaWPComRestResponse response) {
                        MediaModel responseMedia = getMediaFromRestResponse(response);
                        if (responseMedia != null) {
                            responseMedia.setLocalSiteId(site.getId());
                            AppLog.v(T.MEDIA, "Fetched media with ID: " + media.getMediaId());
                            notifyMediaFetched(site, responseMedia, null);
                        } else {
                            AppLog.w(T.MEDIA, "could not parse Fetch media response, ID: " + media.getMediaId());
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            notifyMediaFetched(site, media, error);
                        }
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.v(T.MEDIA, "VolleyError Fetching media: " + error);
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        notifyMediaFetched(site, media, mediaError);
                    }
        }));
    }

    /**
     * Deletes media from a WP.com site whose media ID is in the provided list.
     */
    public void deleteMedia(final SiteModel site, final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyMediaDeleted(site, media, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).delete.getUrlV1_1();
        add(WPComGsonRequest.buildPostRequest(url, null, MediaWPComRestResponse.class,
                new Listener<MediaWPComRestResponse>() {
                    @Override
                    public void onResponse(MediaWPComRestResponse response) {
                        MediaModel deletedMedia = getMediaFromRestResponse(response);
                        if (deletedMedia != null) {
                            AppLog.v(T.MEDIA, "deleted media: " + media.getTitle());
                            notifyMediaDeleted(site, media, null);
                        } else {
                            AppLog.w(T.MEDIA, "could not parse delete media response, ID: " + media.getMediaId());
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            notifyMediaDeleted(site, media, error);
                        }
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.v(T.MEDIA, "VolleyError deleting media (ID=" + media.getMediaId() + "): " + error);
                        MediaErrorType mediaError = MediaErrorType.fromBaseNetworkError(error);
                        if (mediaError == MediaErrorType.NOT_FOUND) {
                            AppLog.i(T.MEDIA, "Attempted to delete media that does not exist remotely.");
                        }
                        notifyMediaDeleted(site, media, new MediaError(mediaError));
                    }
        }));
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
            notifyMediaUploadCanceled(media);
        }
    }

    private void performUpload(final MediaModel media, final SiteModel siteModel) {
        if (!MediaUtils.canReadFile(media.getFilePath())) {
            MediaStore.MediaError error = new MediaError(MediaErrorType.FS_READ_PERMISSION_DENIED);
            notifyMediaUploaded(media, error);
            return;
        }

        String url = WPCOMREST.sites.site(siteModel.getSiteId()).media.new_.getUrlV1_1();
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

                    Gson gson = new Gson();
                    JsonReader reader = new JsonReader(new StringReader(jsonBody));
                    reader.setLenient(true);
                    MultipleMediaResponse mediaResponse = gson.fromJson(reader, MultipleMediaResponse.class);

                    List<MediaModel> responseMedia = getMediaListFromRestResponse(mediaResponse, siteModel.getId());
                    if (responseMedia != null && !responseMedia.isEmpty()) {
                        MediaModel uploadedMedia = responseMedia.get(0);
                        uploadedMedia.setId(media.getId());
                        notifyMediaUploaded(uploadedMedia, null);
                    } else {
                        MediaStore.MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                        notifyMediaUploaded(media, error);
                    }
                } else {
                    AppLog.w(T.MEDIA, "error uploading media: " + response);
                    notifyMediaUploaded(media, parseUploadError(response));
                }
                mCurrentUploadCall = null;
            }

            @Override
            public void onFailure(Call call, IOException e) {
                if (mCurrentUploadCall != null && mCurrentUploadCall.isCanceled()) {
                    // Do not report errors since the upload was canceled and notified separately
                    mCurrentUploadCall = null;
                    return;
                }
                AppLog.w(T.MEDIA, "media upload failed: " + e);
                MediaStore.MediaError error = new MediaError(MediaErrorType.GENERIC_ERROR);
                notifyMediaUploaded(media, error);
                mCurrentUploadCall = null;
            }
        });
    }

    //
    // Helper methods to dispatch media actions
    //

    private MediaError parseUploadError(okhttp3.Response response) {
        MediaError mediaError = new MediaError(MediaErrorType.GENERIC_ERROR);
        if (response.code() == 403) {
            mediaError.type = MediaErrorType.AUTHORIZATION_REQUIRED;
        } else if (response.code() == 413) {
            mediaError.type = MediaErrorType.REQUEST_TOO_LARGE;
            mediaError.message = response.message();
            return mediaError;
        }
        try {
            JSONObject body = new JSONObject(response.body().string());
            // Can be an array or errors
            if (body.has("errors")) {
                JSONArray errors = body.getJSONArray("errors");
                if (errors.length() == 1) {
                    JSONObject error = errors.getJSONObject(0);
                    // error.getString("error")) is always "upload_error"
                    if (error.has("message")) {
                        mediaError.message = error.getString("message");
                    }
                }
            }
            // Or an object
            if (body.has("message")) {
                mediaError.message = body.getString("message");
            }
        } catch (JSONException | IOException e) {
            // no op
        }
        return mediaError;
    }

    private void notifyMediaPushed(SiteModel site, MediaModel media, MediaError error) {
        MediaPayload payload = new MediaPayload(site, media, error);
        mDispatcher.dispatch(MediaActionBuilder.newPushedMediaAction(payload));
    }

    private void notifyMediaProgress(MediaModel media, float progress, MediaError error) {
        ProgressPayload payload = new ProgressPayload(media, progress, false, error);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaUploaded(MediaModel media, MediaError error) {
        ProgressPayload payload = new ProgressPayload(media, 1.f, error == null, error);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaListFetched(SiteModel site, @NonNull List<MediaModel> media,
                                        boolean loadedMore, boolean canLoadMore) {
        FetchMediaListResponsePayload payload = new FetchMediaListResponsePayload(site, media,
                loadedMore, canLoadMore);
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload));
    }

    private void notifyMediaListFetched(SiteModel site, MediaError error) {
        FetchMediaListResponsePayload payload = new FetchMediaListResponsePayload(site, error);
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload));
    }

    private void notifyMediaFetched(SiteModel site, MediaModel media, MediaError error) {
        MediaPayload payload = new MediaPayload(site, media, error);
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaAction(payload));
    }

    private void notifyMediaDeleted(SiteModel site, MediaModel media, MediaError error) {
        MediaPayload payload = new MediaPayload(site, media, error);
        mDispatcher.dispatch(MediaActionBuilder.newDeletedMediaAction(payload));
    }

    private void notifyMediaUploadCanceled(MediaModel media) {
        ProgressPayload payload = new ProgressPayload(media, 0.f, false, true);
        mDispatcher.dispatch(MediaActionBuilder.newCanceledMediaUploadAction(payload));
    }

    //
    // Utility methods
    //

    /**
     * Creates a {@link MediaModel} list from a WP.com REST response to a request for all media.
     */
    private List<MediaModel> getMediaListFromRestResponse(final MultipleMediaResponse from, int localSiteId) {
        if (from == null || from.media == null) return null;

        final List<MediaModel> mediaList = new ArrayList<>();
        for (MediaWPComRestResponse mediaItem : from.media) {
            MediaModel mediaModel = getMediaFromRestResponse(mediaItem);
            mediaModel.setLocalSiteId(localSiteId);
            mediaList.add(mediaModel);
        }
        return mediaList;
    }

    /**
     * Creates a {@link MediaModel} from a WP.com REST response to a fetch request.
     */
    private MediaModel getMediaFromRestResponse(final MediaWPComRestResponse from) {
        if (from == null) return null;

        final MediaModel media = new MediaModel();
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
        if (from.thumbnails != null) {
            media.setThumbnailUrl(from.thumbnails.thumbnail);
        }
        media.setHeight(from.height);
        media.setWidth(from.width);
        media.setLength(from.length);
        media.setVideoPressGuid(from.videopress_guid);
        media.setVideoPressProcessingDone(from.videopress_processing_done);
        media.setDeleted(MediaWPComRestResponse.DELETED_STATUS.equals(from.status));
        if (!media.getDeleted()) {
            media.setUploadState(MediaModel.UploadState.UPLOADED.toString());
        } else {
            media.setUploadState(MediaModel.UploadState.DELETED.toString());
        }
        return media;
    }

    /**
     * The current REST API call (v1.1) accepts 'title', 'description', 'caption', 'alt',
     * and 'parent_id' for all media. Audio media also accepts 'artist' and 'album' attributes.
     *
     * ref https://developer.wordpress.com/docs/api/1.1/post/sites/%24site/media/
     */
    private Map<String, Object> getEditRequestParams(final MediaModel media) {
        if (media == null) return null;

        final Map<String, Object> params = new HashMap<>();
        if (media.getPostId() > 0) {
            params.put("parent_id", String.valueOf(media.getPostId()));
        }
        if (!TextUtils.isEmpty(media.getTitle())) {
            params.put("title", media.getTitle());
        }
        if (!TextUtils.isEmpty(media.getDescription())) {
            params.put("description", media.getDescription());
        }
        if (!TextUtils.isEmpty(media.getCaption())) {
            params.put("caption", media.getCaption());
        }
        if (!TextUtils.isEmpty(media.getAlt())) {
            params.put("alt", media.getAlt());
        }
        return params;
    }
}
