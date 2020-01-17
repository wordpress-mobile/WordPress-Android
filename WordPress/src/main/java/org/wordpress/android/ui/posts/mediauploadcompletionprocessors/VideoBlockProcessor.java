package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;

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
        Matcher matcher = getMatcherForBlock(block);

        if (matcher.find()) {
            String headerComment = new StringBuilder()
                    .append(matcher.group(1))
                    .append(mRemoteId) // here we substitute remote id in place of the local id
                    .append(matcher.group(3))
                    .toString();
            String blockContent = matcher.group(4);
            String closingComment = matcher.group(5);

            // create document from block content
            Document document = Jsoup.parse(blockContent);
            document.outputSettings(OUTPUT_SETTINGS);

            // select video element with our local id
            Element targetVideo = document.select("video").first();

            // if a match is found for video, proceed with replacement
            if (targetVideo != null) {
                // replace attribute
                targetVideo.attr("src", mRemoteUrl);

                // return injected block
                return new StringBuilder()
                        .append(headerComment)
                        .append(document.body().html()) // parser output
                        .append(closingComment)
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

