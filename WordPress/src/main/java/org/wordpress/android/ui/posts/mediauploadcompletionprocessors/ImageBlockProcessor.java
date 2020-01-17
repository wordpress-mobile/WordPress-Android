package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

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
        if (matchAndSpliceBlockHeader(block)) {
            // create document from block content
            Document document = Jsoup.parse(getBlockContent());
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
        return PATTERN_TEMPLATE_IMAGE;
    }
}
