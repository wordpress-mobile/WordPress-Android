package org.wordpress.android.ui.posts.services;

import android.support.annotation.Nullable;

import org.ccil.cowan.tagsoup.AttributesImpl;
import org.wordpress.android.WordPress;
import org.wordpress.android.editor.AztecEditorFragment;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.ui.media.services.MediaUploadReadyListener;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.aztec.AztecText;

public class MediaUploadReadyProcessor implements MediaUploadReadyListener {
    @Override
    public PostModel replaceMediaFileWithUrlInPost(@Nullable PostModel post, String localMediaId, MediaFile mediaFile) {
        if (post != null) {

            boolean showAztecEditor = AppPrefs.isAztecEditorEnabled();
            boolean showNewEditor = AppPrefs.isVisualEditorEnabled();

            if (showAztecEditor) {
                if (mediaFile != null) {
                    String remoteUrl = Utils.escapeQuotes(mediaFile.getFileURL());
                    if (!mediaFile.isVideo()) {
                        AttributesImpl attrs = new AttributesImpl();
                        attrs.addAttribute("", "src", "src", "string", remoteUrl);

                        // clear overlay
                        AztecText content = new AztecText(WordPress.getContext());
                        content.fromHtml(post.getContent());
                        content.clearOverlays(AztecEditorFragment.ImagePredicate.localMediaIdPredicate(localMediaId), attrs);
                        content.refreshText();

                        // re-set the post content
                        post.setContent(content.toHtml(false));

                    } else {
                        // TODO: update video element
                    }
                }
            } else if (showNewEditor) {
                // TODO implement visual editor implementation to update image/video in post
            } else {
                // TODO implement legacy editor implementation to update image/video in post
            }
        }

        return post;
    }
}
