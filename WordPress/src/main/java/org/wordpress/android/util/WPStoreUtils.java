package org.wordpress.android.util;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.helpers.MediaFile;

public class WPStoreUtils {
    /**
     * This method doesn't do much, but insure we're doing the same check in all parts of the app.
     * @return true if the user is signed in a WordPress.com account or if he has a .org site.
     */
    public static boolean isSignedInWPComOrHasWPOrgSite(AccountStore accountStore, SiteStore siteStore) {
        return accountStore.hasAccessToken() || siteStore.hasSelfHostedSite();
    }

    public static MediaModel fromMediaFile(MediaFile file) {
        MediaModel mediaModel = new MediaModel();
        mediaModel.setFileName(file.getFileName());
        mediaModel.setFilePath(file.getFilePath());
        mediaModel.setFileExtension(org.wordpress.android.fluxc.utils.MediaUtils.getExtension(file.getFilePath()));
        mediaModel.setMimeType(file.getMimeType());
        mediaModel.setThumbnailUrl(file.getThumbnailURL());
        mediaModel.setTitle(file.getTitle());
        mediaModel.setDescription(file.getDescription());
        mediaModel.setCaption(file.getCaption());
        mediaModel.setMediaId(Long.valueOf(file.getMediaId()));
        mediaModel.setUploadState(file.getUploadState());
        mediaModel.setSiteId(Long.valueOf(file.getBlogId()));
        return mediaModel;
    }
}
