package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

public class MediaTextBlockProcessor extends BlockProcessor {
    /**
     * Template pattern used to match and splice media-text blocks
     */
    private static final String PATTERN_TEMPLATE_MEDIA_TEXT = "(<!-- wp:media-text \\{[^\\}]*\"mediaId\":)" // block
                                                              + "(%1$s)" // local id must match to be replaced
                                                              + "([,\\}][^>]*-->\n?)" // rest of header
                                                              + "(.*)" // block contents
                                                              + "(<!-- /wp:media-text -->\n?)"; // closing comment

    public MediaTextBlockProcessor(String localId, MediaFile mediaFile) {
        super(localId, mediaFile);
    }

    @Override String getBlockPatternTemplate() {
        return PATTERN_TEMPLATE_MEDIA_TEXT;
    }

    @Override boolean processBlockContentDocument(Document document) {
        // select image element with our local id
        Element targetImg = document.select("img").first();

        // if a match is found for img, proceed with replacement
        if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", mRemoteUrl);

            // replace class
            targetImg.removeClass("wp-image-" + mLocalId);
            targetImg.addClass("wp-image-" + mRemoteId);

            // return injected block
            return true;
        } else { // try video
            // select video element with our local id
            Element targetVideo = document.select("video").first();

            // if a match is found for video, proceed with replacement
            if (targetVideo != null) {
                // replace attribute
                targetVideo.attr("src", mRemoteUrl);

                // return injected block
                return true;
            }
        }

        return false;
    }
}
