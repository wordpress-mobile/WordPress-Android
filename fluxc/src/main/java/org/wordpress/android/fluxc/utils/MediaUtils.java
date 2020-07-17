package org.wordpress.android.fluxc.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.File;
import java.io.IOException;

public class MediaUtils {
    private static final MimeTypes MIME_TYPES = new MimeTypes();
    public static final double MEMORY_LIMIT_FILESIZE_MULTIPLIER = 0.75D;

    public static boolean isImageMimeType(String type) {
        return MIME_TYPES.isImageType(type);
    }

    public static boolean isVideoMimeType(String type) {
        return MIME_TYPES.isVideoType(type);
    }

    public static boolean isAudioMimeType(String type) {
        return MIME_TYPES.isAudioType(type);
    }

    public static boolean isApplicationMimeType(String type) {
        return MIME_TYPES.isApplicationType(type);
    }

    public static boolean isSupportedImageMimeType(String type) {
        return MIME_TYPES.isSupportedImageType(type);
    }

    public static boolean isSupportedVideoMimeType(String type) {
        return MIME_TYPES.isSupportedVideoType(type);
    }

    public static boolean isSupportedAudioMimeType(String type) {
        return MIME_TYPES.isSupportedAudioType(type);
    }

    public static boolean isSupportedApplicationMimeType(String type) {
        return MIME_TYPES.isSupportedApplicationType(type);
    }

    public static boolean isSupportedMimeType(String type) {
        return isSupportedImageMimeType(type)
                || isSupportedVideoMimeType(type)
                || isSupportedAudioMimeType(type)
                || isSupportedApplicationMimeType(type);
    }

    public static String getMimeTypeForExtension(String extension) {
        return MIME_TYPES.getMimeTypeForExtension(extension);
    }

    //
    // File operations
    //

    public static String getMediaValidationError(@NonNull MediaModel media) {
        return BaseUploadRequestBody.hasRequiredData(media);
    }

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

    /**
     * Given the memory limit for media for a site, returns the maximum 'safe' file size we can upload to that site.
     */
    public static double getMaxFilesizeForMemoryLimit(double mediaMemoryLimit) {
        return MEMORY_LIMIT_FILESIZE_MULTIPLIER * mediaMemoryLimit;
    }

    /**
     * Removes location from the Exif information from an image
     *
     * @param imagePath image file path
     * @return success
     */
    public static boolean stripLocation(String imagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "0/0");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "0/0,0/0000,00000000/00000");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "0");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "0/0,0/0,000000/00000 ");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "0");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, "00:00:00");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "0");
            exifInterface.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, " ");
            exifInterface.saveAttributes();
            return true;
        } catch (IOException e) {
            AppLog.e(T.MEDIA, "Removing of GPS info from image failed");
            return false;
        }
    }
}
