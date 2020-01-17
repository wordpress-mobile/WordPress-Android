package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
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

    @Override public String processBlock(String block) {
        if (matchAndSpliceBlockHeader(block)) {
            // create document from block content
            Document document = Jsoup.parse(getBlockContent());
            document.outputSettings(OUTPUT_SETTINGS);

            // select video element with our local id
            Element targetVideo = document.select("video").first();

            // if a match is found for video, proceed with replacement
            if (targetVideo != null) {
                // replace attribute
                targetVideo.attr("src", mRemoteUrl);

                // return injected block
                return new StringBuilder()
                        .append(getHeaderComment())
                        .append(document.body().html()) // parser output
                        .append(getClosingComment())
                        .toString();
            }
        }

        // leave block unchanged
        return block;
    }

    @Override String getBlockPatternTemplate() {
        return PATTERN_TEMPLATE_VIDEO;
    }
}

