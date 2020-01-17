package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

public class VideoBlockProcessor extends BlockProcessor {
    /**
     * Template pattern used to match and splice video blocks
     */
    private static final String PATTERN_TEMPLATE_VIDEO = "(<!-- wp:video \\{[^\\}]*\"id\":)" // block
                                                         + "(%1$s)" // local id must match to be replaced
                                                         + "([,\\}][^>]*-->\n?)" // rest of header
                                                         + "(.*)" // block contents
                                                         + "(<!-- /wp:video -->\n?)"; // closing comment

    public VideoBlockProcessor(String localId, MediaFile mediaFile) {
        super(localId, mediaFile);
    }

    @Override String getBlockPatternTemplate() {
        return PATTERN_TEMPLATE_VIDEO;
    }

    @Override boolean processBlockContentDocument(Document document) {
        // select video element with our local id
        Element targetVideo = document.select("video").first();

        // if a match is found for video, proceed with replacement
        if (targetVideo != null) {
            // replace attribute
            targetVideo.attr("src", mRemoteUrl);

            // return injected block
            return true;
        }

        return false;
    }
}
