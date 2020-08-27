package org.wordpress.android.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.helpers.MediaFile;

import java.io.File;

public class FluxCUtils {
    public static class FluxCUtilsLoggingException extends Exception {
        public FluxCUtilsLoggingException(String message) {
            super(message);
        }
        public FluxCUtilsLoggingException(Throwable originalException) {
            super(originalException);
        }
    }

    /**
     * This method doesn't do much, but insure we're doing the same check in all parts of the app.
     *
     * @return true if the user is signed in a WordPress.com account or if he has a .org site.
     */
    public static boolean isSignedInWPComOrHasWPOrgSite(AccountStore accountStore, SiteStore siteStore) {
        return accountStore.hasAccessToken() || siteStore.hasSiteAccessedViaXMLRPC();
    }

    public static MediaModel mediaModelFromMediaFile(MediaFile file) {
        if (file == null) {
            return null;
        }

        MediaModel mediaModel = new MediaModel();
        mediaModel.setFileName(file.getFileName());
        mediaModel.setFilePath(file.getFilePath());
        mediaModel.setFileExtension(org.wordpress.android.fluxc.utils.MediaUtils.getExtension(file.getFilePath()));
        mediaModel.setMimeType(file.getMimeType());
        mediaModel.setThumbnailUrl(file.getThumbnailURL());
        mediaModel.setUrl(file.getFileURL());
        mediaModel.setTitle(file.getTitle());
        mediaModel.setDescription(file.getDescription());
        mediaModel.setCaption(file.getCaption());
        mediaModel.setMediaId(file.getMediaId() != null ? Long.valueOf(file.getMediaId()) : 0);
        mediaModel.setId(file.getId());
        mediaModel.setUploadState(file.getUploadState());
        mediaModel.setLocalSiteId(Integer.valueOf(file.getBlogId()));
        mediaModel.setVideoPressGuid(ShortcodeUtils.getVideoPressIdFromShortCode(file.getVideoPressShortCode()));
        return mediaModel;
    }

    public static MediaFile mediaFileFromMediaModel(MediaModel media) {
        if (media == null) {
            return null;
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.setBlogId(String.valueOf(media.getLocalSiteId()));
        mediaFile.setMediaId(media.getMediaId() > 0 ? String.valueOf(media.getMediaId()) : null);
        mediaFile.setId(media.getId());
        mediaFile.setFileName(media.getFileName());
        mediaFile.setFilePath(media.getFilePath());
        mediaFile.setMimeType(media.getMimeType());
        mediaFile.setThumbnailURL(media.getThumbnailUrl());
        mediaFile.setFileURL(media.getUrl());
        mediaFile.setTitle(media.getTitle());
        mediaFile.setDescription(media.getDescription());
        mediaFile.setCaption(media.getCaption());
        mediaFile.setUploadState(media.getUploadState());
        mediaFile.setVideo(org.wordpress.android.fluxc.utils.MediaUtils.isVideoMimeType(media.getMimeType()));
        mediaFile.setVideoPressShortCode(ShortcodeUtils.getVideoPressShortcodeFromId(media.getVideoPressGuid()));
        mediaFile.setHeight(media.getHeight());
        mediaFile.setWidth(media.getWidth());
        return mediaFile;
    }

    /**
     * This method returns a FluxC MediaModel from a device media URI
     *
     * @return MediaModel or null in case of problems reading the URI
     */
    public static MediaModel mediaModelFromLocalUri(@NonNull Context context,
                                                    @NonNull Uri uri,
                                                    @Nullable String mimeType,
                                                    @NonNull org.wordpress.android.fluxc.store.MediaStore mediaStore,
                                                    int localSiteId) {
        String path = MediaUtils.getRealPathFromURI(context, uri);

        if (TextUtils.isEmpty(path)) {
            AppLog.d(T.UTILS, "The input URI " + uri.toString() + " can't be read.");
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            AppLog.d(T.UTILS, "The input URI " + uri.toString() + ", converted locally to " + path
                           + " doesn't exist.");
            return null;
        }

        MediaModel media = mediaStore.instantiateMediaModel();
        String filename = org.wordpress.android.fluxc.utils.MediaUtils.getFileName(path);
        String fileExtension = org.wordpress.android.fluxc.utils.MediaUtils.getExtension(path);

        if (TextUtils.isEmpty(mimeType)) {
            mimeType = UrlUtils.getUrlMimeType(uri.toString());
            if (mimeType == null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                if (mimeType == null) {
                    // Default to image jpeg
                    mimeType = "image/jpeg";
                }
            }
        }

        // If file extension is null, upload won't work on wordpress.com
        if (fileExtension == null) {
            fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            filename += "." + fileExtension;
        }

        media.setFileName(filename);
        media.setTitle(filename);
        media.setFilePath(path);
        media.setLocalSiteId(localSiteId);
        media.setFileExtension(fileExtension);
        media.setMimeType(mimeType);
        media.setUploadState(MediaModel.MediaUploadState.QUEUED);
        media.setUploadDate(DateTimeUtils.iso8601UTCFromTimestamp(System.currentTimeMillis() / 1000));

        return media;
    }
}
