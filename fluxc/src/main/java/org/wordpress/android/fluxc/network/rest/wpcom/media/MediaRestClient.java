package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private OkHttpClient mOkHttpClient;
    // this will hold which media is being uploaded by which call, in order to be able
    // to monitor multiple uploads
    private ConcurrentHashMap<Integer, Call> mCurrentUploadCalls = new ConcurrentHashMap<>();

    public MediaRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                           OkHttpClient okHttpClient, AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okHttpClient;
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        if (mCurrentUploadCalls.containsKey(media.getId())) {
            notifyMediaProgress(media, Math.min(progress, 0.99f), null);
        }
    }

    public void pushMedia(final SiteModel site, final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyMediaPushed(site, null, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).getUrlV1_1();

        add(WPComGsonRequest.buildPostRequest(url, getEditRequestParams(media), MediaWPComRestResponse.class,
                new Listener<MediaWPComRestResponse>() {
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
                }, new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AppLog.e(T.MEDIA, "error editing remote media: " + error);
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        notifyMediaPushed(site, media, mediaError);
                    }
                }
        ));
    }

    /**
     * Uploads a single media item to a WP.com site.
     */
    public void uploadMedia(final SiteModel site, final MediaModel media) {
        if (media == null || media.getId() == 0) {
            // we can't have a MediaModel without an ID - otherwise we can't keep track of them.
            MediaError error = new MediaError(MediaErrorType.INVALID_ID);
            notifyMediaUploaded(media, error);
            return;
        }

        if (!MediaUtils.canReadFile(media.getFilePath())) {
            MediaError error = new MediaError(MediaErrorType.FS_READ_PERMISSION_DENIED);
            notifyMediaUploaded(media, error);
            return;
        }

        String url = WPCOMREST.sites.site(site.getSiteId()).media.new_.getUrlV1_1();
        RestUploadRequestBody body = new RestUploadRequestBody(media, getEditRequestParams(media), this);

        // Abort upload if it exceeds the site upload limit
        if (site.hasMaxUploadSize() && body.contentLength() > site.getMaxUploadSize()) {
            AppLog.d(T.MEDIA, "Media size of " + body.contentLength() + " exceeds site limit of "
                    + site.getMaxUploadSize());
            MediaError error = new MediaError(MediaErrorType.EXCEEDS_FILESIZE_LIMIT);
            notifyMediaUploaded(media, error);
            return;
        }

        // Abort upload if it exceeds the 'safe' memory limit for the site
        double maxFilesizeForMemoryLimit = MediaUtils.getMaxFilesizeForMemoryLimit(site.getMemoryLimit());
        if (site.hasMemoryLimit() && body.contentLength() > maxFilesizeForMemoryLimit) {
            AppLog.d(T.MEDIA, "Media size of " + body.contentLength() + " exceeds safe memory limit of "
                    + maxFilesizeForMemoryLimit + " for this site");
            MediaError error = new MediaError(MediaErrorType.EXCEEDS_MEMORY_LIMIT);
            notifyMediaUploaded(media, error);
            return;
        }

        // Abort upload if it exceeds the space quota limit for the site
        if (site.hasDiskSpaceQuotaInformation() && body.contentLength() > site.getSpaceAvailable()) {
            AppLog.d(T.MEDIA, "Media size of " + body.contentLength() + " exceeds disk space quota remaining  "
                              + site.getSpaceAvailable() + " for this site");
            MediaError error = new MediaError(MediaErrorType.EXCEEDS_SITE_SPACE_QUOTA_LIMIT);
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
        HttpUrl httpUrl = getHttpUrlWithLocale(url);

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
                        AppLog.e(T.MEDIA, "error uploading media, response body was empty " + response);
                        notifyMediaUploaded(media, new MediaError(MediaErrorType.PARSE_ERROR));
                        return;
                    }

                    AppLog.d(T.MEDIA, "media upload successful: " + response);
                    String jsonBody = responseBody.string();

                    Gson gson = new Gson();
                    JsonReader reader = new JsonReader(new StringReader(jsonBody));
                    reader.setLenient(true);
                    MultipleMediaResponse mediaResponse = gson.fromJson(reader, MultipleMediaResponse.class);

                    List<MediaModel> responseMedia = getMediaListFromRestResponse(mediaResponse, site.getId());
                    if (responseMedia != null && !responseMedia.isEmpty()) {
                        MediaModel uploadedMedia = responseMedia.get(0);
                        uploadedMedia.setId(media.getId());
                        uploadedMedia.setLocalPostId(media.getLocalPostId());
                        uploadedMedia.setMarkedLocallyAsFeatured(media.getMarkedLocallyAsFeatured());

                        notifyMediaUploaded(uploadedMedia, null);
                    } else {
                        MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                        notifyMediaUploaded(media, error);
                    }
                } else {
                    AppLog.e(T.MEDIA, "error uploading media: " + response.message());

                    MediaError error = parseUploadError(response, site);

                    if (null != error && error.type == MediaErrorType.BAD_REQUEST) {
                        AppLog.e(T.MEDIA, "media upload error message: " + error.message);
                    }

                    notifyMediaUploaded(media, error);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLog.w(T.MEDIA, "media upload failed: " + e);
                if (!mCurrentUploadCalls.containsKey(media.getId())) {
                    // This call has already been removed from the in-progress list - probably because it was cancelled
                    // In that case this has already been handled and there's nothing to do
                    return;
                }

                MediaError error = MediaError.fromIOException(e);
                notifyMediaUploaded(media, error);
            }
        });
    }

    /**
     * Gets a list of media items given the offset on a WP.com site.
     *
     * NOTE: Only media item data is gathered, the actual media file can be downloaded from the URL
     * provided in the response {@link MediaModel}'s (via {@link MediaModel#getUrl()}).
     */
    public void fetchMediaList(final SiteModel site, final int number, final int offset, final MimeType.Type mimeType) {
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
                new Listener<MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MultipleMediaResponse response) {
                        List<MediaModel> mediaList = getMediaListFromRestResponse(response, site.getId());
                        if (mediaList != null) {
                            AppLog.v(T.MEDIA, "Fetched media list for site with size: " + mediaList.size());
                            boolean canLoadMore = mediaList.size() == number;
                            notifyMediaListFetched(site, mediaList, offset > 0, canLoadMore, mimeType);
                        } else {
                            AppLog.w(T.MEDIA, "could not parse Fetch all media response: " + response);
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            notifyMediaListFetched(site, error, mimeType);
                        }
                    }
                }, new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AppLog.e(T.MEDIA, "VolleyError Fetching media: " + error);
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        notifyMediaListFetched(site, mediaError, mimeType);
                    }
                }
        ));
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
                }, new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AppLog.e(T.MEDIA, "VolleyError Fetching media: " + error);
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        notifyMediaFetched(site, media, mediaError);
                    }
                }
            ));
    }

    /**
     * Deletes media from a WP.com site whose media ID is in the provided list.
     */
    public void deleteMedia(final SiteModel site, final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyMediaDeleted(site, null, error);
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
                }, new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AppLog.e(T.MEDIA, "VolleyError deleting media (ID=" + media.getMediaId() + "): " + error);
                        MediaErrorType mediaError = MediaErrorType.fromBaseNetworkError(error);
                        if (mediaError == MediaErrorType.NOT_FOUND) {
                            AppLog.i(T.MEDIA, "Attempted to delete media that does not exist remotely.");
                        }
                        notifyMediaDeleted(site, media, new MediaError(mediaError));
                    }
                }
            ));
    }

    public void cancelUpload(final MediaModel media) {
        if (media == null) {
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
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

        WPComGsonRequest request = WPComGsonRequest.buildPostRequest(url, body, MultipleMediaResponse.class,
                new com.android.volley.Response.Listener<MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MultipleMediaResponse response) {
                        // response is a list of media, exactly like that of MediaRestClient.fetchMediaList()
                        List<MediaModel> mediaList = getMediaListFromRestResponse(response, site.getId());
                        UploadedStockMediaPayload payload = new UploadedStockMediaPayload(site, mediaList);
                        mDispatcher.dispatch(MediaActionBuilder.newUploadedStockMediaAction(payload));
                    }
                }, new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AppLog.e(AppLog.T.MEDIA, "VolleyError uploading stock media: " + error);
                        UploadStockMediaError mediaError = new UploadStockMediaError(
                                UploadStockMediaErrorType.fromNetworkError(error), error.message);
                        UploadedStockMediaPayload payload = new UploadedStockMediaPayload(site, mediaError);
                        mDispatcher.dispatch(MediaActionBuilder.newUploadedStockMediaAction(payload));
                    }
                });

        add(request);
    }

    //
    // Helper methods to dispatch media actions
    //
    private MediaError parseUploadError(Response response, SiteModel siteModel) {
        MediaError mediaError = new MediaError(MediaErrorType.fromHttpStatusCode(response.code()));

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
                    }
                }
            }
            // Or an object
            if (body.has("message")) {
                mediaError.message = body.getString("message");
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
                }
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
        mDispatcher.dispatch(UploadActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaUploaded(MediaModel media, MediaError error) {
        if (media != null) {
            media.setUploadState(error == null ? MediaUploadState.UPLOADED : MediaUploadState.FAILED);
            removeCallFromCurrentUploadsMap(media.getId());
        }

        ProgressPayload payload = new ProgressPayload(media, 1.f, error == null, error);
        mDispatcher.dispatch(UploadActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaListFetched(SiteModel site,
                                        @NonNull List<MediaModel> media,
                                        boolean loadedMore,
                                        boolean canLoadMore,
                                        MimeType.Type mimeType) {
        FetchMediaListResponsePayload payload = new FetchMediaListResponsePayload(site, media,
                loadedMore, canLoadMore, mimeType);
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload));
    }

    private void notifyMediaListFetched(SiteModel site, MediaError error, MimeType.Type mimeType) {
        FetchMediaListResponsePayload payload = new FetchMediaListResponsePayload(site, error, mimeType);
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
        media.setTitle(StringEscapeUtils.unescapeHtml4(from.title));
        media.setCaption(StringEscapeUtils.unescapeHtml4(from.caption));
        media.setDescription(StringEscapeUtils.unescapeHtml4(from.description));
        media.setAlt(StringEscapeUtils.unescapeHtml4(from.alt));
        if (from.thumbnails != null) {
            if (!TextUtils.isEmpty(from.thumbnails.fmt_std)) {
                media.setThumbnailUrl(from.thumbnails.fmt_std);
            } else {
                media.setThumbnailUrl(from.thumbnails.thumbnail);
            }
        }
        media.setHeight(from.height);
        media.setWidth(from.width);
        media.setLength(from.length);
        media.setVideoPressGuid(from.videopress_guid);
        media.setVideoPressProcessingDone(from.videopress_processing_done);
        media.setDeleted(MediaWPComRestResponse.DELETED_STATUS.equals(from.status));
        if (!media.getDeleted()) {
            media.setUploadState(MediaUploadState.UPLOADED);
        } else {
            media.setUploadState(MediaUploadState.DELETED);
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
