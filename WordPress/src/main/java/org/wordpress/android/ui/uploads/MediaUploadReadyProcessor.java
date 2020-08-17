package org.wordpress.android.ui.uploads;

import androidx.annotation.Nullable;

import org.wordpress.android.WordPress;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.ui.media.services.MediaUploadReadyListener;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase;
import org.wordpress.android.util.helpers.MediaFile;


public class MediaUploadReadyProcessor implements MediaUploadReadyListener {
    @Override
    public PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile,
                                                   String siteUrl) {
        if (post != null) {
            boolean showAztecEditor = AppPrefs.isAztecEditorEnabled();
            boolean showGutenbergEditor = AppPrefs.isGutenbergEditorEnabled();

            if (showGutenbergEditor && PostUtils.contentContainsGutenbergBlocks(post.getContent())) {
                post.setContent(

                        PostUtils.replaceMediaFileWithUrlInGutenbergPost(post.getContent(), localMediaId, mediaFile,
                                siteUrl));
            } else if (showAztecEditor) {
                post.setContent(AztecEditorFragment.replaceMediaFileWithUrl(WordPress.getContext(), post.getContent(),
                                                                            localMediaId, mediaFile));
            }
        }

        return post;
    }

    @Override
    public PostModel markMediaUploadFailedInPost(@Nullable PostModel post, String localMediaId,
                                                 final MediaFile mediaFile) {
        if (post != null) {
            boolean showAztecEditor = AppPrefs.isAztecEditorEnabled();
            boolean showGutenbergEditor = AppPrefs.isGutenbergEditorEnabled();

            if (showGutenbergEditor) {
                // TODO check if anything needs be done in Gutenberg
            } else if (showAztecEditor) {
                post.setContent(AztecEditorFragment.markMediaFailed(WordPress.getContext(), post.getContent(),
                                                                    localMediaId, mediaFile));
            }
        }

        return post;
    }

    @Override public PostModel replaceMediaLocalIdWithRemoteMediaIdInPost(@Nullable PostModel post,
                                                                          MediaFile mediaFile) {
        if (PostUtils.contentContainsWPStoryGutenbergBlocks(post.getContent())) {
            SaveStoryGutenbergBlockUseCase saveStoryGutenbergBlockUseCase = new SaveStoryGutenbergBlockUseCase();
            saveStoryGutenbergBlockUseCase
                    .replaceLocalMediaIdsWithRemoteMediaIdsInPost(post, mediaFile);
        } else {
            post.setContent(
                    PostUtils.replaceMediaLocalIdWithMediaRemoteIdInStoryPost(
                            post.getContent(),
                            String.valueOf(mediaFile.getId()),
                            String.valueOf(mediaFile.getMediaId())
                    )
            );
        }
        return post;
    }
}
