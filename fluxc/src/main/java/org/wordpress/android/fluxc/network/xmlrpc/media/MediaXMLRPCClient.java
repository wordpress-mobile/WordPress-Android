package org.wordpress.android.fluxc.network.xmlrpc.media;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
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

public class MediaXMLRPCClient extends BaseXMLRPCClient implements ProgressListener {
    private OkHttpClient mOkHttpClient;

    public MediaXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, OkHttpClient okClient,
                             AccessToken accessToken, UserAgent userAgent,
                             HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
        mOkHttpClient = okClient;
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        notifyMediaProgress(media, Math.min(0.99f, progress), null);
    }

    public void pushMedia(final SiteModel site, final List<MediaModel> mediaList) {
        for (final MediaModel media : mediaList) {
            List<Object> params = getBasicParams(site, media);
            params.add(getEditMediaFields(media));
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.EDIT_POST, params, new Listener() {
                @Override
                public void onResponse(Object response) {
                    // response should be a boolean indicating result of push request
                    if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                        AppLog.w(T.MEDIA, "could not parse XMLRPC.EDIT_MEDIA response: " + response);
                        MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                        notifyMediaPushed(MediaAction.PUSH_MEDIA, site, media, error);
                        return;
                    }

                    // success!
                    AppLog.i(T.MEDIA, "Media updated on remote: " + media.getTitle());
                    notifyMediaPushed(MediaAction.PUSH_MEDIA, site, media, null);
                }
            }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    AppLog.e(T.MEDIA, "error response to XMLRPC.EDIT_MEDIA request: " + error);
                    if (is404Response(error)) {
                        AppLog.e(T.MEDIA, "media does not exist, no need to report error");
                        notifyMediaPushed(MediaAction.PUSH_MEDIA, site, media, null);
                    } else {
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        notifyMediaPushed(MediaAction.PUSH_MEDIA, site, media, mediaError);
                    }
                }
            }));
        }
    }

    public void uploadMedia(SiteModel site, MediaModel media) {
        performUpload(site, media);
    }

    public void fetchAllMedia(final SiteModel site, final MediaFilter filter) {
        List<Object> params = getBasicParams(site, null);
        if (filter != null) {
            params.add(getQueryParams(filter));
        }
        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_LIBRARY, params, new Listener() {
            @Override
            public void onResponse(Object response) {
                List<MediaModel> media = getMediaListFromXmlrpcResponse(response, site.getSiteId());
                if (media != null) {
                    AppLog.v(T.MEDIA, "Fetched all media for site via XMLRPC.GET_MEDIA_LIBRARY");
                    notifyMediaFetched(MediaAction.FETCH_ALL_MEDIA, site, media, null);
                } else {
                    AppLog.w(T.MEDIA, "could not parse XMLRPC.GET_MEDIA_LIBRARY response: " + response);
                    MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                    notifyMediaFetched(MediaAction.FETCH_ALL_MEDIA, site, (MediaModel) null, error);
                }
            }
        }, new BaseRequest.BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                AppLog.e(T.MEDIA, "XMLRPC.GET_MEDIA_LIBRARY error response:", error.volleyError);
                MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                notifyMediaFetched(MediaAction.FETCH_ALL_MEDIA, site, (MediaModel) null, mediaError);
            }
        }));
    }

    public void fetchMedia(final SiteModel site, List<MediaModel> mediaToFetch) {
        if (site == null || mediaToFetch == null || mediaToFetch.isEmpty()) return;

        for (final MediaModel media : mediaToFetch) {
            List<Object> params = getBasicParams(site, media);
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_ITEM, params, new Listener() {
                @Override
                public void onResponse(Object response) {
                    AppLog.v(T.MEDIA, "Fetched media for site via XMLRPC.GET_MEDIA_ITEM");
                    MediaModel responseMedia = getMediaFromXmlrpcResponse((HashMap) response);
                    if (responseMedia != null) {
                        AppLog.v(T.MEDIA, "Fetched media with ID: " + media.getMediaId());
                        responseMedia.setSiteId(site.getSiteId());
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
                    AppLog.v(T.MEDIA, "XMLRPC.GET_MEDIA_ITEM error response: " + error);
                    MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                    notifyMediaFetched(MediaAction.FETCH_MEDIA, site, media, mediaError);
                }
            }));
        }
    }

    public void deleteMedia(final SiteModel site, final List<MediaModel> mediaToDelete) {
        if (site == null || mediaToDelete == null || mediaToDelete.isEmpty()) return;

        for (final MediaModel media : mediaToDelete) {
            List<Object> params = getBasicParams(site, media);
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_POST, params, new Listener() {
                @Override
                public void onResponse(Object response) {
                    // response should be a boolean indicating result of push request
                    if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                        AppLog.w(T.MEDIA, "could not parse XMLRPC.DELETE_MEDIA response: " + response);
                        MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                        notifyMediaDeleted(MediaAction.DELETE_MEDIA, site, media, error);
                        return;
                    }

                    AppLog.v(T.MEDIA, "Successful response from XMLRPC.DELETE_MEDIA");
                }
            }, new BaseRequest.BaseErrorListener() {
                @Override
                public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                    AppLog.v(T.MEDIA, "Error response from XMLRPC.DELETE_MEDIA:" + error);
                    MediaErrorType mediaError = MediaErrorType.fromBaseNetworkError(error);
                    notifyMediaDeleted(MediaAction.DELETE_MEDIA, site, media, new MediaError(mediaError));
                }
            }));
        }
    }

    private void performUpload(SiteModel site, final MediaModel media) {
        URL xmlrpcUrl;
        try {
            xmlrpcUrl = new URL(site.getXmlRpcUrl());
        } catch (MalformedURLException e) {
            AppLog.w(T.MEDIA, "bad XMLRPC URL for site: " + site.getXmlRpcUrl());
            return;
        }

        if (!MediaUtils.canReadFile(media.getFilePath())) {
            MediaStore.MediaError error = new MediaError(MediaErrorType.FS_READ_PERMISSION_DENIED);
            notifyMediaUploaded(media, error);
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

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    AppLog.d(T.MEDIA, "media upload successful: " + media.getTitle());
                    MediaModel responseMedia = getMediaFromUploadResponse(response);
                    notifyMediaUploaded(responseMedia, null);
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

    private void notifyMediaProgress(MediaModel media, float progress, MediaError error) {
        AppLog.v(AppLog.T.MEDIA, "Progress update on upload of " + media.getFilePath() + ": " + progress);
        MediaStore.ProgressPayload payload = new MediaStore.ProgressPayload(media, progress, false);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaFetched(MediaAction cause, SiteModel site, MediaModel media, MediaError error) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        notifyMediaFetched(cause, site, mediaList, error);
    }

    private void notifyMediaFetched(MediaAction cause, SiteModel site, List<MediaModel> mediaList, MediaError error) {
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(cause, site, mediaList);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaAction(payload));
    }

    private void notifyMediaPushed(MediaAction cause, SiteModel site, MediaModel media, MediaError error) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        notifyMediaPushed(cause, site, mediaList, error);
    }

    private void notifyMediaPushed(MediaAction cause, SiteModel site, List<MediaModel> mediaList, MediaError error) {
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(cause, site, mediaList);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newPushedMediaAction(payload));
    }

    private void notifyMediaUploaded(MediaModel media, MediaError error) {
        AppLog.v(AppLog.T.MEDIA, "Notify media uploaded: " + media.getFilePath());
        MediaStore.ProgressPayload payload = new MediaStore.ProgressPayload(media, 1.f, error == null);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaDeleted(MediaAction cause, SiteModel site, MediaModel media, MediaError error) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(cause, site, mediaList);
        payload.error = error;
        mDispatcher.dispatch(MediaActionBuilder.newDeletedMediaAction(payload));
    }

    //
    // Utility methods
    //

    // WordPress XML-RPC response keys
    private static final String MEDIA_ID_KEY         = "attachment_id";
    private static final String POST_ID_KEY          = "parent";
    private static final String TITLE_KEY            = "title";
    private static final String CAPTION_KEY          = "caption";
    private static final String DESCRIPTION_KEY      = "description";
    private static final String VIDEOPRESS_GUID_KEY  = "videopress_shortcode";
    private static final String THUMBNAIL_URL_KEY    = "thumbnail";
    private static final String DATE_UPLOADED_KEY    = "date_created_gmt";
    private static final String LINK_KEY             = "link";
    private static final String METADATA_KEY         = "metadata";
    private static final String WIDTH_KEY            = "width";
    private static final String HEIGHT_KEY           = "height";

    // filter query parameter keys for XMLRPC.GET_MEDIA_LIBRARY
    private static final String NUMBER_FILTER_KEY    = "number";
    private static final String OFFSET_FILTER_KEY    = "offset";
    private static final String PARENT_FILTER_KEY    = "parent_id";
    private static final String MIME_TYPE_FILTER_KEY = "mime_type";

    // keys for pushing changes to existing remote media
    public static final String TITLE_EDIT_KEY       = "post_title";
    public static final String DESCRIPTION_EDIT_KEY = "post_content";
    public static final String CAPTION_EDIT_KEY     = "post_excerpt";

    // media list responses should be of type Object[] with each media item in the array represented by a HashMap
    private List<MediaModel> getMediaListFromXmlrpcResponse(Object response, long siteId) {
        if (response == null || !(response instanceof Object[])) return null;

        Object[] responseArray = (Object[]) response;
        List<MediaModel> responseMedia = new ArrayList<>();
        for (Object mediaObject : responseArray) {
            if (!(mediaObject instanceof HashMap)) continue;
            MediaModel media = getMediaFromXmlrpcResponse((HashMap) mediaObject);
            if (media != null) {
                media.setSiteId(siteId);
                responseMedia.add(media);
            }
        }

        return responseMedia;
    }

    private MediaModel getMediaFromXmlrpcResponse(HashMap response) {
        if (response == null || response.isEmpty()) return null;

        MediaModel media = new MediaModel();
        media.setMediaId(MapUtils.getMapLong(response, MEDIA_ID_KEY));
        media.setPostId(MapUtils.getMapLong(response, POST_ID_KEY));
        media.setTitle(MapUtils.getMapStr(response, TITLE_KEY));
        media.setCaption(MapUtils.getMapStr(response, CAPTION_KEY));
        media.setDescription(MapUtils.getMapStr(response, DESCRIPTION_KEY));
        media.setVideoPressGuid(MapUtils.getMapStr(response, VIDEOPRESS_GUID_KEY));
        media.setThumbnailUrl(MapUtils.getMapStr(response, THUMBNAIL_URL_KEY));
        media.setUploadDate(MapUtils.getMapDate(response, DATE_UPLOADED_KEY).toString());

        String link = MapUtils.getMapStr(response, LINK_KEY);
        String fileExtension = MediaUtils.getExtension(link);
        media.setUrl(link);
        media.setFileName(MediaUtils.getFileName(link));
        media.setFileExtension(fileExtension);
        media.setMimeType(MediaUtils.getMimeTypeForExtension(fileExtension));

        Object metadataObject = response.get(METADATA_KEY);
        if (metadataObject instanceof Map) {
            Map metadataMap = (Map) metadataObject;
            media.setWidth(MapUtils.getMapInt(metadataMap, WIDTH_KEY));
            media.setHeight(MapUtils.getMapInt(metadataMap, HEIGHT_KEY));
        }

        return media;
    }

    private MediaModel getMediaFromUploadResponse(okhttp3.Response response) {
        MediaModel media = new MediaModel();
        try {
            String data = new String(response.body().bytes(), "UTF-8");
            InputStream is = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
            Object obj = XMLSerializerUtils.deserialize(XMLSerializerUtils.scrubXmlResponse(is));
            if (obj instanceof Map) {
                media.setMediaId(MapUtils.getMapLong((Map) obj, MEDIA_ID_KEY));
            }
        } catch (IOException | XMLRPCException | XmlPullParserException e) {
            AppLog.w(AppLog.T.MEDIA, "failed to parse XMLRPC.wpUploadFile response: " + response);
            return null;
        }
        return media;
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

    private Map<String, Object> getEditMediaFields(final MediaModel media) {
        if (media == null) return null;
        Map<String, Object> mediaFields = new HashMap<>();
        mediaFields.put(TITLE_EDIT_KEY, media.getTitle());
        mediaFields.put(DESCRIPTION_EDIT_KEY, media.getDescription());
        mediaFields.put(CAPTION_EDIT_KEY, media.getCaption());
        return mediaFields;
    }

    private Map<String, Object> getQueryParams(final MediaFilter filter) {
        if (filter == null) return null;

        Map<String, Object> queryParams = new HashMap<>();
        if (filter.number > 0) {
            queryParams.put(NUMBER_FILTER_KEY, Math.min(filter.number, MediaFilter.MAX_NUMBER));
        }
        if (filter.offset > 0) {
            queryParams.put(OFFSET_FILTER_KEY, filter.offset);
        }
        if (filter.postId > 0) {
            queryParams.put(PARENT_FILTER_KEY, filter.postId);
        }
        if (!TextUtils.isEmpty(filter.mimeType)) {
            queryParams.put(MIME_TYPE_FILTER_KEY, filter.mimeType);
        }
        return queryParams;
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
}
