package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.MediaNetworkListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.ChangedMediaPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;

import java.io.IOException;
import java.net.HttpURLConnection;
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
 *     <li>Pull existing media from a WP.com site
 *     (via {@link #pullAllMedia(long)} and {@link #pullMedia(long, List)}</li>
 *     <li>Push new media to a WP.com site
 *     (via {@link #uploadMedia(long, MediaModel)})</li>
 *     <li>Push updates to existing media to a WP.com site
 *     (via {@link #pushMedia(long, List)})</li>
 *     <li>Delete existing media from a WP.com site
 *     (via {@link #deleteMedia(long, List)})</li>
 * </ul>
 */
public class MediaRestClient extends BaseWPComRestClient implements ProgressListener {
    private MediaNetworkListener mListener;
    private OkHttpClient mOkHttpClient;

    public MediaRestClient(Dispatcher dispatcher, RequestQueue requestQueue, OkHttpClient okClient,
                           AccessToken accessToken, UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okClient;
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        notifyMediaProgress(media, progress);
    }
    /**
     * Pushes updates to existing media items on a WP.com site, creating (and uploading) new
     * media files as necessary.
     */
    public void pushMedia(final long siteId, List<MediaModel> mediaList) {
        for (final MediaModel media : mediaList) {
            String url = WPCOMREST.sites.site(siteId).media.item(media.getMediaId()).getUrlV1_1();
            add(new WPComGsonRequest<>(Method.POST, url, MediaUtils.getMediaRestParams(media),
                    MediaWPComRestResponse.class, new Listener<MediaWPComRestResponse>() {
                @Override
                public void onResponse(MediaWPComRestResponse response) {
                    MediaModel responseMedia = MediaUtils.mediaFromRestResponse(response);
                    AppLog.v(AppLog.T.MEDIA, "media pushed to site: " + responseMedia.getUrl());
                    List<MediaModel> mediaList = new ArrayList<>();
                    mediaList.add(responseMedia);
                    notifyMediaPushed(MediaAction.PUSH_MEDIA,
                            new ChangedMediaPayload(mediaList, null, null));
                }
            }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    if (error.type == BaseRequest.GenericErrorType.NOT_FOUND) {
                        AppLog.i(AppLog.T.MEDIA, "media does not exist, uploading");
                        performUpload(media, siteId);
                    } else {
                        AppLog.e(AppLog.T.MEDIA, "unhandled XMLRPC.EDIT_MEDIA error: " + error);
                    }
                }
            }));
        }
    }

    /**
     * Uploads a single media item to a WP.com site.
     */
    public void uploadMedia(long siteId, MediaModel media) {
        performUpload(media, siteId);
    }

    /**
     * Gets a list of all media items on a WP.com site.
     *
     * NOTE: Only media item data is gathered, the actual media file can be downloaded from the URL
     * provided in the response {@link MediaModel}'s (via {@link MediaModel#getUrl()}).
     */
    public void pullAllMedia(long siteId) {
        String url = WPCOMREST.sites.site(siteId).media.getUrlV1_1();
        add(new WPComGsonRequest<>(Method.GET, url, null, MultipleMediaResponse.class,
                new Listener<MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MultipleMediaResponse response) {
                        AppLog.v(AppLog.T.MEDIA, "pulled all media for site");
                        notifyMediaPulled(MediaAction.PULL_ALL_MEDIA, new ChangedMediaPayload(
                                MediaUtils.mediaListFromRestResponse(response), null, null));
                    }
                }, new BaseRequest.BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                AppLog.v(AppLog.T.MEDIA, "VolleyError pulling media: " + error);
                notifyMediaError(MediaAction.PULL_ALL_MEDIA, null, MediaNetworkListener.MediaNetworkError.UNKNOWN, error.volleyError);
            }
        }));
    }

    /**
     * Gets a list of media items whose media IDs match the provided list.
     */
    public void pullMedia(long siteId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) return;

        final int requestCount = mediaIds.size();
        for (final Long mediaId : mediaIds) {
            final MediaStore.ChangedMediaPayload payload = new MediaStore.ChangedMediaPayload(
                    new ArrayList<MediaModel>(), new ArrayList<Exception>(), null);
            String url = WPCOMREST.sites.site(siteId).media.item(mediaId).getUrlV1_1();
            add(new WPComGsonRequest<>(Method.GET, url, null, MediaWPComRestResponse.class,
                    new Listener<MediaWPComRestResponse>() {
                        @Override
                        public void onResponse(MediaWPComRestResponse response) {
                            MediaModel media = MediaUtils.mediaFromRestResponse(response);
                            AppLog.v(AppLog.T.MEDIA, "pulled media with ID: " + media.getMediaId());
                            onPullMediaResponse(payload, media, null, requestCount);
                        }
                    }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    AppLog.v(AppLog.T.MEDIA, "VolleyError pulling media: " + error);
                    onPullMediaResponse(payload, null, error, requestCount);
                }
            }));
        }
    }

    /**
     * Deletes media from a WP.com site whose media ID is in the provided list.
     */
    public void deleteMedia(long siteId, List<MediaModel> media) {
        if (media == null || media.isEmpty()) return;

        for (final MediaModel toDelete : media) {
            String url = WPCOMREST.sites.site(siteId).media.item(toDelete.getMediaId()).delete.getUrlV1_1();
            add(new WPComGsonRequest<>(Method.GET, url, null, MediaWPComRestResponse.class,
                    new Listener<MediaWPComRestResponse>() {
                        @Override
                        public void onResponse(MediaWPComRestResponse response) {
                            AppLog.v(AppLog.T.MEDIA, "deleted media with ID: " + toDelete.getMediaId());
                            List<MediaModel> mediaList = new ArrayList<>();
                            mediaList.add(MediaUtils.mediaFromRestResponse(response));
                            notifyMediaDeleted(MediaAction.DELETE_MEDIA,
                                    new ChangedMediaPayload(mediaList, null, null));
                        }
                    }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    AppLog.v(AppLog.T.MEDIA, "VolleyError deleting media: " + error);
                    notifyMediaError(MediaAction.DELETE_MEDIA, toDelete, MediaNetworkListener.MediaNetworkError.UNKNOWN, error.volleyError);
                }
            }));
        }
    }

    public void setListener(MediaNetworkListener listener) {
        mListener = listener;
    }

    // Used in both uploadMedia and pushMedia methods
    private void performUpload(MediaModel media, long siteId) {
        String url = WPCOMREST.sites.site(siteId).media.new_.getUrlV1_1();
        final RestUploadRequestBody body = new RestUploadRequestBody(media, this);
        String authHeader = String.format(WPComGsonRequest.REST_AUTHORIZATION_FORMAT, getAccessToken().get());

        okhttp3.Request request = new okhttp3.Request.Builder()
                .addHeader(WPComGsonRequest.REST_AUTHORIZATION_HEADER, authHeader)
                .url(url)
                .post(body)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    AppLog.d(AppLog.T.MEDIA, "media upload successful: " + response);
                    // TODO: serialize MediaModel from response and add to resultList
//                    MediaModel responseMedia = resToMediaModel
                    List<MediaModel> resultList = new ArrayList<>();
//                    resultList.add(responseMedia);
                    notifyMediaPushed(MediaAction.UPLOAD_MEDIA, new ChangedMediaPayload(
                            resultList, null, null));
                } else {
                    AppLog.w(AppLog.T.MEDIA, "error uploading media: " + response);
                    notifyMediaError(MediaAction.UPLOAD_MEDIA, null, MediaNetworkListener.MediaNetworkError.UNKNOWN, new Exception(response.toString()));
                }
            }

            @Override public void onFailure(Call call, IOException e) {
                AppLog.w(AppLog.T.MEDIA, "media upload failed: " + e);
                notifyMediaError(MediaAction.UPLOAD_MEDIA, null, MediaNetworkListener.MediaNetworkError.UNKNOWN, e);
            }
        });
    }

    /**
     * Helper method used by pullMedia to track response progress
     */
    private void onPullMediaResponse(ChangedMediaPayload payload, MediaModel media, BaseRequest.BaseNetworkError error, int count) {
        payload.media.add(media);
        if (error != null) {
            payload.errors.add(error.volleyError);
        }
        if (payload.media.size() == count) {
            mListener.onMediaPulled(MediaAction.PULL_MEDIA, payload.media);
        }
    }

    private void notifyMediaProgress(MediaModel media, float progress) {
        if (mListener != null) {
            mListener.onMediaUploadProgress(MediaAction.UPLOAD_MEDIA, media, progress);
        }
    }

    private void notifyMediaPulled(MediaAction cause, ChangedMediaPayload payload) {
        if (mListener != null) {
            mListener.onMediaPulled(cause, payload.media);
        }
    }

    private void notifyMediaPushed(MediaAction cause, ChangedMediaPayload payload) {
        if (mListener != null) {
            mListener.onMediaPushed(cause, payload.media);
        }
    }

    private void notifyMediaDeleted(MediaAction cause, ChangedMediaPayload payload) {
        if (mListener != null) {
            mListener.onMediaDeleted(cause, payload.media);
        }
    }

    private void notifyMediaError(MediaAction cause, MediaModel media, MediaNetworkListener.MediaNetworkError error, Exception exception) {
        if (mListener != null) {
            error.exception = exception;
            mListener.onMediaError(cause, media, error);
        }
    }
}
