package org.wordpress.android.ui.posts.services;

import android.support.annotation.Nullable;

import org.wordpress.android.WordPress;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.editor.EditorFragment;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.ui.media.services.MediaUploadReadyListener;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.helpers.MediaFile;

public class MediaUploadReadyProcessor implements MediaUploadReadyListener {
    @Override
    public PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile) {
        if (post != null) {

            boolean showAztecEditor = AppPrefs.isAztecEditorEnabled();
            boolean showNewEditor = AppPrefs.isVisualEditorEnabled();

            if (showAztecEditor) {
                String modifiedContents = AztecEditorFragment.replaceMediaFileWithUrl(WordPress.getContext(), post.getContent(),
                        localMediaId, mediaFile);
                post.setContent(modifiedContents);
            } else if (showNewEditor) {
                post.setContent(EditorFragment.replaceMediaFileWithUrl(post.getContent(), mediaFile));
            }
            // No implementation necessary for the legacy editor as it doesn't support uploading media while editing
        }

        return post;
    }
}
