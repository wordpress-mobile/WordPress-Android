package org.wordpress.android.ui.media.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.helpers.MediaFile;

/**
 * Callbacks - requests for editor capabilities to replace media once it's finished uploading
 * and mark media failed if could not be uploaded
 */
public interface MediaUploadReadyListener {
    // TODO: We're passing a SiteModel parameter here in order to debug a crash on SaveStoryGutenbergBlockUseCase.
    //  Once that's done, the parameter should be replaced with a site url String, like it was before.
    //  See: https://git.io/JqfhK
    PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, @NonNull String localMediaId, MediaFile mediaFile,
                                            @Nullable SiteModel site);
    PostModel markMediaUploadFailedInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile);
}
