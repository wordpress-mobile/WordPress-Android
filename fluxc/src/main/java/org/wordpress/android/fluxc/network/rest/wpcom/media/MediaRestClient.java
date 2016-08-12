package org.wordpress.android.fluxc.network.rest.wpcom.media;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPCOMREST;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.MediaStore.ChangedMediaPayload;
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
 *     (via {@link #fetchAllMedia(long)} and {@link #fetchMedia(long, List)}</li>
 *     <li>Push new media to a WP.com site
 *     (via {@link #uploadMedia(long, MediaModel)})</li>
 *     <li>Push updates to existing media to a WP.com site
 *     (via {@link #pushMedia(long, List)})</li>
 *     <li>Delete existing media from a WP.com site
 *     (via {@link #deleteMedia(long, List)})</li>
 * </ul>
 */
public class MediaRestClient extends BaseWPComRestClient implements UploadRequestBody.ProgressListener {
    public interface MediaRestListener {
        void onMediaPulled(MediaAction cause, List<MediaModel> pulledMedia);
        void onMediaPushed(MediaAction cause, List<MediaModel> pushedMedia);
        void onMediaDeleted(MediaAction cause, List<MediaModel> deletedMedia);
        void onMediaError(MediaAction cause, Exception error);
        void onMediaUploadProgress(MediaAction cause, MediaModel media, float progress);
    }

    private MediaRestListener mListener;
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
    public void pushMedia(long siteId, List<MediaModel> media) {
    }

    /**
     * Uploads a single media item to a WP.com site.
     */
    public void uploadMedia(long siteId, MediaModel media) {
        final UploadRequestBody body = new UploadRequestBody(media, this);
        String url = WPCOMREST.sites.site(siteId).media.new_.getUrlV1_1();
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
                    AppLog.d(AppLog.T.MEDIA, "media upload successful: " + response);
                    List<MediaModel> result = new ArrayList<>();
                    result.add(body.getMedia());
                    notifyMediaPushed(MediaAction.UPLOAD_MEDIA, result);
                } else {
                    AppLog.w(AppLog.T.MEDIA, "error uploading media: " + response);
                    notifyMediaError(MediaAction.UPLOAD_MEDIA, new Exception(response.toString()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                AppLog.w(AppLog.T.MEDIA, "media upload failed: " + e);
                notifyMediaError(MediaAction.UPLOAD_MEDIA, e);
            }
        });
    }

    /**
     * Gets a list of all media items on a WP.com site.
     *
     * NOTE: Only media item data is gathered, the actual media file can be downloaded from the URL
     * provided in the response {@link MediaModel}'s (via {@link MediaModel#getUrl()}).
     */
    public void fetchAllMedia(long siteId) {
        String url = WPCOMREST.sites.site(siteId).media.getUrlV1_1();
        add(new WPComGsonRequest<>(Request.Method.GET, url, null, MediaWPComRestResponse.MultipleMediaResponse.class,
                new Response.Listener<MediaWPComRestResponse.MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MediaWPComRestResponse.MultipleMediaResponse response) {
                        List<MediaModel> mediaList = responseToMediaModelList(response);
                        notifyMediaPulled(MediaAction.FETCH_ALL_MEDIA, mediaList);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        notifyMediaError(MediaAction.FETCH_ALL_MEDIA, error);
                    }
                }
        ));
    }

    /**
     * Gets a list of media items whose media IDs match the provided list.
     */
    public void fetchMedia(long siteId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) return;

        final int count = mediaIds.size();
        final ChangedMediaPayload payload = new ChangedMediaPayload(new ArrayList<MediaModel>(), new ArrayList<Exception>(), null);
        for (final Long mediaId : mediaIds) {
            String url = WPCOMREST.sites.site(siteId).media.item(mediaId).getUrlV1_1();
            add(new WPComGsonRequest<>(Request.Method.GET, url, null, MediaWPComRestResponse.class,
                    new Response.Listener<MediaWPComRestResponse>() {
                        @Override
                        public void onResponse(MediaWPComRestResponse response) {
                            MediaModel media = responseToMediaModel(response);
                            onMediaResponse(payload, media, null, count);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            onMediaResponse(payload, null, error, count);
                        }
                    }
            ));
        }
    }

    /**
     * Deletes media from a WP.com site whose media ID is in the provided list.
     */
    public void deleteMedia(long siteId, List<MediaModel> media) {
        if (media == null || media.isEmpty()) return;

        for (MediaModel toDelete : media) {
            String url = WPCOMREST.sites.site(siteId).media.item(toDelete.getMediaId()).delete.getUrlV1_1();
            add(new WPComGsonRequest<>(Request.Method.GET, url, null, MediaWPComRestResponse.class,
                    new Response.Listener<MediaWPComRestResponse>() {
                        @Override
                        public void onResponse(MediaWPComRestResponse response) {
                            MediaModel media = responseToMediaModel(response);
                            List<MediaModel> mediaList = new ArrayList<>();
                            mediaList.add(media);
                            notifyMediaDeleted(MediaAction.DELETE_MEDIA, mediaList);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            notifyMediaError(MediaAction.DELETE_MEDIA, error);
                        }
                    }
            ));
        }
    }

    public void setListener(MediaRestListener listener) {
        mListener = listener;
    }

    private List<MediaModel> responseToMediaModelList(MediaWPComRestResponse.MultipleMediaResponse from) {
        List<MediaModel> media = new ArrayList<>();
        for (int i = 0; i < from.found; ++i) {
            media.add(i, responseToMediaModel(from.media.get(i)));
        }
        return media;
    }

    private MediaModel responseToMediaModel(MediaWPComRestResponse from) {
        MediaModel media = new MediaModel();
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
        // TODO: legacy fields
        return media;
    }

    /**
     * Helper method used by fetchMedia to track response progress
     */
    private void onMediaResponse(ChangedMediaPayload payload, MediaModel media, Exception error, int count) {
        payload.media.add(media);
        payload.errors.add(error);
        if (payload.media.size() == count) {
//            mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaAction(payload));
        }
    }

    private void notifyMediaProgress(MediaModel media, float progress) {
        if (mListener != null) {
            mListener.onMediaUploadProgress(MediaAction.UPLOAD_MEDIA, media, progress);
        }
    }

    private void notifyMediaPulled(MediaAction cause, List<MediaModel> pulledMedia) {
        if (mListener != null) {
            mListener.onMediaPulled(cause, pulledMedia);
        }
    }

    private void notifyMediaPushed(MediaAction cause, List<MediaModel> pushedMedia) {
        if (mListener != null) {
            mListener.onMediaPushed(cause, pushedMedia);
        }
    }

    private void notifyMediaDeleted(MediaAction cause, List<MediaModel> deletedMedia) {
        if (mListener != null) {
            mListener.onMediaDeleted(cause, deletedMedia);
        }
    }

    private void notifyMediaError(MediaAction cause, Exception error) {
        if (mListener != null) {
            mListener.onMediaError(cause, error);
        }
    }
}
