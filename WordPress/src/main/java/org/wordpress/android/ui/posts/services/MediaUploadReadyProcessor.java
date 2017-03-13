package org.wordpress.android.ui.posts.services;

import android.support.annotation.Nullable;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.ui.media.services.MediaUploadReadyListener;
import org.wordpress.android.util.helpers.MediaFile;

public class MediaUploadReadyProcessor implements MediaUploadReadyListener {
    @Override
    public PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile) {
        return null;
    }
}
