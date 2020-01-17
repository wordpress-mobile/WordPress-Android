package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;

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
                return new StringBuilder()
                        .append(headerComment)
                        .append(document.body().html()) // parser output
                        .append(closingComment)
                        .toString();
            } else { // try video
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
        }

        // leave block unchanged
        return block;
    }

    @Override String getBlockPatternTemplate() {
        return PATTERN_TEMPLATE_MEDIA_TEXT;
    }
}
