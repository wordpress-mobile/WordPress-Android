package org.wordpress.android.fluxc.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCException;
import org.wordpress.android.fluxc.network.xmlrpc.XMLSerializerUtils;
import org.wordpress.android.fluxc.store.MediaStore.MediaFilter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MapUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaUtils {
    //
    // WP.com REST API
    //

    private static final String MEDIA_TITLE_KEY = "title";
    private static final String MEDIA_DESCRIPTION_KEY = "description";
    private static final String MEDIA_CAPTION_KEY = "caption";
    private static final String MEDIA_ALT_KEY = "alt";
    private static final String MEDIA_PARENT_KEY = "parent_id";

    /**
     * Creates a {@link MediaModel} list from a WP.com REST response to a request for all media.
     */
    public static List<MediaModel> mediaListFromRestResponse(MultipleMediaResponse from, long siteId) {
        if (from == null || from.media == null) return null;
        List<MediaModel> media = new ArrayList<>();
        for (int i = 0; i < from.media.size(); ++i) {
            media.add(i, mediaFromRestResponse(from.media.get(i), siteId));
        }
        return media;
    }

    /**
     * Creates a {@link MediaModel} from a WP.com REST response to a fetch request.
     */
    public static MediaModel mediaFromRestResponse(MediaWPComRestResponse from, long siteId) {
        MediaModel media = new MediaModel();
        media.setSiteId(siteId);
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
     * The current REST API call (v1.1) accepts 'title', 'description', 'caption', 'alt',
     * and 'parent_id' for all media. Audio media also accepts 'artist' and 'album' attributes.
     *
     * ref https://developer.wordpress.com/docs/api/1.1/post/sites/%24site/media/new/
     */
    @NonNull
    public static Map<String, String> getMediaRestParams(@NonNull MediaModel media) {
        final Map<String, String> params = new HashMap<>();
        if (!TextUtils.isEmpty(media.getTitle())) {
            params.put(MEDIA_TITLE_KEY, media.getTitle());
        }
        if (!TextUtils.isEmpty(media.getDescription())) {
            params.put(MEDIA_DESCRIPTION_KEY, media.getDescription());
        }
        if (!TextUtils.isEmpty(media.getCaption())) {
            params.put(MEDIA_CAPTION_KEY, media.getCaption());
        }
        if (!TextUtils.isEmpty(media.getAlt())) {
            params.put(MEDIA_ALT_KEY, media.getAlt());
        }
        if (media.getPostId() > 0) {
            params.put(MEDIA_PARENT_KEY, String.valueOf(media.getPostId()));
        }
        return params;
    }

    //
    // Self hosted XMLRPC API
    //

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

    public static List<MediaModel> mediaListFromXmlrpcResponse(Object response, long siteId) {
        if (!(response instanceof Object[])) return null;

        Object[] responseArray = (Object[]) response;
        List<MediaModel> responseMedia = new ArrayList<>();
        for (Object mediaObject : responseArray) {
            if (!(mediaObject instanceof HashMap)) continue;
            MediaModel media = mediaFromXmlrpcResponse((HashMap) mediaObject, siteId);
            if (media != null) responseMedia.add(media);
        }

        return responseMedia;
    }

    public static MediaModel mediaFromXmlrpcResponse(HashMap<String, ?> response, long siteId) {
        if (response == null || response.isEmpty()) return null;

        String link = MapUtils.getMapStr(response, LINK_KEY);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(link);

        MediaModel media = new MediaModel();
        media.setSiteId(siteId);
        media.setMediaId(MapUtils.getMapLong(response, MEDIA_ID_KEY));
        media.setPostId(MapUtils.getMapLong(response, POST_ID_KEY));
        media.setTitle(MapUtils.getMapStr(response, TITLE_KEY));
        media.setCaption(MapUtils.getMapStr(response, CAPTION_KEY));
        media.setDescription(MapUtils.getMapStr(response, DESCRIPTION_KEY));
        media.setVideoPressGuid(MapUtils.getMapStr(response, VIDEOPRESS_GUID_KEY));
        media.setUrl(link);
        media.setFileName(getFileName(link));
        media.setFileExtension(fileExtension);
        media.setMimeType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension));
        media.setThumbnailUrl(MapUtils.getMapStr(response, THUMBNAIL_URL_KEY));
        media.setUploadDate(MapUtils.getMapDate(response, DATE_UPLOADED_KEY).toString());

        Object metadataObject = response.get(METADATA_KEY);
        if (metadataObject instanceof Map) {
            Map metadataMap = (Map) metadataObject;
            media.setWidth(MapUtils.getMapInt(metadataMap, WIDTH_KEY));
            media.setHeight(MapUtils.getMapInt(metadataMap, HEIGHT_KEY));
        }

        return media;
    }

    public static MediaModel mediaFromXmlrpcUploadResponse(okhttp3.Response response) {
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
            AppLog.w(AppLog.T.MEDIA, "failed to parse XMLRPC.wpUploadFile response: " + response);
            return null;
        }
        return media;
    }

    // filter query parameter keys for XMLRPC.GET_MEDIA_LIBRARY
    public static final String NUMBER_FILTER_KEY = "number";
    public static final String OFFSET_FILTER_KEY = "offset";
    public static final String PARENT_FILTER_KEY = "parent_id";
    public static final String MIME_TYPE_FILTER_KEY = "mime_type";

    public static void addFilterParams(@NonNull final List<Object> params, @NonNull final MediaFilter filter) {
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
        if (!queryParams.isEmpty()) {
            params.add(queryParams);
        }
    }

    //
    // MIME types
    //

    public static final String MIME_TYPE_IMAGE       = "image/";
    public static final String MIME_TYPE_VIDEO       = "video/";
    public static final String MIME_TYPE_AUDIO       = "audio/";
    public static final String MIME_TYPE_APPLICATION = "application/";

    // ref https://en.support.wordpress.com/accepted-filetypes/
    public static final String[] SUPPORTED_IMAGE_SUBTYPES = {
            "jpg", "jpeg", "png", "gif"
    };
    public static final String[] SUPPORTED_VIDEO_SUBTYPES = {
            "mp4", "m4v", "mov", "wmv", "avi", "mpg", "ogv", "3gp", "3g2"
    };
    public static final String[] SUPPORTED_AUDIO_SUBTYPES = {
            "mp3", "m4a", "ogg", "wav"
    };
    public static final String[] SUPPORTED_APPLICATION_SUBTYPES = {
            "pdf", "doc", "ppt", "odt", "pptx", "docx", "pps", "ppsx", "xls", "xlsx", "key", ".zip"
    };

    public static boolean isImageMimeType(String type) {
        return isExpectedMimeType(MIME_TYPE_IMAGE, type);
    }

    public static boolean isVideoMimeType(String type) {
        return isExpectedMimeType(MIME_TYPE_VIDEO, type);
    }

    public static boolean isAudioMimeType(String type) {
        return isExpectedMimeType(MIME_TYPE_AUDIO, type);
    }

    public static boolean isApplicationMimeType(String type) {
        return isExpectedMimeType(MIME_TYPE_APPLICATION, type);
    }

    public static boolean isSupportedImageMimeType(String type) {
        return isSupportedMimeType(MIME_TYPE_IMAGE, SUPPORTED_IMAGE_SUBTYPES, type);
    }

    public static boolean isSupportedVideoMimeType(String type) {
        return isSupportedMimeType(MIME_TYPE_VIDEO, SUPPORTED_VIDEO_SUBTYPES, type);
    }

    public static boolean isSupportedAudioMimeType(String type) {
        return isSupportedMimeType(MIME_TYPE_AUDIO, SUPPORTED_AUDIO_SUBTYPES, type);
    }

    public static boolean isSupportedApplicationMimeType(String type) {
        return isSupportedMimeType(MIME_TYPE_APPLICATION, SUPPORTED_APPLICATION_SUBTYPES, type);
    }

    public static boolean isSupportedMimeType(String type) {
        return isSupportedImageMimeType(type)
                || isSupportedVideoMimeType(type)
                || isSupportedAudioMimeType(type)
                || isSupportedApplicationMimeType(type);
    }

    public static String getMimeTypeForExtension(String extension) {
        if (isSupportedImageMimeType(MIME_TYPE_IMAGE + extension)) {
            return MIME_TYPE_IMAGE + extension;
        }
        if (isSupportedVideoMimeType(MIME_TYPE_VIDEO + extension)) {
            return MIME_TYPE_VIDEO + extension;
        }
        if (isSupportedAudioMimeType(MIME_TYPE_AUDIO + extension)) {
            return MIME_TYPE_AUDIO + extension;
        }
        if (isSupportedApplicationMimeType(MIME_TYPE_APPLICATION + extension)) {
            return MIME_TYPE_APPLICATION + extension;
        }
        return null;
    }

    private static boolean isExpectedMimeType(String expected, String type) {
        if (type == null) return false;
        String[] split = type.split("/");
        return split.length == 2 && expected.startsWith(split[0]);
    }

    private static boolean isSupportedMimeType(String type, String[] supported, String mimeType) {
        if (type == null || supported == null || mimeType == null) return false;
        for (String supportedSubtype : supported) {
            if (mimeType.equals(type + supportedSubtype)) return true;
        }
        return false;
    }

    //
    // File operations
    //

    /**
     * Queries filesystem to determine if a given file can be read.
     */
    public static boolean canReadFile(String filePath) {
        if (filePath == null || TextUtils.isEmpty(filePath)) return false;
        File file = new File(filePath);
        return file.canRead();
    }

    /**
     * Returns the substring of characters that follow the final '.' in the given string.
     */
    public static String getExtension(String filePath) {
        if (TextUtils.isEmpty(filePath) || !filePath.contains(".")) return null;
        if (filePath.lastIndexOf(".") + 1 >= filePath.length()) return null;
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }

    /**
     * Returns the substring of characters that follow the final '/' in the given string.
     */
    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath) || !filePath.contains("/")) return null;
        if (filePath.lastIndexOf("/") + 1 >= filePath.length()) return null;
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }
}
