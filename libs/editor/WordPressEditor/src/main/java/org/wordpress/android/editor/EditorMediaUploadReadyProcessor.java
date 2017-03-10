package org.wordpress.android.editor;

import android.support.annotation.Nullable;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.util.helpers.MediaFile;

public class EditorMediaUploadReadyProcessor implements EditorMediaUploadReadyListener {
    @Override
    public PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile) {
        return null;
    }
}
