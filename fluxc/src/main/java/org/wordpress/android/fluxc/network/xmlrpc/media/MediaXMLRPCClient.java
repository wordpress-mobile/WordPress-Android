package org.wordpress.android.fluxc.network.xmlrpc.media;

import android.webkit.MimeTypeMap;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPC;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.store.MediaStore.ChangedMediaPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaXMLRPCClient extends BaseXMLRPCClient {
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

    public MediaXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                             UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
    }

    public void pullAllMedia(String xmlRpcUrl, String username, String password) {
        List<Object> params = new ArrayList<>(3);
        // TODO: need site ID
//        params.add();
        params.add(username);
        params.add(password);
        XMLRPCRequest request = new XMLRPCRequest(xmlRpcUrl, XMLRPC.GET_MEDIA_LIBRARY, params,
                new Listener() {
                    @Override public void onResponse(Object response) {
                        AppLog.v(AppLog.T.API, "Successful response from XMLRPC.getMediaLibrary");

                        List<MediaModel> media = allMediaResponseToMediaModelList(response);
                        ChangedMediaPayload payload = new ChangedMediaPayload(media, null, null);
//                        mDispatcher.dispatch(MediaActionBuilder.newFetchedAllMediaAction(payload));
                    }
                },
                new ErrorListener() {
                    @Override public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.API, "Volley error", error);
                    }
                }
        );
        add(request);
    }

    public void pullMediaItem(String xmlRpcUrl, String username, String password, long mediaId) {
        List<Object> params = new ArrayList<>(3);
        // TODO: need site ID
//        params.add()
        params.add(username);
        params.add(password);
        XMLRPCRequest request = new XMLRPCRequest(xmlRpcUrl, XMLRPC.GET_MEDIA_ITEM, params,
                new Listener() {
                    @Override
                    public void onResponse(Object response) {
                        AppLog.v(AppLog.T.API, "Successful response from XMLRPC.getMediaItem");

                        if (!(response instanceof HashMap)) {
                            // TODO: log? handle error some other way?
                            return;
                        }
                        List<MediaModel> media = new ArrayList<>(1);
                        media.add(responseMapToMediaModel((HashMap) response));
                        ChangedMediaPayload payload = new ChangedMediaPayload(media, null, null);
//                        mDispatcher.dispatch(MediaActionBuilder.newFetchedMediaAction(payload));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.API, "Volley error", error);
                    }
                }
        );
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
}
