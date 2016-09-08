package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
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
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;

/**
 * MediaRestClient provides an interface for manipulating a WP.com site's media. It provides
 * methods to:
 *
 * <ul>
 *     <li>Fetch existing media from a WP.com site
 *     (via {@link #fetchAllMedia(SiteModel)} and {@link #fetchMedia(SiteModel, List)}</li>
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

    public MediaRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                           OkHttpClient okClient, AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okClient;
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        notifyMediaProgress(media, Math.max(progress, 0.99f), null);
    }

    /**
     * Pushes updates to existing media items on a WP.com site, creating (and uploading) new
     * media files as necessary.
     */
    public void pushMedia(final SiteModel site, final List<MediaModel> mediaToPush) {
        for (final MediaModel media : mediaToPush) {
            String url = WPCOMREST.sites.site(site.getSiteId()).media.item(media.getMediaId()).getUrlV1_1();
            add(new WPComGsonRequest<>(Method.POST, url, MediaUtils.getMediaRestParams(media),
                    MediaWPComRestResponse.class, new Listener<MediaWPComRestResponse>() {
                @Override
                public void onResponse(MediaWPComRestResponse response) {
                    MediaModel responseMedia = MediaUtils.mediaFromRestResponse(response, site.getSiteId());
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
    public void fetchAllMedia(final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).media.getUrlV1_1();
        add(new WPComGsonRequest<>(Method.GET, url, null, MultipleMediaResponse.class,
                new Listener<MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MultipleMediaResponse response) {
                        List<MediaModel> media = MediaUtils.mediaListFromRestResponse(response, site.getSiteId());
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
            add(new WPComGsonRequest<>(Method.GET, url, null, MediaWPComRestResponse.class,
                    new Listener<MediaWPComRestResponse>() {
                        @Override
                        public void onResponse(MediaWPComRestResponse response) {
                            MediaModel responseMedia = MediaUtils.mediaFromRestResponse(response, site.getSiteId());
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
            add(new WPComGsonRequest<>(Method.GET, url, null, MediaWPComRestResponse.class,
                    new Listener<MediaWPComRestResponse>() {
                        @Override
                        public void onResponse(MediaWPComRestResponse response) {
                            MediaModel deletedMedia = MediaUtils.mediaFromRestResponse(response, site.getSiteId());
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

    // Used in both uploadMedia and pushMedia methods
    private void performUpload(final MediaModel media, final long siteId) {
        String url = WPCOMREST.sites.site(siteId).media.new_.getUrlV1_1();
        RestUploadRequestBody body = new RestUploadRequestBody(media, this);
        String authHeader = String.format(WPComGsonRequest.REST_AUTHORIZATION_FORMAT, getAccessToken().get());

        okhttp3.Request request = new okhttp3.Request.Builder()
                .addHeader(WPComGsonRequest.REST_AUTHORIZATION_HEADER, authHeader)
                .url(url)
                .post(body)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    AppLog.d(T.MEDIA, "media upload successful: " + response);
                    String jsonBody = response.body().string();
                    MultipleMediaResponse mediaResponse =
                            new Gson().fromJson(jsonBody, MultipleMediaResponse.class);
                    List<MediaModel> responseMedia = MediaUtils.mediaListFromRestResponse(mediaResponse, siteId);
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
        AppLog.v(AppLog.T.MEDIA, "Progress update on upload of " + media.getTitle() + ": " + progress);
        MediaStore.ProgressPayload payload = new MediaStore.ProgressPayload(media, progress, false);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaUploaded(MediaModel media, MediaError error) {
        MediaStore.ProgressPayload payload = new MediaStore.ProgressPayload(media, 1.f, error == null);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }
}
