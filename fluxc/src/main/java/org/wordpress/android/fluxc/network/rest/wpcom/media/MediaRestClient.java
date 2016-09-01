package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.MediaNetworkListener;
import org.wordpress.android.fluxc.network.MediaNetworkListener.MediaNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

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
    private MediaNetworkListener mListener;
    private OkHttpClient mOkHttpClient;

    public MediaRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                           OkHttpClient okClient, AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okClient;
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        if (progress >= 1.0f) progress = 0.99f;
        notifyMediaProgress(media, progress);
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
                    MediaModel responseMedia = MediaUtils.mediaFromRestResponse(response);
                    if (responseMedia != null) {
                        AppLog.v(T.MEDIA, "media pushed to site: " + responseMedia.getUrl());
                        List<MediaModel> mediaList = new ArrayList<>();
                        mediaList.add(responseMedia);
                        notifyMediaPushed(MediaAction.PUSH_MEDIA, mediaList);
                    } else {
                        AppLog.w(T.MEDIA, "could not parse push media response, ID: " + media.getMediaId());
                        notifyMediaError(MediaAction.PUSH_MEDIA, media, MediaNetworkError.RESPONSE_PARSE_ERROR);
                    }
                }
            }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    if (error.type == BaseRequest.GenericErrorType.NOT_FOUND) {
                        AppLog.i(T.MEDIA, "media does not exist, uploading");
                        notifyMediaError(MediaAction.PUSH_MEDIA, media, MediaNetworkError.MEDIA_NOT_FOUND);
                    } else {
                        AppLog.e(T.MEDIA, "unhandled XMLRPC.EDIT_MEDIA error: " + error);
                        notifyMediaError(MediaAction.PUSH_MEDIA, media, MediaNetworkError.UNKNOWN);
                    }
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
                        List<MediaModel> media = MediaUtils.mediaListFromRestResponse(response);
                        if (media != null) {
                            AppLog.v(T.MEDIA, "Fetched all media for site");
                            notifyMediaFetched(MediaAction.FETCH_ALL_MEDIA, media);
                        } else {
                            AppLog.w(T.MEDIA, "could not parse Fetch all media response: " + response);
                            notifyMediaError(MediaAction.FETCH_ALL_MEDIA, null, MediaNetworkError.RESPONSE_PARSE_ERROR);
                        }
                    }
                }, new BaseRequest.BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                AppLog.v(T.MEDIA, "VolleyError Fetching media: " + error);
                notifyMediaError(MediaAction.FETCH_ALL_MEDIA, null, MediaNetworkError.UNKNOWN);
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
                            MediaModel responseMedia = MediaUtils.mediaFromRestResponse(response);
                            if (responseMedia != null) {
                                AppLog.v(T.MEDIA, "Fetched media with ID: " + media.getMediaId());
                                notifyMediaFetched(MediaAction.FETCH_MEDIA, responseMedia);
                            } else {
                                AppLog.w(T.MEDIA, "could not parse Fetch media response, ID: " + media.getMediaId());
                                MediaNetworkError error = MediaNetworkError.RESPONSE_PARSE_ERROR;
                                notifyMediaError(MediaAction.FETCH_MEDIA, media, error);
                            }
                        }
                    }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    AppLog.v(T.MEDIA, "VolleyError Fetching media: " + error);
                    notifyMediaError(MediaAction.FETCH_MEDIA, media, MediaNetworkError.UNKNOWN);
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
                            MediaModel deletedMedia = MediaUtils.mediaFromRestResponse(response);
                            if (deletedMedia != null) {
                                AppLog.v(T.MEDIA, "deleted media with ID: " + media.getMediaId());
                                notifyMediaDeleted(MediaAction.DELETE_MEDIA, deletedMedia);
                            } else {
                                AppLog.w(T.MEDIA, "could not parse delete media response, ID: " + media.getMediaId());
                                MediaNetworkError error = MediaNetworkError.RESPONSE_PARSE_ERROR;
                                notifyMediaError(MediaAction.DELETE_MEDIA, media, error);
                            }
                        }
                    }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    AppLog.v(T.MEDIA, "VolleyError deleting media (ID=" + media.getMediaId() + "): " + error);
                    if (error.type == BaseRequest.GenericErrorType.NOT_FOUND) {
                        AppLog.i(T.MEDIA, "Attempted to delete media that does not exist remotely.");
                        notifyMediaDeleted(MediaAction.DELETE_MEDIA, media);
                    } else {
                        notifyMediaError(MediaAction.DELETE_MEDIA, media, MediaNetworkError.UNKNOWN);
                    }
                }
            }));
        }
    }

    public void setListener(MediaNetworkListener listener) {
        mListener = listener;
    }

    // Used in both uploadMedia and pushMedia methods
    private void performUpload(final MediaModel media, long siteId) {
        String url = WPCOMREST.sites.site(siteId).media.new_.getUrlV1_1();
        final RestUploadRequestBody body = new RestUploadRequestBody(media, this);
        String authHeader = String.format(WPComGsonRequest.REST_AUTHORIZATION_FORMAT, getAccessToken().get());

        okhttp3.Request request = new okhttp3.Request.Builder()
                .addHeader(WPComGsonRequest.REST_AUTHORIZATION_HEADER, authHeader)
                .url(url)
                .post(body)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    AppLog.d(T.MEDIA, "media upload successful: " + response);
                    // TODO: serialize MediaModel from response and add to resultList
                    MediaModel responseMedia = media;
                    notifyMediaProgress(responseMedia, 1.f);
                } else {
                    AppLog.w(T.MEDIA, "error uploading media: " + response);
                    notifyMediaError(MediaAction.UPLOAD_MEDIA, null, MediaNetworkError.UNKNOWN);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                AppLog.w(T.MEDIA, "media upload failed: " + e);
                notifyMediaError(MediaAction.UPLOAD_MEDIA, null, MediaNetworkError.UNKNOWN);
            }
        });
    }

    private void notifyMediaFetched(MediaAction cause, MediaModel media) {
        if (mListener != null && media != null) {
            List<MediaModel> mediaList = new ArrayList<>();
            mediaList.add(media);
            mListener.onMediaFetched(cause, mediaList);
        }
    }

    private void notifyMediaFetched(MediaAction cause, List<MediaModel> media) {
        if (mListener != null) {
            mListener.onMediaFetched(cause, media);
        }
    }

    private void notifyMediaPushed(MediaAction cause, List<MediaModel> media) {
        if (mListener != null) {
            mListener.onMediaPushed(cause, media);
        }
    }

    private void notifyMediaDeleted(MediaAction cause, MediaModel media) {
        if (mListener != null && media != null) {
            List<MediaModel> mediaList = new ArrayList<>();
            mediaList.add(media);
            mListener.onMediaDeleted(cause, mediaList);
        }
    }

    private void notifyMediaError(MediaAction cause, MediaModel media, MediaNetworkError error) {
        if (mListener != null) {
            mListener.onMediaError(cause, media, error);
        }
    }

    private void notifyMediaProgress(MediaModel media, float progress) {
        if (mListener != null) {
            mListener.onMediaUploadProgress(MediaAction.UPLOAD_MEDIA, media, progress);
        }
    }
}
