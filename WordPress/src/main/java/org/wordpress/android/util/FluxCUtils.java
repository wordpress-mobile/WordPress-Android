package org.wordpress.android.util;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.helpers.MediaFile;

public class FluxCUtils {
    /**
     * This method doesn't do much, but insure we're doing the same check in all parts of the app.
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
}
