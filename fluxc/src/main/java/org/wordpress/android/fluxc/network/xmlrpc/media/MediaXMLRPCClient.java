package org.wordpress.android.fluxc.network.xmlrpc.media;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.HTTPAuthModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCException;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCFault;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.XMLSerializerUtils;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaFilter;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;

import static org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.fromHttpStatusCode;

public class MediaXMLRPCClient extends BaseXMLRPCClient implements ProgressListener {
    private OkHttpClient mOkHttpClient;
    // track the network call to support cancelling
    private Call mCurrentUploadCall;

    private int mFetchAllOffset = 0;
    private List<MediaModel> mFetchedMedia = new ArrayList<>();

    public MediaXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, OkHttpClient.Builder okClientBuilder,
                             AccessToken accessToken, UserAgent userAgent,
                             HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
        mOkHttpClient = okClientBuilder.build();
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        notifyMediaProgress(media, Math.min(0.99f, progress), null);
    }

    public void pushMedia(final SiteModel site, final MediaModel media) {
            List<Object> params = getBasicParams(site, media);
            params.add(getEditMediaFields(media));
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.EDIT_POST, params, new Listener() {
                @Override
                public void onResponse(Object response) {
                    // response should be a boolean indicating result of push request
                    if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                        AppLog.w(T.MEDIA, "could not parse XMLRPC.EDIT_MEDIA response: " + response);
                        MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                        notifyMediaPushed(site, media, error);
                        return;
                    }

                    // success!
                    AppLog.i(T.MEDIA, "Media updated on remote: " + media.getTitle());
                    notifyMediaPushed(site, media, null);
                }
            }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    AppLog.e(T.MEDIA, "error response to XMLRPC.EDIT_MEDIA request: " + error);
                    if (is404Response(error)) {
                        AppLog.e(T.MEDIA, "media does not exist, no need to report error");
                        notifyMediaPushed(site, media, null);
                    } else {
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        notifyMediaPushed(site, media, mediaError);
                    }
                }
            }));
    }

    /**
     * ref: https://codex.wordpress.org/XML-RPC_WordPress_API/Media#wp.uploadFile
     */
    public void uploadMedia(final SiteModel site, final MediaModel media) {
        URL xmlrpcUrl;
        try {
            xmlrpcUrl = new URL(site.getXmlRpcUrl());
        } catch (MalformedURLException e) {
            AppLog.w(T.MEDIA, "bad XMLRPC URL for site: " + site.getXmlRpcUrl());
            return;
        }

        if (!MediaUtils.canReadFile(media.getFilePath())) {
            MediaStore.MediaError error = new MediaError(MediaErrorType.FS_READ_PERMISSION_DENIED);
            notifyMediaProgress(media, 0.f, error);
            return;
        }

        XmlrpcUploadRequestBody requestBody = new XmlrpcUploadRequestBody(media, this, site);
        HttpUrl url = new HttpUrl.Builder()
                .scheme(xmlrpcUrl.getProtocol())
                .host(xmlrpcUrl.getHost())
                .encodedPath(xmlrpcUrl.getPath())
                .username(site.getUsername())
                .password(site.getPassword())
                .build();

        // Use the HTTP Auth Manager to check if we need HTTP Auth for this url
        HTTPAuthModel httpAuthModel = mHTTPAuthManager.getHTTPAuthModel(xmlrpcUrl.toString());
        String authString = null;
        if (httpAuthModel != null) {
            String creds = String.format("%s:%s", httpAuthModel.getUsername(), httpAuthModel.getPassword());
            authString = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
        }

        Builder builder = new okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("User-Agent", mUserAgent.toString());

        if (authString != null) {
            // Add the authorization header
            builder.addHeader("Authorization", authString);
        }
        okhttp3.Request request = builder.build();

        mCurrentUploadCall = mOkHttpClient.newCall(request);
        mCurrentUploadCall.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    AppLog.d(T.MEDIA, "media upload successful: " + media.getTitle());
                    MediaModel responseMedia = getMediaFromUploadResponse(response);
                    if (responseMedia != null) {
                        responseMedia.setLocalSiteId(site.getId());
                        responseMedia.setId(media.getId());
                        notifyMediaUploaded(responseMedia, null);
                    } else {
                        fetchMedia(site, media, true);
                    }
                } else {
                    AppLog.w(T.MEDIA, "error uploading media: " + response.message());
                    MediaError error = new MediaError(fromHttpStatusCode(response.code()));
                    notifyMediaProgress(media, 0.f, error);
                }
                mCurrentUploadCall = null;
            }

            @Override
            public void onFailure(Call call, IOException e) {
                AppLog.w(T.MEDIA, "media upload failed: " + e);
                MediaStore.MediaError error = new MediaError(MediaErrorType.GENERIC_ERROR);
                notifyMediaProgress(media, 0.f, error);
                mCurrentUploadCall = null;
            }
        });
    }

    /**
     * ref: https://codex.wordpress.org/XML-RPC_WordPress_API/Media#wp.getMediaLibrary
     */
    public void fetchAllMedia(final SiteModel site) {
        if (site == null) {
            AppLog.w(T.MEDIA, "No site given with FETCH_ALL_MEDIA request, dispatching error.");
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyAllMediaFetched(null, null, error, null);
            return;
        }

        List<Object> params = getBasicParams(site, null);
        final MediaFilter filter = new MediaFilter();
        filter.number = MediaFilter.MAX_NUMBER;
        filter.offset = mFetchAllOffset;
        params.add(getQueryParams(filter));

        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_LIBRARY, params, new Listener() {
            @Override
            public void onResponse(Object response) {
                List<MediaModel> responseMedia = getMediaListFromXmlrpcResponse(response, site.getId());
                if (responseMedia != null) {
                    mFetchedMedia.addAll(responseMedia);
                    if (responseMedia.size() < MediaFilter.MAX_NUMBER) {
                        AppLog.v(T.MEDIA, "Fetched all media for site via XMLRPC.GET_MEDIA_LIBRARY");
                        notifyAllMediaFetched(site, mFetchedMedia, null, filter);
                        mFetchAllOffset = 0;
                        mFetchedMedia = new ArrayList<>();
                    } else {
                        mFetchAllOffset += MediaFilter.MAX_NUMBER;
                        fetchAllMedia(site);
                    }
                } else {
                    AppLog.w(T.MEDIA, "could not parse XMLRPC.GET_MEDIA_LIBRARY response: " + response);
                    MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                    notifyAllMediaFetched(site, null, error, filter);
                }
            }
        }, new BaseRequest.BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                AppLog.e(T.MEDIA, "XMLRPC.GET_MEDIA_LIBRARY error response:", error.volleyError);
                MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                notifyAllMediaFetched(site, null, mediaError, filter);
            }
        }));
    }

    public void fetchMedia(final SiteModel site, final MediaModel media) {
        fetchMedia(site, media, false);
    }

    /**
     * ref: https://codex.wordpress.org/XML-RPC_WordPress_API/Media#wp.getMediaItem
     */
    public void fetchMedia(final SiteModel site, final MediaModel media, final boolean isFreshUpload) {
        if (site == null || media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyMediaFetched(site, media, error);
            return;
        }

        List<Object> params = getBasicParams(site, media);
        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_ITEM, params, new Listener() {
            @Override
            public void onResponse(Object response) {
                AppLog.v(T.MEDIA, "Fetched media for site via XMLRPC.GET_MEDIA_ITEM");
                MediaModel responseMedia = getMediaFromXmlrpcResponse((HashMap) response);
                if (responseMedia != null) {
                    AppLog.v(T.MEDIA, "Fetched media with ID: " + media.getMediaId());
                    responseMedia.setLocalSiteId(site.getId());
                    if (isFreshUpload) {
                        notifyMediaUploaded(responseMedia, null);
                    } else {
                        notifyMediaFetched(site, responseMedia, null);
                    }
                } else {
                    AppLog.w(T.MEDIA, "could not parse Fetch media response, ID: " + media.getMediaId());
                    MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                    notifyMediaFetched(site, media, error);
                }
            }
        }, new BaseRequest.BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                AppLog.v(T.MEDIA, "XMLRPC.GET_MEDIA_ITEM error response: " + error);
                MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                notifyMediaFetched(site, media, mediaError);
            }
        }));
    }

    public void deleteMedia(final SiteModel site, final MediaModel media) {
        if (site == null || media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            notifyMediaDeleted(site, media, error);
            return;
        }

        List<Object> params = getBasicParams(site, media);
        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_POST, params, new Listener() {
            @Override
            public void onResponse(Object response) {
                // response should be a boolean indicating result of push request
                if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                    AppLog.w(T.MEDIA, "could not parse XMLRPC.DELETE_MEDIA response: " + response);
                    MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                    notifyMediaDeleted(site, media, error);
                    return;
                }

                AppLog.v(T.MEDIA, "Successful response from XMLRPC.DELETE_MEDIA");
                notifyMediaDeleted(site, media, null);
            }
        }, new BaseRequest.BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                AppLog.v(T.MEDIA, "Error response from XMLRPC.DELETE_MEDIA:" + error);
                MediaErrorType mediaError = MediaErrorType.fromBaseNetworkError(error);
                notifyMediaDeleted(site, media, new MediaError(mediaError));
            }
        }));
    }

    public void cancelUpload(final MediaModel media) {
        // cancel in-progress upload if necessary
        if (mCurrentUploadCall != null && mCurrentUploadCall.isExecuted() && !mCurrentUploadCall.isCanceled()) {
            mCurrentUploadCall.cancel();
            mCurrentUploadCall = null;
        }
        // always report without error
        notifyMediaUploadCanceled(media);
    }

    //
    // Helper methods to dispatch media actions
    //

    private void notifyMediaPushed(SiteModel site, MediaModel media, MediaError error) {
        MediaPayload payload = new MediaPayload(site, media, error);
        mDispatcher.dispatch(MediaActionBuilder.newPushedMediaAction(payload));
    }

    private void notifyMediaProgress(MediaModel media, float progress, MediaError error) {
        ProgressPayload payload = new ProgressPayload(media, progress, progress == 1.f, error);
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaUploaded(MediaModel media, MediaError error) {
        ProgressPayload payload = new ProgressPayload(media, 1.f, error == null, error);
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyAllMediaFetched(SiteModel site, List<MediaModel> media, MediaError error, MediaFilter filter) {
        MediaListPayload payload = new MediaListPayload(site, media, error, filter);
        mDispatcher.dispatch(MediaActionBuilder.newFetchedAllMediaAction(payload));
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

    // media list responses should be of type Object[] with each media item in the array represented by a HashMap
    private List<MediaModel> getMediaListFromXmlrpcResponse(Object response, int localSiteId) {
        if (response == null || !(response instanceof Object[])) return null;

        Object[] responseArray = (Object[]) response;
        List<MediaModel> responseMedia = new ArrayList<>();
        for (Object mediaObject : responseArray) {
            if (!(mediaObject instanceof HashMap)) continue;
            MediaModel media = getMediaFromXmlrpcResponse((HashMap) mediaObject);
            if (media != null) {
                media.setLocalSiteId(localSiteId);
                responseMedia.add(media);
            }
        }

        return responseMedia;
    }

    private MediaModel getMediaFromXmlrpcResponse(HashMap response) {
        if (response == null || response.isEmpty()) return null;

        MediaModel media = new MediaModel();
        media.setMediaId(MapUtils.getMapLong(response, "attachment_id"));
        media.setPostId(MapUtils.getMapLong(response, "parent"));
        media.setTitle(MapUtils.getMapStr(response, "title"));
        media.setCaption(MapUtils.getMapStr(response, "caption"));
        media.setDescription(MapUtils.getMapStr(response, "description"));
        media.setVideoPressGuid(MapUtils.getMapStr(response, "videopress_shortcode"));
        media.setThumbnailUrl(MapUtils.getMapStr(response, "thumbnail"));
        media.setUploadDate(MapUtils.getMapDate(response, "date_created_gmt").toString());

        String link = MapUtils.getMapStr(response, "link");
        String fileExtension = MediaUtils.getExtension(link);
        media.setUrl(link);
        media.setFileName(MediaUtils.getFileName(link));
        media.setFileExtension(fileExtension);
        media.setMimeType(MediaUtils.getMimeTypeForExtension(fileExtension));

        Object metadataObject = response.get("metadata");
        if (metadataObject instanceof Map) {
            Map metadataMap = (Map) metadataObject;
            media.setWidth(MapUtils.getMapInt(metadataMap, "width"));
            media.setHeight(MapUtils.getMapInt(metadataMap, "height"));
        }

        media.setUploadState(MediaModel.UploadState.UPLOADED.toString());
        return media;
    }

    private MediaModel getMediaFromUploadResponse(okhttp3.Response response) {
        MediaModel media = new MediaModel();
        try {
            String data = new String(response.body().bytes(), "UTF-8");
            InputStream is = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
            Object obj = XMLSerializerUtils.deserialize(XMLSerializerUtils.scrubXmlResponse(is));
            if (obj instanceof Map) {
                media.setMediaId(MapUtils.getMapLong((Map) obj, "attachment_id"));
            }
        } catch (IOException | XMLRPCException | XmlPullParserException e) {
            AppLog.w(AppLog.T.MEDIA, "Failed to parse XMLRPC.wpUploadFile response: " + response);
            return null;
        }
        return media;
    }

    private Map<String, Object> getEditMediaFields(final MediaModel media) {
        if (media == null) return null;
        Map<String, Object> mediaFields = new HashMap<>();
        mediaFields.put("post_title", media.getTitle());
        mediaFields.put("post_content", media.getDescription());
        mediaFields.put("post_excerpt", media.getCaption());
        return mediaFields;
    }

    private boolean is404Response(BaseRequest.BaseNetworkError error) {
        if (error.isGeneric() && error.type == BaseRequest.GenericErrorType.NOT_FOUND) {
            return true;
        }

        if (error.hasVolleyError() && error.volleyError != null) {
            VolleyError volleyError = error.volleyError;
            if (volleyError.networkResponse != null
                    && volleyError.networkResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return true;
            }

            if (volleyError.getCause() instanceof XMLRPCFault) {
                if (((XMLRPCFault) volleyError.getCause()).getFaultCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    return true;
                }
            }
        }

        return false;
    }

    private Map<String, Object> getQueryParams(final MediaFilter filter) {
        Map<String, Object> queryParams = null;
        if (filter != null) {
            queryParams = new HashMap<>();
            if (filter.number > 0) {
                queryParams.put("number", Math.min(filter.number, MediaFilter.MAX_NUMBER));
            }
            if (filter.offset > 0) {
                queryParams.put("offset", filter.offset);
            }
            if (filter.postId > 0) {
                queryParams.put("parent_id", filter.postId);
            }
            if (!TextUtils.isEmpty(filter.mimeType)) {
                queryParams.put("mime_type", filter.mimeType);
            }
        }
        return queryParams;
    }

    @NonNull
    private List<Object> getBasicParams(final SiteModel site, final MediaModel media) {
        List<Object> params = new ArrayList<>();
        if (site != null) {
            params.add(site.getSelfHostedSiteId());
            params.add(site.getUsername());
            params.add(site.getPassword());
            if (media != null) {
                params.add(media.getMediaId());
            }
        }
        return params;
    }
}
