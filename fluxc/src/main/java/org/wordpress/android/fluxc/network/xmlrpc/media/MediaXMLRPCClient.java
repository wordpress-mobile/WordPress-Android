package org.wordpress.android.fluxc.network.xmlrpc.media;

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.HTTPAuthModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCException;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCFault;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.XMLSerializerUtils;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.MimeType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
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
import java.util.Arrays;
import java.util.Date;
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
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
public class MediaXMLRPCClient extends BaseXMLRPCClient implements ProgressListener {
    private static final String[] REQUIRED_UPLOAD_RESPONSE_FIELDS = {
            "attachment_id", "parent", "title", "caption", "description", "thumbnail", "date_created_gmt", "link"};

    private OkHttpClient mOkHttpClient;
    // this will hold which media is being uploaded by which call, in order to be able
    // to monitor multiple uploads
    private ConcurrentHashMap<Integer, Call> mCurrentUploadCalls = new ConcurrentHashMap<>();

    @Inject
    public MediaXMLRPCClient(Dispatcher dispatcher,
                             @Named("custom-ssl") RequestQueue requestQueue,
                             @Named("custom-ssl") OkHttpClient okHttpClient,
                             UserAgent userAgent,
                             HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, userAgent, httpAuthManager);
        mOkHttpClient = okHttpClient;
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        if (mCurrentUploadCalls.containsKey(media.getId())) {
            notifyMediaProgress(media, Math.min(progress, 0.99f));
        }
    }

    public void pushMedia(final SiteModel site, final MediaModel media) {
        List<Object> params = getBasicParams(site, media);
        params.add(getEditMediaFields(media));
        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.EDIT_POST, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        // response should be a boolean indicating result of push request
                        if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                            String message = "could not parse XMLRPC.EDIT_MEDIA response: " + response;
                            AppLog.w(T.MEDIA, message);
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            error.logMessage = message;
                            notifyMediaPushed(site, media, error);
                            return;
                        }

                        // success!
                        AppLog.i(T.MEDIA, "Media updated on remote: " + media.getTitle());
                        notifyMediaPushed(site, media, null);
                    }
                }, new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        String errorMessage = "error response to XMLRPC.EDIT_MEDIA request: " + error;
                        AppLog.e(T.MEDIA, errorMessage);
                        if (is404Response(error)) {
                            AppLog.e(T.MEDIA, "media does not exist, no need to report error");
                            notifyMediaPushed(site, media, null);
                        } else {
                            MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                            mediaError.message = error.message;
                            mediaError.logMessage = errorMessage;
                            notifyMediaPushed(site, media, mediaError);
                        }
                    }
                }
        ));
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

        if (media == null || media.getId() == 0) {
            // we can't have a MediaModel without an ID - otherwise we can't keep track of them.
            MediaError error = new MediaError(MediaErrorType.INVALID_ID);
            if (media == null) {
                error.logMessage = "XMLRPC: media is null on upload";
            } else {
                error.logMessage = "XMLRPC: media ID is 0 on upload";
            }
            notifyMediaUploaded(media, error);
            return;
        }

        if (!MediaUtils.canReadFile(media.getFilePath())) {
            MediaError error = new MediaError(MediaErrorType.FS_READ_PERMISSION_DENIED);
            error.logMessage = "XMLRPC: cannot read file on upload";
            notifyMediaUploaded(media, error);
            return;
        }

        XmlrpcUploadRequestBody requestBody = new XmlrpcUploadRequestBody(media, this, site);
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme(xmlrpcUrl.getProtocol())
                .host(xmlrpcUrl.getHost())
                .encodedPath(xmlrpcUrl.getPath())
                .username(site.getUsername())
                .password(site.getPassword());
        if (xmlrpcUrl.getPort() > 0) {
            urlBuilder.port(xmlrpcUrl.getPort());
        }
        HttpUrl url = urlBuilder.build();

        // Use the HTTP Auth Manager to check if we need HTTP Auth for this url
        HTTPAuthModel httpAuthModel = mHTTPAuthManager.getHTTPAuthModel(xmlrpcUrl.toString());
        String authString = null;
        if (httpAuthModel != null) {
            String creds = String.format("%s:%s", httpAuthModel.getUsername(), httpAuthModel.getPassword());
            authString = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
        }

        Builder builder = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("User-Agent", mUserAgent.toString());

        if (authString != null) {
            // Add the authorization header
            builder.addHeader("Authorization", authString);
        }
        Request request = builder.build();

        Call call = mOkHttpClient.newCall(request);
        mCurrentUploadCalls.put(media.getId(), call);

        AppLog.d(T.MEDIA, "starting upload for: " + media.getId());
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    // HTTP_OK code doesn't mean the upload is successful, XML-RPC API returns code 200 with an
                    // xml field "faultCode" on error.
                    try {
                        Map responseMap = getMapFromUploadResponse(response);
                        if (responseMap != null) {
                            AppLog.d(T.MEDIA, "media upload successful, local id=" + media.getId());
                            if (isDeprecatedUploadResponse(responseMap)) {
                                media.setMediaId(MapUtils.getMapLong(responseMap, "id"));
                                // Upload media response only has `type, id, file, url` fields whereas we need
                                // `parent, title, caption, description, videopress_shortcode, thumbnail,
                                // date_created_gmt, link, width, height` fields, so we need to make a fetch for them
                                // This only applies to WordPress sites running versions older than WordPress 4.4
                                fetchMedia(site, media, true);
                            } else {
                                MediaModel responseMedia = getMediaFromXmlrpcResponse(responseMap);
                                // Retain local IDs
                                responseMedia.setId(media.getId());
                                responseMedia.setLocalSiteId(site.getId());
                                responseMedia.setLocalPostId(media.getLocalPostId());
                                responseMedia.setMarkedLocallyAsFeatured(media.getMarkedLocallyAsFeatured());

                                notifyMediaUploaded(responseMedia, null);
                            }
                        } else {
                            String message = "error uploading media - malformed response: " + response.message();
                            AppLog.w(T.MEDIA, message);
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR, response.message());
                            error.logMessage = "XMLRPC: " + message;
                            notifyMediaUploaded(media, error);
                        }
                    } catch (XMLRPCException fault) {
                        MediaError mediaError = getMediaErrorFromXMLRPCException(fault);
                        String message = "media upload failed with error: " + mediaError.message;
                        AppLog.w(T.MEDIA, message);
                        mediaError.logMessage = "XMLRPC: " + message;
                        notifyMediaUploaded(media, mediaError);
                    }
                } else {
                    AppLog.e(T.MEDIA, "error uploading media: " + response.message());
                    MediaError error = new MediaError(MediaErrorType.fromHttpStatusCode(response.code()));
                    error.message = response.message();
                    error.logMessage = "XMLRPC: error uploading media";
                    error.statusCode = response.code();
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
                error.logMessage = "XMLRPC: " + e.getMessage();
                notifyMediaUploaded(media, error);
            }
        });
    }

    /**
     * ref: https://codex.wordpress.org/XML-RPC_WordPress_API/Media#wp.getMediaLibrary
     */
    public void fetchMediaList(final SiteModel site, final int number, final int offset, final MimeType.Type mimeType) {
        List<Object> params = getBasicParams(site, null);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("number", number);
        if (offset > 0) {
            queryParams.put("offset", offset);
        }
        if (mimeType != null) {
            queryParams.put("mime_type", mimeType.getValue());
        }
        params.add(queryParams);

        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_LIBRARY, params,
                new Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] response) {
                        List<MediaModel> mediaList = getMediaListFromXmlrpcResponse(response, site.getId());
                        if (mediaList != null) {
                            AppLog.v(T.MEDIA, "Fetched media list for site via XMLRPC.GET_MEDIA_LIBRARY");
                            boolean canLoadMore = mediaList.size() == number;
                            notifyMediaListFetched(site, mediaList, offset > 0, canLoadMore, mimeType);
                        } else {
                            String message = "could not parse XMLRPC.GET_MEDIA_LIBRARY response: "
                                             + Arrays.toString(response);
                            AppLog.w(T.MEDIA, message);
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            error.logMessage = "XMLRPC: " + message;
                            notifyMediaListFetched(site, error, mimeType);
                        }
                    }
                }, new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        String message = "XMLRPC.GET_MEDIA_LIBRARY error response:";
                        AppLog.e(T.MEDIA, message, error.volleyError);
                        MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                        mediaError.logMessage = "XMLRPC: " + message;
                        notifyMediaListFetched(site, mediaError, mimeType);
                    }
                }
        ));
    }

    public void fetchMedia(final SiteModel site, final MediaModel media) {
        fetchMedia(site, media, false);
    }

    /**
     * ref: https://codex.wordpress.org/XML-RPC_WordPress_API/Media#wp.getMediaItem
     */
    private void fetchMedia(final SiteModel site, final MediaModel media, final boolean isFreshUpload) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            error.logMessage = "XMLRPC: empty media on fetchMedia";
            if (isFreshUpload) {
                notifyMediaUploaded(null, error);
            } else {
                notifyMediaFetched(site, null, error);
            }
            return;
        }

        List<Object> params = getBasicParams(site, media);
        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_ITEM, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        AppLog.v(T.MEDIA, "Fetched media for site via XMLRPC.GET_MEDIA_ITEM");
                        MediaModel responseMedia = getMediaFromXmlrpcResponse((HashMap) response);
                        if (responseMedia != null) {
                            AppLog.v(T.MEDIA, "Fetched media with remoteId= " + media.getMediaId()
                                              + " localId=" + media.getId());
                            // Retain local IDs
                            responseMedia.setId(media.getId());
                            responseMedia.setLocalSiteId(site.getId());
                            responseMedia.setLocalPostId(media.getLocalPostId());
                            responseMedia.setMarkedLocallyAsFeatured(media.getMarkedLocallyAsFeatured());

                            if (isFreshUpload) {
                                notifyMediaUploaded(responseMedia, null);
                            } else {
                                notifyMediaFetched(site, responseMedia, null);
                            }
                        } else {
                            String message = "could not parse Fetch media response, ID: " + media.getMediaId();
                            AppLog.w(T.MEDIA, message);
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            error.logMessage = "XMLRPC: " + message;
                            if (isFreshUpload) {
                                notifyMediaUploaded(media, error);
                            } else {
                                notifyMediaFetched(site, media, error);
                            }
                        }
                    }
                }, new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        String message = "XMLRPC.GET_MEDIA_ITEM error response: " + error;
                        AppLog.e(T.MEDIA, message);
                        if (isFreshUpload) {
                            // we tried to fetch a media that's just uploaded but failed, so we should return
                            // an upload error and not a fetch error as initially parsing the upload response failed
                            MediaError mediaError = new MediaError(MediaErrorType.PARSE_ERROR);
                            mediaError.logMessage = "XMLRPC: " + message;
                            notifyMediaUploaded(media, mediaError);
                        } else {
                            MediaError mediaError = new MediaError(MediaErrorType.fromBaseNetworkError(error));
                            mediaError.logMessage = "XMLRPC: " + message;
                            notifyMediaFetched(site, media, mediaError);
                        }
                    }
                }
            ));
    }

    public void deleteMedia(final SiteModel site, final MediaModel media) {
        if (media == null) {
            // caller may be expecting a notification
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            error.logMessage = "XMLRPC: empty media on delete";
            notifyMediaDeleted(site, null, error);
            return;
        }

        List<Object> params = getBasicParams(site, media);
        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_POST, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        // response should be a boolean indicating result of push request
                        if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                            String message = "could not parse XMLRPC.DELETE_MEDIA response: " + response;
                            AppLog.w(T.MEDIA, message);
                            MediaError error = new MediaError(MediaErrorType.PARSE_ERROR);
                            error.logMessage = "XMLRPC: " + message;
                            notifyMediaDeleted(site, media, error);
                            return;
                        }

                        AppLog.v(T.MEDIA, "Successful response from XMLRPC.DELETE_MEDIA");
                        notifyMediaDeleted(site, media, null);
                    }
                }, new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        String message = "Error response from XMLRPC.DELETE_MEDIA:" + error;
                        AppLog.e(T.MEDIA, message);
                        MediaErrorType mediaErrorType = MediaErrorType.fromBaseNetworkError(error);
                        MediaError mediaError = new MediaError(mediaErrorType);
                        mediaError.logMessage = "XMLRPC: " + message;
                        notifyMediaDeleted(site, media, mediaError);
                    }
                }
            ));
    }

    public void cancelUpload(final MediaModel media) {
        if (media == null) {
            MediaError error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
            error.logMessage = "XMLRPC: empty media on cancel upload";
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
        AppLog.d(T.MEDIA, "mediaXMLRPCClient: removed id: " + id + " from current uploads, remaining: "
                + mCurrentUploadCalls.size());
    }

    //
    // Helper methods to dispatch media actions
    //

    private void notifyMediaPushed(SiteModel site, MediaModel media, MediaError error) {
        MediaPayload payload = new MediaPayload(site, media, error);
        mDispatcher.dispatch(MediaActionBuilder.newPushedMediaAction(payload));
    }

    private void notifyMediaProgress(MediaModel media, float progress) {
        ProgressPayload payload = new ProgressPayload(media, progress, false, null);
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

    // media list responses should be of type Object[] with each media item in the array represented by a HashMap
    private List<MediaModel> getMediaListFromXmlrpcResponse(Object[] response, int localSiteId) {
        if (response == null) return null;

        List<MediaModel> responseMedia = new ArrayList<>();
        for (Object mediaObject : response) {
            if (!(mediaObject instanceof HashMap)) continue;
            MediaModel media = getMediaFromXmlrpcResponse((HashMap) mediaObject);
            if (media != null) {
                media.setLocalSiteId(localSiteId);
                responseMedia.add(media);
            }
        }

        return responseMedia;
    }

    private MediaModel getMediaFromXmlrpcResponse(Map response) {
        if (response == null || response.isEmpty()) return null;

        MediaModel media = new MediaModel();
        media.setMediaId(MapUtils.getMapLong(response, "attachment_id"));
        media.setPostId(MapUtils.getMapLong(response, "parent"));
        media.setTitle(StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(response, "title")));
        media.setCaption(StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(response, "caption")));
        media.setDescription(StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(response, "description")));
        media.setVideoPressGuid(MapUtils.getMapStr(response, "videopress_shortcode"));
        media.setThumbnailUrl(MapUtils.getMapStr(response, "thumbnail"));
        Date uploadDate = MapUtils.getMapDate(response, "date_created_gmt");
        media.setUploadDate(DateTimeUtils.iso8601UTCFromDate(uploadDate));
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
            media.setFileUrlMediumSize(getFileUrlForSize(link, metadataMap, "medium"));
            media.setFileUrlMediumLargeSize(getFileUrlForSize(link, metadataMap, "medium_large"));
            media.setFileUrlLargeSize(getFileUrlForSize(link, metadataMap, "large"));
        }

        media.setUploadState(MediaUploadState.UPLOADED);
        return media;
    }

    private String getFileUrlForSize(String mediaUrl, Map metadataMap, String size) {
        if (metadataMap == null || TextUtils.isEmpty(mediaUrl) || !mediaUrl.contains("/")) {
            return null;
        }

        String fileName = getFileForSize(metadataMap, size);
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }

        // make sure the path to the original image is a valid path to a file
        if (mediaUrl.lastIndexOf("/") + 1 >= mediaUrl.length()) return null;

        String baseURL = mediaUrl.substring(0, mediaUrl.lastIndexOf("/") + 1);
        return baseURL + fileName;
    }

    private String getFileForSize(Map metadataMap, String size) {
        if (metadataMap == null) {
            return null;
        }
        Object sizesObject = metadataMap.get("sizes");
        if (sizesObject instanceof Map) {
            Map sizesMap = (Map) sizesObject;
            Object requestedSizeObject = sizesMap.get(size);
            if (requestedSizeObject instanceof Map) {
                Map requestedSizeMap = (Map) requestedSizeObject;
                return MapUtils.getMapStr(requestedSizeMap, "file");
            }
        }
        return null;
    }

    private MediaError getMediaErrorFromXMLRPCException(XMLRPCException exception) {
        MediaError mediaError = new MediaError(MediaErrorType.GENERIC_ERROR);
        mediaError.message = exception.getLocalizedMessage();
        mediaError.logMessage = exception.getMessage();
        if (exception instanceof XMLRPCFault) {
            switch (((XMLRPCFault) exception).getFaultCode()) {
                case 401:
                    mediaError.type = MediaErrorType.XMLRPC_OPERATION_NOT_ALLOWED;
                    break;
                case 403:
                    mediaError.type = MediaErrorType.NOT_AUTHENTICATED;
                    break;
                case 404:
                    mediaError.type = MediaErrorType.NOT_FOUND;
                    break;
                case 500:
                    mediaError.type = MediaErrorType.XMLRPC_UPLOAD_ERROR;
                    break;
            }
        }
        return mediaError;
    }

    private static Map getMapFromUploadResponse(Response response) throws XMLRPCException {
        try {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                AppLog.e(T.MEDIA, "Failed to parse XMLRPC.wpUploadFile response - body was empty: " + response);
                return null;
            }
            String data = new String(responseBody.bytes(), "UTF-8");
            InputStream is = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
            Object responseObject = XMLSerializerUtils.deserialize(XMLSerializerUtils.scrubXmlResponse(is));
            if (responseObject instanceof Map) {
                return (Map) responseObject;
            }
        } catch (IOException | XmlPullParserException e) {
            AppLog.e(T.MEDIA, "Failed to parse XMLRPC.wpUploadFile response: " + response);
            return null;
        }
        return null;
    }

    private static boolean isDeprecatedUploadResponse(Map responseMap) {
        for (String requiredResponseField : REQUIRED_UPLOAD_RESPONSE_FIELDS) {
            if (!responseMap.containsKey(requiredResponseField)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> getEditMediaFields(final MediaModel media) {
        if (media == null) return null;
        Map<String, Object> mediaFields = new HashMap<>();
        mediaFields.put("post_title", media.getTitle());
        mediaFields.put("post_content", media.getDescription());
        mediaFields.put("post_excerpt", media.getCaption());
        return mediaFields;
    }

    private boolean is404Response(BaseNetworkError error) {
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
