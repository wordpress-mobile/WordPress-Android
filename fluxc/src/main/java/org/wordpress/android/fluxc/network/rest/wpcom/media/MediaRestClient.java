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

import java.util.ArrayList;
import java.util.List;

public class MediaRestClient extends BaseWPComRestClient {
    public interface MediaRestListener {
        void onMediaPulled(MediaAction cause, List<MediaModel> pulledMedia);
        void onMediaPushed(MediaAction cause, List<MediaModel> pushedMedia);
        void onMediaDeleted(MediaAction cause, List<MediaModel> deletedMedia);
        void onMediaError(MediaAction cause, VolleyError error);
    }

    private MediaRestListener mListener;

    public MediaRestClient(Dispatcher dispatcher, RequestQueue requestQueue,
                           AccessToken accessToken, UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
    }

    public void setListener(MediaRestListener listener) {
        mListener = listener;
    }

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

    public void deleteMedia(long siteId, List<MediaModel> media) {
        if (media == null || media.isEmpty()) return;

        final int count = media.size();
        final ChangedMediaPayload payload = new ChangedMediaPayload(new ArrayList<MediaModel>(), new ArrayList<Exception>(), null);
        for (MediaModel toDelete : media) {
            String url = WPCOMREST.sites.site(siteId).media.item(toDelete.getMediaId()).delete.getUrlV1_1();
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
                            notifyMediaError(MediaAction.DELETE_MEDIA, error);
                        }
                    }
            ));
        }
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

    private void notifyMediaError(MediaAction cause, VolleyError error) {
        if (mListener != null) {
            mListener.onMediaError(cause, error);
        }
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
}
