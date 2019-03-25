package org.wordpress.android.ui.uploads;

import android.support.annotation.Nullable;

import org.wordpress.android.WordPress;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.editor.EditorFragment;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.ui.media.services.MediaUploadReadyListener;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.helpers.MediaFile;


public class MediaUploadReadyProcessor implements MediaUploadReadyListener {
    @Override
    public PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile) {
        if (post != null) {
            boolean showAztecEditor = AppPrefs.isAztecEditorEnabled();
            boolean showNewEditor = AppPrefs.isVisualEditorEnabled();
            boolean showGutenbergEditor = AppPrefs.isGutenbergEditorEnabled();

            if (showGutenbergEditor && PostUtils.contentContainsGutenbergBlocks(post.getContent())) {
                post.setContent(
                        PostUtils.replaceMediaFileWithUrlInGutenbergPost(post.getContent(), localMediaId, mediaFile));
            } else if (showAztecEditor) {
                post.setContent(AztecEditorFragment.replaceMediaFileWithUrl(WordPress.getContext(), post.getContent(),
                                                                            localMediaId, mediaFile));
            } else if (showNewEditor) {
                post.setContent(EditorFragment.replaceMediaFileWithUrl(post.getContent(), mediaFile));
            }
            // No implementation necessary for the legacy editor as it doesn't support uploading media while editing
        }

        return post;
    }

    @Override
    public PostModel markMediaUploadFailedInPost(@Nullable PostModel post, String localMediaId,
                                                 final MediaFile mediaFile) {
        if (post != null) {
            boolean showAztecEditor = AppPrefs.isAztecEditorEnabled();
            boolean showNewEditor = AppPrefs.isVisualEditorEnabled();
            boolean showGutenbergEditor = AppPrefs.isGutenbergEditorEnabled();

            if (showGutenbergEditor) {
                // TODO check if anything needs be done in Gutenberg
            } else if (showAztecEditor) {
                post.setContent(AztecEditorFragment.markMediaFailed(WordPress.getContext(), post.getContent(),
                                                                    localMediaId, mediaFile));
            } else if (showNewEditor) {
                // No implementation necessary for the Visual Editor as it marks media failed at Post open
            }
            // No implementation necessary for the legacy editor as it doesn't support uploading media while editing
        }

        return post;
    }
}
