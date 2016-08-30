package org.wordpress.android.fluxc.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaUtils {
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

    //
    // MediaModel
    //

    private static final String MEDIA_TITLE_KEY = "title";
    private static final String MEDIA_DESCRIPTION_KEY = "description";
    private static final String MEDIA_CAPTION_KEY = "caption";
    private static final String MEDIA_ALT_KEY = "alt";
    private static final String MEDIA_PARENT_KEY = "parent_id";

    /**
     * Creates a {@link MediaModel} list from a WP.com REST response to a request for all media.
     */
    public static List<MediaModel> mediaListFromRestResponse(MultipleMediaResponse from) {
        if (from == null || from.media == null) return null;
        List<MediaModel> media = new ArrayList<>();
        for (int i = 0; i < from.media.size(); ++i) {
            media.add(i, mediaFromRestResponse(from.media.get(i)));
        }
        return media;
    }

    /**
     * Creates a {@link MediaModel} from a WP.com REST response to a fetch request.
     */
    public static MediaModel mediaFromRestResponse(MediaWPComRestResponse from) {
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
     * The current REST API call (v1.1) accepts 'title', 'description', 'caption', 'alt',
     * and 'parent_id' for all media. Audio media also accepts 'artist' and 'album' attributes.
     *
     * ref https://developer.wordpress.com/docs/api/1.1/post/sites/%24site/media/new/
     */
    public static @NonNull Map<String, String> getMediaRestParams(@NonNull MediaModel media) {
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
        return isSupportedImageMimeType(type) ||
               isSupportedVideoMimeType(type) ||
               isSupportedAudioMimeType(type) ||
               isSupportedApplicationMimeType(type);
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
}
