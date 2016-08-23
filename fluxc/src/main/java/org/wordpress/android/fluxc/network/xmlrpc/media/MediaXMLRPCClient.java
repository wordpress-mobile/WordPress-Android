package org.wordpress.android.fluxc.network.xmlrpc.media;

import android.webkit.MimeTypeMap;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MediaNetworkListener;
import org.wordpress.android.fluxc.network.MediaNetworkListener.MediaNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCException;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.XMLSerializerUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;
import org.xmlpull.v1.XmlPullParserException;

import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;

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

public class MediaXMLRPCClient extends BaseXMLRPCClient implements ProgressListener {
    // keys for de-serializing remote responses
    public static final String MEDIA_ID_KEY         = "attachment_id";
    public static final String POST_ID_KEY          = "parent";
    public static final String TITLE_KEY            = "title";
    public static final String CAPTION_KEY          = "caption";
    public static final String DESCRIPTION_KEY      = "description";
    public static final String VIDEOPRESS_GUID_KEY  = "videopress_shortcode";
    public static final String THUMBNAIL_URL_KEY    = "thumbnail";
    public static final String DATE_UPLOADED_KEY    = "date_created_gmt";
    public static final String LINK_KEY             = "link";
    public static final String METADATA_KEY         = "metadata";
    public static final String WIDTH_KEY            = "width";
    public static final String HEIGHT_KEY           = "height";

    // keys for pushing changes to existing remote media
    public static final String TITLE_EDIT_KEY       = "post_title";
    public static final String DESCRIPTION_EDIT_KEY = "post_content";
    public static final String CAPTION_EDIT_KEY     = "post_excerpt";

    private static final String FILE_NAME_REGEX = "^.*/([A-Za-z0-9_-]+)\\.\\w+$";

    private MediaNetworkListener mListener;
    private OkHttpClient mOkHttpClient;

    public MediaXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, OkHttpClient okClient,
                             AccessToken accessToken, UserAgent userAgent,
                             HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
        mOkHttpClient = okClient;
    }

    @Override
    public void onProgress(MediaModel media, float progress) {
        notifyMediaProgress(media, progress);
    }

    public void pushMedia(final SiteModel site, List<MediaModel> mediaList) {
        for (final MediaModel media : mediaList) {
            List<Object> params = getBasicParams(site);
            params.add(media.getMediaId());
            Map<String, Object> mediaFields = new HashMap<>();
            mediaFields.put(TITLE_EDIT_KEY, media.getTitle());
            mediaFields.put(DESCRIPTION_EDIT_KEY, media.getDescription());
            mediaFields.put(CAPTION_EDIT_KEY, media.getCaption());
            params.add(mediaFields);
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.EDIT_MEDIA, params, new Listener() {
                    @Override public void onResponse(Object response) {
                        // response should be a boolean indicating result of push request
                        if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                            String msg = "Unknown response to XMLRPC.EDIT_MEDIA: " + response;
                            AppLog.w(T.MEDIA, msg);
                            notifyMediaError(MediaAction.PUSH_MEDIA, media, MediaNetworkError.UNKNOWN, new Exception(msg));
                            return;
                        }

                        // success!
                        AppLog.i(T.MEDIA, "Media updated on remote: " + media.getTitle());
                        notifyMediaPushed(MediaAction.PUSH_MEDIA, media);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String msg = "Error response from XMLRPC.EDIT_MEDIA: ";
                        if (error != null && error.networkResponse != null && error.networkResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            msg += "media does not exist";
                        } else {
                            msg += "unhandled XMLRPC.EDIT_MEDIA response: " + error;
                        }
                        AppLog.e(T.MEDIA, msg);
                        notifyMediaError(MediaAction.PUSH_MEDIA, media, MediaNetworkError.UNKNOWN, new Exception(msg));
                    }
                }
            ));
        }
    }

    public void uploadMedia(SiteModel site, MediaModel media) {
        performUpload(site, media);
    }

    public void pullAllMedia(SiteModel site) {
        List<Object> params = getBasicParams(site);
        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_LIBRARY, params, new Listener() {
                @Override public void onResponse(Object response) {
                    AppLog.v(T.MEDIA, "Successful response from XMLRPC.GET_MEDIA_LIBRARY");
                    List<MediaModel> media = allMediaResponseToMediaModelList(response);
                    notifyMediaPulled(MediaAction.PULL_ALL_MEDIA, media);
                }
            }, new ErrorListener() {
                @Override public void onErrorResponse(VolleyError error) {
                    AppLog.e(T.MEDIA, "Volley error", error);
                    notifyMediaError(MediaAction.PULL_ALL_MEDIA, null, MediaNetworkError.UNKNOWN, error);
                }
            }
        ));
    }

    public void pullMedia(SiteModel site, List<Long> mediaIds) {
        if (site == null || mediaIds == null || mediaIds.isEmpty()) return;

        for (Long mediaId : mediaIds) {
            List<Object> params = getBasicParams(site);
            params.add(mediaId);
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_ITEM, params, new Listener() {
                    @Override public void onResponse(Object response) {
                        AppLog.v(T.MEDIA, "Successful response from XMLRPC.GET_MEDIA_ITEM");
                        MediaModel media = responseMapToMediaModel((HashMap) response);
                        notifyMediaPulled(MediaAction.PULL_MEDIA, media);
                    }
                }, new ErrorListener() {
                    @Override public void onErrorResponse(VolleyError error) {
                        String msg = "Error response from XMLRPC.GET_MEDIA_ITEM: " + error;
                        AppLog.v(T.MEDIA, msg);
                        if (msg.contains("404")) {
                            notifyMediaError(MediaAction.PULL_MEDIA, null, MediaNetworkError.MEDIA_NOT_FOUND, error);
                        } else if (error.networkResponse != null && error.networkResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            notifyMediaError(MediaAction.PULL_MEDIA, null, MediaNetworkError.MEDIA_NOT_FOUND, error);
                        } else {
                            notifyMediaError(MediaAction.PULL_MEDIA, null, MediaNetworkError.UNKNOWN, error);
                        }
                    }
                }
            ));
        }
    }

    public void deleteMedia(SiteModel site, List<MediaModel> media) {
        if (site == null || media == null || media.isEmpty()) return;

        for (final MediaModel mediaItem : media) {
            List<Object> params = getBasicParams(site);
            params.add(mediaItem.getMediaId());
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_MEDIA, params, new Listener() {
                    @Override public void onResponse(Object response) {
                        // response should be a boolean indicating result of push request
                        if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                            String msg = "Unknown response to XMLRPC.DELETE_MEDIA: " + response;
                            AppLog.w(T.MEDIA, msg);
                            notifyMediaError(MediaAction.PUSH_MEDIA, mediaItem, MediaNetworkError.UNKNOWN, new Exception(msg));
                            return;
                        }

                        AppLog.v(T.MEDIA, "Successful response from XMLRPC.DELETE_MEDIA");
                        List<MediaModel> media = new ArrayList<>(1);
                        media.add(mediaItem);
                        if (mListener != null) {
                            mListener.onMediaDeleted(MediaAction.DELETE_MEDIA, media);
                        }
                    }
                }, new ErrorListener() {
                    @Override public void onErrorResponse(VolleyError error) {
                        String msg = "Error response from XMLRPC.DELETE_MEDIA: " + error;
                        AppLog.v(T.MEDIA, msg);
                        if (msg.contains("404")) {
                            notifyMediaError(MediaAction.DELETE_MEDIA, null, MediaNetworkError.MEDIA_NOT_FOUND, error);
                        }
                    }
                }
            ));
        }
    }

    public void setListener(MediaNetworkListener listener) {
        mListener = listener;
    }

    private void performUpload(SiteModel site, final MediaModel media) {
        URL xmlrpcUrl;
        try {
            xmlrpcUrl = new URL(site.getXmlRpcUrl());
        } catch (MalformedURLException e) {
            AppLog.w(T.MEDIA, "bad XMLRPC URL for site: " + site.getXmlRpcUrl());
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
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    AppLog.d(T.MEDIA, "media upload successful: " + media.getTitle());
                    List<MediaModel> resultList = new ArrayList<>();
                    resultList.add(responseXmlToMediaModel(response));
                    notifyMediaPushed(MediaAction.UPLOAD_MEDIA, resultList);
                } else {
                    AppLog.w(T.MEDIA, "error uploading media: " + response);
                    notifyMediaError(MediaAction.UPLOAD_MEDIA, media, MediaNetworkError.UNKNOWN, null);
                }
            }

            @Override public void onFailure(Call call, IOException e) {
                AppLog.w(T.MEDIA, "media upload failed: " + e);
                notifyMediaError(MediaAction.UPLOAD_MEDIA, media, MediaNetworkError.UNKNOWN, e);
            }
        });
    }

    private List<Object> getBasicParams(SiteModel site) {
        List<Object> params = new ArrayList<>();
        params.add(site.getDotOrgSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        return params;
    }

    private List<MediaModel> allMediaResponseToMediaModelList(Object response) {
        if (!(response instanceof Object[])) {
            return null;
        }

        Object[] responseArray = (Object[]) response;
        List<MediaModel> responseMedia = new ArrayList<>();

        for (Object mediaObject : responseArray) {
            if (!(mediaObject instanceof HashMap)) continue;
            MediaModel media = responseMapToMediaModel((HashMap) mediaObject);
            if (media != null) responseMedia.add(media);
        }

        return responseMedia;
    }

    private MediaModel responseXmlToMediaModel(okhttp3.Response response) {
        MediaModel media = new MediaModel();
        try {
            String data = new String(response.body().bytes(), "UTF-8");
            InputStream is = new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
            Object obj = XMLSerializerUtils.deserialize(XMLSerializerUtils.scrubXmlResponse(is));
            if (obj instanceof Map) {
                Map<String, String> map = (Map) obj;
                media.setMediaId(Long.parseLong(map.get(MEDIA_ID_KEY)));
            }
        } catch (IOException | XMLRPCException | XmlPullParserException e) {
        }
        return media;
    }

    private MediaModel responseMapToMediaModel(HashMap<String, ?> responseMap) {
        if (responseMap == null || responseMap.isEmpty()) return null;

        String link = MapUtils.getMapStr(responseMap, LINK_KEY);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(link);

        MediaModel mediaModel = new MediaModel();
        mediaModel.setMediaId(MapUtils.getMapLong(responseMap, MEDIA_ID_KEY));
        mediaModel.setPostId(MapUtils.getMapLong(responseMap, POST_ID_KEY));
        mediaModel.setTitle(MapUtils.getMapStr(responseMap, TITLE_KEY));
        mediaModel.setCaption(MapUtils.getMapStr(responseMap, CAPTION_KEY));
        mediaModel.setDescription(MapUtils.getMapStr(responseMap, DESCRIPTION_KEY));
        mediaModel.setVideoPressGuid(MapUtils.getMapStr(responseMap, VIDEOPRESS_GUID_KEY));
        mediaModel.setUrl(link);
        mediaModel.setFileName(link.replaceAll(FILE_NAME_REGEX, "$1"));
        mediaModel.setFileExtension(fileExtension);
        mediaModel.setMimeType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension));
        mediaModel.setThumbnailUrl(MapUtils.getMapStr(responseMap, THUMBNAIL_URL_KEY));
        mediaModel.setUploadDate(MapUtils.getMapDate(responseMap, DATE_UPLOADED_KEY).toString());

        Object metadataObject = responseMap.get(METADATA_KEY);
        if (metadataObject instanceof Map) {
            Map metadataMap = (Map) metadataObject;
            mediaModel.setWidth(MapUtils.getMapInt(metadataMap, WIDTH_KEY));
            mediaModel.setHeight(MapUtils.getMapInt(metadataMap, HEIGHT_KEY));
        }

        return mediaModel;
    }

    private void notifyMediaProgress(MediaModel media, float progress) {
        if (mListener != null) {
            mListener.onMediaUploadProgress(MediaAction.UPLOAD_MEDIA, media, progress);
        }
    }

    private void notifyMediaPulled(MediaAction cause, MediaModel media) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        notifyMediaPulled(cause, mediaList);
    }

    private void notifyMediaPulled(MediaAction cause, List<MediaModel> media) {
        if (mListener != null) {
            mListener.onMediaPulled(cause, media);
        }
    }

    private void notifyMediaPushed(MediaAction cause, MediaModel media) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        notifyMediaPushed(cause, mediaList);
    }

    private void notifyMediaPushed(MediaAction cause, List<MediaModel> media) {
        if (mListener != null) {
            mListener.onMediaPushed(cause, media);
        }
    }

    private void notifyMediaError(MediaAction cause, MediaModel media, MediaNetworkError error, Exception exception) {
        if (mListener != null) {
            error.exception = exception;
            mListener.onMediaError(cause, media, error);
        }
    }
}
