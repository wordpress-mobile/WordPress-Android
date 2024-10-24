package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaFields;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.store.MediaStore.UploadStockMediaError;
import org.wordpress.android.fluxc.store.MediaStore.UploadStockMediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.UploadedStockMediaPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.MimeType;
import org.wordpress.android.fluxc.utils.WPComRestClientUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * MediaRestClient provides an interface for manipulating a WP.com site's media. It provides
 * methods to:
 *
 * <ul>
 *     <li>Fetch existing media from a WP.com site
 *     (via {@link #fetchMediaList(SiteModel, int, int, MimeType.Type)} and
 *     {@link #fetchMedia(SiteModel, MediaModel)}</li>
 *     <li>Push new media to a WP.com site
 *     (via {@link #uploadMedia(SiteModel, MediaModel)})</li>
 *     <li>Push updates to existing media to a WP.com site
 *     (via {@link #pushMedia(SiteModel, MediaModel)})</li>
 *     <li>Delete existing media from a WP.com site
 *     (via {@link #deleteMedia(SiteModel, MediaModel)})</li>
 * </ul>
 */
@Singleton
public class MediaRestClient extends BaseWPComRestClient implements ProgressListener {
    @NonNull private final OkHttpClient mOkHttpClient;
    @NonNull private final MediaResponseUtils mMediaResponseUtils;
    // this will hold which media is being uploaded by which call, in order to be able
    // to monitor multiple uploads
    @NonNull private final ConcurrentHashMap<Integer, Call> mCurrentUploadCalls = new ConcurrentHashMap<>();

    @Inject public MediaRestClient(
            Context appContext,
            Dispatcher dispatcher,
            @Named("regular") RequestQueue requestQueue,
            @NonNull @Named("regular") OkHttpClient okHttpClient,
            AccessToken accessToken,
            UserAgent userAgent,
            @NonNull MediaResponseUtils mediaResponseUtils) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okHttpClient;
        mMediaResponseUtils = mediaResponseUtils;
    }

    @Override
    public void onProgress(@NonNull MediaModel media, float progress) {
        if (mCurrentUploadCalls.containsKey(media.getId())) {
            notifyMediaProgress(media, Math.min(progress, 0.99f));
        }
    }

    public void pushMedia(@NonNull final SiteModel site, @Nullable final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            error.logMessage = "Pushed media is null";
            notifyMediaPushed(site, null, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).getUrlV1_1();

        add(WPComGsonRequest.buildPostRequest(url, getEditRequestParams(media), MediaWPComRestResponse.class,
                response -> {
                    MediaModel responseMedia = mMediaResponseUtils.getMediaFromRestResponse(response);
                    AppLog.v(T.MEDIA, "media changes pushed for " + responseMedia.getTitle());
                    responseMedia.setLocalSiteId(site.getId());
                    notifyMediaPushed(site, responseMedia, null);
                },
                error -> {
                    String errorMessage = "error editing remote media: " + error;
                    AppLog.e(T.MEDIA, errorMessage);
                    MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                    mediaError.logMessage = errorMessage;
                    notifyMediaPushed(site, media, mediaError);
                }));
    }

    /**
     * Uploads a single media item to a WP.com site.
     */
    public void uploadMedia(@NonNull final SiteModel site, @Nullable final MediaModel media) {
        if (media == null || media.getId() == 0) {
            // we can't have a MediaModel without an ID - otherwise we can't keep track of them.
            MediaError error = new MediaError(MediaErrorType.INVALID_ID);
            if (media == null) {
                error.logMessage = "Media object is null on upload";
            } else {
                error.logMessage = "Media ID is 0 on upload";
            }
            notifyMediaUploaded(media, error);
            return;
        }

        if (!MediaUtils.canReadFile(media.getFilePath())) {
            MediaError error = new MediaError(MediaErrorType.FS_READ_PERMISSION_DENIED);
            error.logMessage = "Can't read file on upload";
            notifyMediaUploaded(media, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.new_.getUrlV1_1();
        RestUploadRequestBody body = new RestUploadRequestBody(media, getEditRequestParams(media), this);

        // Abort upload if it exceeds the site upload limit
        if (site.hasMaxUploadSize() && body.contentLength() > site.getMaxUploadSize()) {
            String errorMessage = "Media size of " + body.contentLength() + " exceeds site limit of "
                                  + site.getMaxUploadSize();
            AppLog.d(T.MEDIA, errorMessage);
            MediaError error = new MediaError(MediaErrorType.EXCEEDS_FILESIZE_LIMIT);
            error.logMessage = errorMessage;
            notifyMediaUploaded(media, error);
            return;
        }

        // Abort upload if it exceeds the 'safe' memory limit for the site
        double maxFilesizeForMemoryLimit = MediaUtils.getMaxFilesizeForMemoryLimit(site.getMemoryLimit());
        if (site.hasMemoryLimit() && body.contentLength() > maxFilesizeForMemoryLimit) {
            String errorMessage = "Media size of " + body.contentLength() + " exceeds safe memory limit of "
                                  + maxFilesizeForMemoryLimit + " for this site";
            AppLog.d(T.MEDIA, errorMessage);
            MediaError error = new MediaError(MediaErrorType.EXCEEDS_MEMORY_LIMIT);
            error.logMessage = errorMessage;
            notifyMediaUploaded(media, error);
            return;
        }

        // Abort upload if it exceeds the space quota limit for the site
        if (site.hasDiskSpaceQuotaInformation() && body.contentLength() > site.getSpaceAvailable()) {
            String errorMessage = "Media size of " + body.contentLength() + " exceeds disk space quota remaining  "
                                  + site.getSpaceAvailable() + " for this site";
            AppLog.d(T.MEDIA, errorMessage);
            MediaError error = new MediaError(MediaErrorType.EXCEEDS_SITE_SPACE_QUOTA_LIMIT);
            error.logMessage = errorMessage;
            notifyMediaUploaded(media, error);
            return;
        }

        String authHeader = String.format(WPComGsonRequest.REST_AUTHORIZATION_FORMAT, getAccessToken().get());

        Request request = new Request.Builder()
                .addHeader(WPComGsonRequest.REST_AUTHORIZATION_HEADER, authHeader)
                .addHeader("User-Agent", mUserAgent.toString())
                .url(url)
                .post(body)
                .build();

        // Try to add locale query param
        HttpUrl httpUrl = WPComRestClientUtils.getHttpUrlWithLocale(mAppContext, url);

        if (null != httpUrl) {
            request = request.newBuilder()
                             .url(httpUrl)
                             .build();
        } else {
            AppLog.d(T.MEDIA, "Could not add locale query param for url '" + url + "'.");
        }

        Call call = mOkHttpClient.newCall(request);
        mCurrentUploadCalls.put(media.getId(), call);

        AppLog.d(T.MEDIA, "starting upload for: " + media.getId());
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        String errorMessage = "error uploading media, response body was empty " + response;
                        AppLog.e(T.MEDIA, errorMessage);
                        MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                        error.logMessage = errorMessage;
                        notifyMediaUploaded(media, error);
                        return;
                    }

                    AppLog.d(T.MEDIA, "media upload successful: " + response);
                    String jsonBody = responseBody.string();

                    Gson gson = new Gson();
                    JsonReader reader = new JsonReader(new StringReader(jsonBody));
                    reader.setLenient(true);
                    MultipleMediaResponse mediaResponse = gson.fromJson(reader, MultipleMediaResponse.class);

                    List<MediaModel> responseMedia = mMediaResponseUtils.getMediaListFromRestResponse(
                            mediaResponse,
                            site.getId());
                    if (!responseMedia.isEmpty()) {
                        MediaModel uploadedMedia = responseMedia.get(0);
                        uploadedMedia.setId(media.getId());
                        uploadedMedia.setLocalPostId(media.getLocalPostId());
                        uploadedMedia.setMarkedLocallyAsFeatured(media.getMarkedLocallyAsFeatured());

                        notifyMediaUploaded(uploadedMedia, null);
                    } else {
                        MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                        error.logMessage = "Failed to parse response on uploadMedia";
                        notifyMediaUploaded(media, error);
                    }
                } else {
                    AppLog.e(T.MEDIA, "error uploading media: " + response.message());

                    MediaError error = parseUploadError(response, site);

                    if (error.type == MediaErrorType.BAD_REQUEST) {
                        AppLog.e(T.MEDIA, "media upload error message: " + error.message);
                    }

                    notifyMediaUploaded(media, error);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String message = "media upload failed: " + e;
                AppLog.w(T.MEDIA, message);
                if (!mCurrentUploadCalls.containsKey(media.getId())) {
                    // This call has already been removed from the in-progress list - probably because it was cancelled
                    // In that case this has already been handled and there's nothing to do
                    return;
                }

                MediaError error = MediaError.fromIOException(e);
                error.logMessage = message;
                notifyMediaUploaded(media, error);
            }
        });
    }

    /**
     * Gets a list of media items given the offset on a WP.com site.
     * <p>
     * NOTE: Only media item data is gathered, the actual media file can be downloaded from the URL
     * provided in the response {@link MediaModel}'s (via {@link MediaModel#getUrl()}).
     */
    public void fetchMediaList(
            @NonNull final SiteModel site,
            final int number,
            final int offset,
            @Nullable final MimeType.Type mimeType) {
        final Map<String, String> params = new HashMap<>();
        params.put("number", String.valueOf(number));
        if (offset > 0) {
            params.put("offset", String.valueOf(offset));
        }
        if (mimeType != null) {
            params.put("mime_type", mimeType.getValue());
        }
        String url = WPCOMREST.sites.site(site.getSiteId()).media.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, params, MultipleMediaResponse.class,
                response -> {
                    List<MediaModel> mediaList = mMediaResponseUtils.getMediaListFromRestResponse(
                            response,
                            site.getId());
                    AppLog.v(T.MEDIA, "Fetched media list for site with size: " + mediaList.size());
                    boolean canLoadMore = mediaList.size() == number;
                    notifyMediaListFetched(site, mediaList, offset > 0, canLoadMore, mimeType);
                },
                error -> {
                    String errorMessage = "VolleyError Fetching media: " + error;
                    AppLog.e(T.MEDIA, errorMessage);
                    MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                    mediaError.message = error.message;
                    mediaError.logMessage = error.apiError;
                    notifyMediaListFetched(site, mediaError, mimeType);
                }));
    }

    /**
     * Gets a list of media items whose media IDs match the provided list.
     */
    public void fetchMedia(@NonNull final SiteModel site, @Nullable final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            error.logMessage = "Requested media is null";
            notifyMediaFetched(site, null, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, MediaWPComRestResponse.class,
                response -> {
                    MediaModel responseMedia = mMediaResponseUtils.getMediaFromRestResponse(response);
                    responseMedia.setLocalSiteId(site.getId());
                    AppLog.v(T.MEDIA, "Fetched media with ID: " + media.getMediaId());
                    notifyMediaFetched(site, responseMedia, null);
                },
                error -> {
                    AppLog.e(T.MEDIA, "VolleyError Fetching media: " + error);
                    MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                    mediaError.message = error.message;
                    mediaError.logMessage = error.apiError;
                    notifyMediaFetched(site, media, mediaError);
                }));
    }

    /**
     * Deletes media from a WP.com site whose media ID is in the provided list.
     */
    public void deleteMedia(@NonNull final SiteModel site, @Nullable final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            error.logMessage = "Null media on delete";
            notifyMediaDeleted(site, null, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).delete.getUrlV1_1();
        add(WPComGsonRequest.buildPostRequest(url, null, MediaWPComRestResponse.class,
                response -> {
                    mMediaResponseUtils.getMediaFromRestResponse(response);
                    AppLog.v(T.MEDIA, "deleted media: " + media.getTitle());
                    notifyMediaDeleted(site, media, null);
                },
                error -> {
                    AppLog.e(T.MEDIA, "VolleyError deleting media (ID=" + media.getMediaId() + "): " + error);
                    MediaErrorType mediaErrorType = MediaErrorType.fromBaseNetworkError(error);
                    if (mediaErrorType == MediaErrorType.NOT_FOUND) {
                        AppLog.i(T.MEDIA, "Attempted to delete media that does not exist remotely.");
                    }
                    MediaError mediaError = new MediaError(mediaErrorType);
                    mediaError.message = error.message;
                    mediaError.logMessage = error.apiError;
                    notifyMediaDeleted(site, media, mediaError);
                }));
    }

    public void cancelUpload(@Nullable final MediaModel media) {
        if (media == null) {
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            error.logMessage = "Null media on cancel upload";
            notifyMediaUploaded(null, error);
            return;
        }

        // cancel in-progress upload if necessary
        Call correspondingCall = mCurrentUploadCalls.get(media.getId());
        if (correspondingCall != null && correspondingCall.isExecuted() && !correspondingCall.isCanceled()) {
            AppLog.d(T.MEDIA, "Canceled in-progress upload: " + media.getFileName());
            removeCallFromCurrentUploadsMap(media.getId());
            correspondingCall.cancel();

            // report the upload was successfully cancelled
            notifyMediaUploadCanceled(media);
        }
    }

    private void removeCallFromCurrentUploadsMap(int id) {
        mCurrentUploadCalls.remove(id);
        AppLog.d(T.MEDIA, "mediaRestClient: removed id: " + id + " from current uploads, remaining: "
                          + mCurrentUploadCalls.size());
    }

    public void uploadStockMedia(@NonNull final SiteModel site,
                                 @NonNull List<StockMediaModel> stockMediaList) {
        String url = WPCOMREST.sites.site(site.getSiteId()).external_media_upload.getUrlV1_1();

        JsonArray jsonBody = new JsonArray();
        for (StockMediaModel stockMedia : stockMediaList) {
            JsonObject json = new JsonObject();
            json.addProperty("url", StringUtils.notNullStr(stockMedia.getUrl()));
            json.addProperty("name", StringUtils.notNullStr(stockMedia.getName()));
            json.addProperty("title", StringUtils.notNullStr(stockMedia.getTitle()));
            jsonBody.add(json.toString());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("service", "pexels");
        body.put("external_ids", jsonBody);

        WPComGsonRequest<MultipleMediaResponse> request = WPComGsonRequest.buildPostRequest(
                url,
                body,
                MultipleMediaResponse.class,
                response -> {
                    // response is a list of media, exactly like that of MediaRestClient.fetchMediaList()
                    List<MediaModel> mediaList = mMediaResponseUtils.getMediaListFromRestResponse(
                            response,
                            site.getId());
                    UploadedStockMediaPayload payload = new UploadedStockMediaPayload(site, mediaList);
                    mDispatcher.dispatch(MediaActionBuilder.newUploadedStockMediaAction(payload));
                },
                error -> {
                    AppLog.e(T.MEDIA, "VolleyError uploading stock media: " + error);
                    UploadStockMediaError mediaError = new UploadStockMediaError(
                            UploadStockMediaErrorType.fromNetworkError(error), error.message);
                    UploadedStockMediaPayload payload = new UploadedStockMediaPayload(site, mediaError);
                    mDispatcher.dispatch(MediaActionBuilder.newUploadedStockMediaAction(payload));
                });

        add(request);
    }

    //
    // Helper methods to dispatch media actions
    //
    @NonNull
    private MediaError parseUploadError(
            @NonNull Response response,
            @NonNull SiteModel siteModel) {
        MediaError mediaError = new MediaError(MediaErrorType.fromHttpStatusCode(response.code()));
        mediaError.statusCode = response.code();
        mediaError.logMessage = response.message();
        if (mediaError.type == MediaErrorType.REQUEST_TOO_LARGE) {
            // 413 (Request too large) errors are coming from the web server and are not an API response like the rest
            mediaError.message = response.message();
            return mediaError;
        }

        try {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                AppLog.e(T.MEDIA, "error uploading media, response body was empty " + response);
                mediaError.type = MediaErrorType.PARSE_ERROR;
                return mediaError;
            }
            JSONObject body = new JSONObject(responseBody.string());
            // Can be an array or errors
            if (body.has("errors")) {
                JSONArray errors = body.getJSONArray("errors");
                if (errors.length() == 1) {
                    JSONObject error = errors.getJSONObject(0);
                    // error.getString("error")) is always "upload_error"
                    if (error.has("message")) {
                        mediaError.message = error.getString("message");
                        mediaError.logMessage = error.getString("message");
                    }
                }
            }
            // Or an object
            if (body.has("message")) {
                mediaError.message = body.getString("message");
                mediaError.logMessage = body.getString("message");
            }

            if (!siteModel.isWPCom()) {
                // TODO : temporary fix for "big" media uploads on Jetpack connected site
                // See https://github.com/wordpress-mobile/WordPress-FluxC-Android/issues/402
                // Tried to upload a media that's too large (larger than the site's max_upload_filesize)
                if (body.has("error")) {
                    String error = body.getString("error");
                    if ("invalid_hmac".equals(error)) {
                        mediaError.type = MediaErrorType.REQUEST_TOO_LARGE;
                    }
                    mediaError.logMessage = error;
                }
            }
        } catch (JSONException | IOException e) {
            // no op
            mediaError.logMessage = e.getMessage();
        }
        return mediaError;
    }

    private void notifyMediaPushed(
            @NonNull SiteModel site,
            @Nullable MediaModel media,
            @Nullable MediaError error) {
        MediaPayload payload = new MediaPayload(site, media, error);
        mDispatcher.dispatch(MediaActionBuilder.newPushedMediaAction(payload));
    }

    private void notifyMediaProgress(@NonNull MediaModel media, float progress) {
        ProgressPayload payload = new ProgressPayload(media, progress, false, null);
        mDispatcher.dispatch(UploadActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaUploaded(@Nullable MediaModel media, @Nullable MediaError error) {
        if (media != null) {
            media.setUploadState(error == null ? MediaUploadState.UPLOADED : MediaUploadState.FAILED);
            removeCallFromCurrentUploadsMap(media.getId());
        }

        ProgressPayload payload = new ProgressPayload(media, 1.f, error == null, error);
        mDispatcher.dispatch(UploadActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaListFetched(
            @NonNull SiteModel site,
            @NonNull List<MediaModel> media,
            boolean loadedMore,
            boolean canLoadMore,
            @Nullable MimeType.Type mimeType) {
        FetchMediaListResponsePayload payload = new FetchMediaListResponsePayload(site, media,
                loadedMore, canLoadMore, mimeType);
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload));
    }

    private void notifyMediaListFetched(
            @NonNull SiteModel site,
            @NonNull MediaError error,
            @Nullable MimeType.Type mimeType) {
        FetchMediaListResponsePayload payload = new FetchMediaListResponsePayload(site, error, mimeType);
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload));
    }

    private void notifyMediaFetched(
            @NonNull SiteModel site,
            @Nullable MediaModel media,
            @Nullable MediaError error) {
        MediaPayload payload = new MediaPayload(site, media, error);
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaAction(payload));
    }

    private void notifyMediaDeleted(
            @NonNull SiteModel site,
            @Nullable MediaModel media,
            @Nullable MediaError error) {
        MediaPayload payload = new MediaPayload(site, media, error);
        mDispatcher.dispatch(MediaActionBuilder.newDeletedMediaAction(payload));
    }

    private void notifyMediaUploadCanceled(@NonNull MediaModel media) {
        ProgressPayload payload = new ProgressPayload(media, 0.f, false, true);
        mDispatcher.dispatch(MediaActionBuilder.newCanceledMediaUploadAction(payload));
    }

    //
    // Utility methods
    //

    /**
     * The current REST API call (v1.1) accepts 'title', 'description', 'caption', 'alt',
     * and 'parent_id' for all media. Audio media also accepts 'artist' and 'album' attributes.
     * <p>
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/post/sites/%24site/media/">documentation</a>
     */
    @NonNull
    private Map<String, Object> getEditRequestParams(@NonNull final MediaModel media) {
        MediaFields[] fieldsToUpdate = media.getFieldsToUpdate();

        final Map<String, Object> params = new HashMap<>();
        for (MediaFields field : fieldsToUpdate) {
            switch (field) {
                case PARENT_ID:
                    if (media.getPostId() > 0) {
                        params.put(MediaFields.PARENT_ID.getFieldName(), String.valueOf(media.getPostId()));
                    }
                    break;
                case TITLE:
                    if (!TextUtils.isEmpty(media.getTitle())) {
                        params.put(MediaFields.TITLE.getFieldName(), media.getTitle());
                    }
                    break;
                case DESCRIPTION:
                    if (!TextUtils.isEmpty(media.getDescription())) {
                        params.put(MediaFields.DESCRIPTION.getFieldName(), media.getDescription());
                    }
                    break;
                case CAPTION:
                    if (!TextUtils.isEmpty(media.getCaption())) {
                        params.put(MediaFields.CAPTION.getFieldName(), media.getCaption());
                    }
                    break;
                case ALT:
                    if (!TextUtils.isEmpty(media.getAlt())) {
                        params.put(MediaFields.ALT.getFieldName(), media.getAlt());
                    }
                    break;
            }
        }
        return params;
    }
}
