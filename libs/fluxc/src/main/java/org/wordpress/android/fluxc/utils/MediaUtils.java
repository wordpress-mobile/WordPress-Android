package org.wordpress.android.fluxc.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
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

    // Every GPS tag ExifInterface supports - setting a tag to null removes it from the file
    @VisibleForTesting
    static final String[] GPS_TAGS = {
            ExifInterface.TAG_GPS_VERSION_ID,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_SATELLITES,
            ExifInterface.TAG_GPS_STATUS,
            ExifInterface.TAG_GPS_MEASURE_MODE,
            ExifInterface.TAG_GPS_DOP,
            ExifInterface.TAG_GPS_SPEED_REF,
            ExifInterface.TAG_GPS_SPEED,
            ExifInterface.TAG_GPS_TRACK_REF,
            ExifInterface.TAG_GPS_TRACK,
            ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
            ExifInterface.TAG_GPS_IMG_DIRECTION,
            ExifInterface.TAG_GPS_MAP_DATUM,
            ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
            ExifInterface.TAG_GPS_DEST_LATITUDE,
            ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
            ExifInterface.TAG_GPS_DEST_LONGITUDE,
            ExifInterface.TAG_GPS_DEST_BEARING_REF,
            ExifInterface.TAG_GPS_DEST_BEARING,
            ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
            ExifInterface.TAG_GPS_DEST_DISTANCE,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_AREA_INFORMATION,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_DIFFERENTIAL,
            ExifInterface.TAG_GPS_H_POSITIONING_ERROR
    };

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
                for (String tag : GPS_TAGS) {
                    exifInterface.setAttribute(tag, null);
                }
                exifInterface.saveAttributes();
            } catch (IOException e) {
                AppLog.e(T.MEDIA, "Removing of GPS info from image failed [IO Exception]");
            }
        } else {
            AppLog.e(T.MEDIA, "Removing of GPS info from image failed [Null Image Path]");
        }
    }
}
