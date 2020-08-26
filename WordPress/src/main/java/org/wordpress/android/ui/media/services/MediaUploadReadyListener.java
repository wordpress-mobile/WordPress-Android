package org.wordpress.android.ui.media.services;

import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.util.helpers.MediaFile;

/**
 * Callbacks - requests for editor capabilities to replace media once it's finished uploading
 * and mark media failed if could not be uploaded
 */
public interface MediaUploadReadyListener {
    PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile,
                                            String siteUrl);
    PostModel markMediaUploadFailedInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile);
}
