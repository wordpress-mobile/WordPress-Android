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
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPC;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class MediaXMLRPCClient extends BaseXMLRPCClient implements ProgressListener {
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
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.EDIT_MEDIA, params,
                    new Listener() {
                        @Override public void onResponse(Object response) {
                            if (response == null || !(response instanceof Boolean) || !(Boolean) response) {
                                AppLog.v(T.MEDIA, "failed to update media: " + media.getTitle());
                                return;
                            }

                            AppLog.v(T.MEDIA, "media updated: " + media.getUrl());
                            List<MediaModel> mediaList = new ArrayList<>();
                            mediaList.add(media);
                            notifyMediaPushed(MediaAction.PUSH_MEDIA, mediaList, null);
                        }
                },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                                AppLog.i(T.MEDIA, "media does not exist, uploading");
                                performUpload(site, media);
                            } else {
                                AppLog.e(T.MEDIA, "unhandled XMLRPC.EDIT_MEDIA response: " + error);
                            }
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
        add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_LIBRARY, params,
                new Listener() {
                    @Override public void onResponse(Object response) {
                        AppLog.v(T.MEDIA, "Successful response from XMLRPC.getMediaLibrary");
                        List<MediaModel> media = allMediaResponseToMediaModelList(response);
                        notifyMediaPulled(MediaAction.PULL_ALL_MEDIA, media, null);
                    }
                },
                new ErrorListener() {
                    @Override public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.MEDIA, "Volley error", error);
                        notifyMediaError(MediaAction.PULL_ALL_MEDIA, error);
                    }
                }
        ));
    }

    public void pullMedia(SiteModel site, List<Long> mediaIds) {
        for (Long mediaId : mediaIds) {
            List<Object> params = getBasicParams(site);
            params.add(mediaId);
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_MEDIA_ITEM, params,
                    new Listener() {
                        @Override public void onResponse(Object response) {
                            AppLog.v(T.MEDIA, "Successful response from XMLRPC.getMediaItem");
                            List<MediaModel> media = new ArrayList<>(1);
                            media.add(responseMapToMediaModel((HashMap) response));
                            notifyMediaPulled(MediaAction.PULL_MEDIA, media, null);
                        }
                    },
                    new ErrorListener() {
                        @Override public void onErrorResponse(VolleyError error) {
                            AppLog.e(T.MEDIA, "Volley error", error);
                            notifyMediaError(MediaAction.PULL_MEDIA, error);
                        }
                    }
            ));
        }
    }

    public void deleteMedia(SiteModel site, List<MediaModel> media) {
        for (MediaModel mediaItem : media) {
            List<Object> params = getBasicParams(site);
            params.add(mediaItem.getMediaId());
            add(new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_MEDIA, params,
                    new Listener() {
                        @Override public void onResponse(Object response) {
                            AppLog.v(T.MEDIA, "Successful response from XMLRPC.deleteMedia");
                            List<MediaModel> media = new ArrayList<>(1);
                            media.add(responseMapToMediaModel((HashMap) response));
                            notifyMediaDeleted(MediaAction.DELETE_MEDIA, media, null);
                        }
                    },
                    new ErrorListener() {
                        @Override public void onErrorResponse(VolleyError error) {
                            AppLog.e(T.MEDIA, "Volley error", error);
                            notifyMediaError(MediaAction.DELETE_MEDIA, error);
                        }
                    }
            ));
        }
    }

    public void setListener(MediaNetworkListener listener) {
        mListener = listener;
    }

    private void performUpload(SiteModel site, MediaModel media) {
        URL xmlrpcUrl;
        try {
            xmlrpcUrl = new URL(site.getXmlRpcUrl());
        } catch (MalformedURLException e) {
            AppLog.w(T.MEDIA, "bad XMLRPC URL for site: " + site.getXmlRpcUrl());
            return;
        }

        HttpUrl url = new HttpUrl.Builder()
                .scheme(xmlrpcUrl.getProtocol())
                .host(xmlrpcUrl.getHost())
                .encodedPath(xmlrpcUrl.getPath())
                .username(site.getUsername())
                .password(site.getPassword())
                .build();

        XmlrpcUploadRequestBody requestBody = new XmlrpcUploadRequestBody(media, this, site);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    String responseString = response.body().string();
                    AppLog.d(T.MEDIA, "media upload successful: " + responseString);
                    // TODO: serialize MediaModel from response and add to resultList
//                    MediaModel responseMedia = resToMediaModel
                    List<MediaModel> resultList = new ArrayList<>();
//                    resultList.add(responseMedia);
                    notifyMediaPushed(MediaAction.UPLOAD_MEDIA, resultList, null);
                } else {
                    AppLog.w(T.MEDIA, "error uploading media: " + response);
                    notifyMediaError(MediaAction.UPLOAD_MEDIA, new Exception(response.toString()));
                }
            }

            @Override public void onFailure(Call call, IOException e) {
                AppLog.w(T.MEDIA, "media upload failed: " + e);
                notifyMediaError(MediaAction.UPLOAD_MEDIA, e);
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

    private void notifyMediaPulled(MediaAction cause, List<MediaModel> media, List<Exception> errors) {
        if (mListener != null) {
            mListener.onMediaPulled(cause, media, errors);
        }
    }

    private void notifyMediaPushed(MediaAction cause, List<MediaModel> media, List<Exception> errors) {
        if (mListener != null) {
            mListener.onMediaPushed(cause, media, errors);
        }
    }

    private void notifyMediaDeleted(MediaAction cause, List<MediaModel> media, List<Exception> errors) {
        if (mListener != null) {
            mListener.onMediaDeleted(cause, media, errors);
        }
    }

    private void notifyMediaError(MediaAction cause, Exception error) {
        if (mListener != null) {
            mListener.onMediaError(cause, error);
        }
    }
}
