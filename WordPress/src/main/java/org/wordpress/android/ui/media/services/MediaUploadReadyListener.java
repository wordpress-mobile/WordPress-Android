package org.wordpress.android.ui.media.services;

import android.support.annotation.Nullable;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.util.helpers.MediaFile;

/**
 * Callbacks - requests for editor capabilities to replace media once it's finished uploading
 * and mark media failed if could not be uploaded
 */
public interface MediaUploadReadyListener {
    PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, final String localMediaId,
                                            final MediaFile mediaFile);
    PostModel markMediaUploadFailedInPost(@Nullable PostModel post, final String localMediaId,
                                          final MediaFile mediaFile);
}
