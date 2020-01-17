package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;

public class ImageBlockProcessor extends BlockProcessor {
    /**
     * Template pattern used to match and splice image blocks
     */
    private static final String PATTERN_TEMPLATE_IMAGE = "(<!-- wp:image \\{[^\\}]*\"id\":)" // block
                                                         + "(%1$s)" // local id must match to be replaced
                                                         + "([,\\}][^>]*-->\n?)" // rest of header
                                                         + "(.*)" // block contents
                                                         + "(<!-- /wp:image -->\n?)"; // closing comment

    public ImageBlockProcessor(String localId, MediaFile mediaFile) {
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

            // if a match is found, proceed with replacement
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
            }
        }

        // leave block unchanged
        return block;
    }

    @Override String getBlockPatternTemplate() {
        return PATTERN_TEMPLATE_IMAGE;
    }
}
