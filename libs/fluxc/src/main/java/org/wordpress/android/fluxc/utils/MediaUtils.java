package org.wordpress.android.fluxc.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.File;
import java.io.IOException;

public class MediaUtils {
    private static final MimeTypes MIME_TYPES = new MimeTypes();
    public static final double MEMORY_LIMIT_FILESIZE_MULTIPLIER = 0.75D;

    public static boolean isImageMimeType(@Nullable String type) {
        return MIME_TYPES.isImageType(type);
    }

    public static boolean isVideoMimeType(@Nullable String type) {
        return MIME_TYPES.isVideoType(type);
    }

    public static boolean isAudioMimeType(@Nullable String type) {
        return MIME_TYPES.isAudioType(type);
    }

    public static boolean isApplicationMimeType(@Nullable String type) {
        return MIME_TYPES.isApplicationType(type);
    }

    public static boolean isSupportedImageMimeType(@Nullable String type) {
        return MIME_TYPES.isSupportedImageType(type);
    }

    public static boolean isSupportedVideoMimeType(@Nullable String type) {
        return MIME_TYPES.isSupportedVideoType(type);
    }

    public static boolean isSupportedAudioMimeType(@Nullable String type) {
        return MIME_TYPES.isSupportedAudioType(type);
    }

    public static boolean isSupportedApplicationMimeType(@Nullable String type) {
        return MIME_TYPES.isSupportedApplicationType(type);
    }

    public static boolean isSupportedMimeType(@Nullable String type) {
        return isSupportedImageMimeType(type)
                || isSupportedVideoMimeType(type)
                || isSupportedAudioMimeType(type)
                || isSupportedApplicationMimeType(type);
    }

    @Nullable
    public static String getMimeTypeForExtension(@Nullable String extension) {
        return MIME_TYPES.getMimeTypeForExtension(extension);
    }

    //
    // File operations
    //

    @NonNull
    @SuppressWarnings("unused")
    public static String getMediaValidationError(@NonNull MediaModel media) {
        return BaseUploadRequestBody.hasRequiredData(media);
    }

    @NonNull
    public static MalformedMediaArgSubType getMediaValidationErrorType(@NonNull MediaModel media) {
        return BaseUploadRequestBody.checkMediaArg(media);
    }

    /**
     * Queries filesystem to determine if a given file can be read.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canReadFile(@Nullable String filePath) {
        if (filePath == null || TextUtils.isEmpty(filePath)) return false;
        File file = new File(filePath);
        return file.canRead();
    }

    /**
     * Returns the substring of characters that follow the final '.' in the given string.
     */
    @Nullable
    public static String getExtension(@Nullable String filePath) {
        if (TextUtils.isEmpty(filePath) || !filePath.contains(".")) return null;
        if (filePath.lastIndexOf(".") + 1 >= filePath.length()) return null;
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }

    /**
     * Returns the substring of characters that follow the final '/' in the given string.
     */
    @Nullable
    public static String getFileName(@Nullable String filePath) {
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
     */
    public static void stripLocation(@Nullable String imagePath) {
        if (imagePath != null) {
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
            } catch (IOException e) {
                AppLog.e(T.MEDIA, "Removing of GPS info from image failed [IO Exception]");
            }
        } else {
            AppLog.e(T.MEDIA, "Removing of GPS info from image failed [Null Image Path]");
        }
    }
}
