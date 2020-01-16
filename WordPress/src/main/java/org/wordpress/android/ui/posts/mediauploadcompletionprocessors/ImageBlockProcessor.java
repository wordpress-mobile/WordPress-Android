package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageBlockProcessor extends BlockProcessor {
    /**
     * Template pattern used to match and splice image blocks
     */
    private static final String PATTERN_TEMPLATE_IMAGE = "(<!-- wp:image \\{[^\\}]*\"id\":)" // block
                                                         + "(%1$s)" // local id must match to be replaced
                                                         + "([,\\]][^>]*-->\n?)" // rest of header
                                                         + "(.*)" // block contents
                                                         + "(<!-- /wp:image -->\n?)"; // closing comment

    /**
     * A {@link Pattern} to extract image block contents and splice the header with a remote id. The pattern has the
     * following capture groups:
     *
     * <ol>
     * <li>Block header before id</li>
     * <li>The localId (to be replaced)</li>
     * <li>Block header after id</li>
     * <li>Block contents</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     */
    private Pattern mImageBlockPattern;

    public ImageBlockProcessor(String localId, MediaFile mediaFile) {
        super(localId, mediaFile);
        mImageBlockPattern = Pattern.compile(String.format(PATTERN_TEMPLATE_IMAGE, localId), Pattern.DOTALL);
    }

    @Override public String processBlock(String block) {
        Matcher matcher = mImageBlockPattern.matcher(block);

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
}
