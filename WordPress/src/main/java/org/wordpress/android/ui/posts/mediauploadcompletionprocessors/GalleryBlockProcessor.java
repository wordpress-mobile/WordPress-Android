package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryBlockProcessor extends BlockProcessor {
    /**
     * Template pattern used to match and splice gallery blocks
     */
    private static final String PATTERN_TEMPLATE_GALLERY = "(<!-- wp:gallery \\{[^\\}]*\"ids\":\\[(?:\"?\\d+\"?,)*)"
                                                           + "(\"?%1$s\"?)" // local id must match to be replaced
                                                           + "([,\\]][^>]*-->\n?)" // rest of header
                                                           + "(.*)" // block contents
                                                           + "(<!-- /wp:gallery -->\n?)"; // closing comment


    /**
     * A {@link Pattern} to match and capture gallery linkTo property from block header
     *
     * <ol>
     *     <li>Block header before linkTo property</li>
     *     <li>The linkTo property</li>
     *     <li>Block header after linkTo property</li>
     * </ol>
     */
    public static final Pattern PATTERN_GALLERY_LINK_TO = Pattern.compile("(<!-- wp:gallery \\{[^\\}]*\"linkTo\":\")"
                                                                          + "([^\"]*)" // linkTo value
                                                                          + "([\"][^>]*-->\n?)"); // rest of header


    private String mAttachmentPageUrl;

    /**
     * Query selector for selecting the img element from gallery which needs processing
     */
    private String mGalleryImageQuerySelector;

    public GalleryBlockProcessor(String localId, MediaFile mediaFile, String siteUrl) {
        super(localId, mediaFile);
        mGalleryImageQuerySelector = new StringBuilder()
                .append("img[data-id=\"")
                .append(localId)
                .append("\"]")
                .toString();
        mAttachmentPageUrl = mediaFile.getAttachmentPageURL(siteUrl);
    }

    @Override public String processBlock(String block) {
        if (matchAndSpliceBlockHeader(block)) {
            // create document from block content
            Document document = Jsoup.parse(getBlockContent());
            document.outputSettings(OUTPUT_SETTINGS);

            // select image element with our local id
            Element targetImg = document.select(mGalleryImageQuerySelector).first();

            // if a match is found, proceed with replacement
            if (targetImg != null) {
                // replace attributes
                targetImg.attr("src", mRemoteUrl);
                targetImg.attr("data-id", mRemoteId);
                targetImg.attr("data-full-url", mRemoteUrl);
                targetImg.attr("data-link", mAttachmentPageUrl);

                // replace class
                targetImg.removeClass("wp-image-" + mLocalId);
                targetImg.addClass("wp-image-" + mRemoteId);

                // check for linkTo property
                Matcher linkToMatcher = PATTERN_GALLERY_LINK_TO.matcher(getHeaderComment());

                // set parent anchor href if necessary
                Element parent = targetImg.parent();
                if (parent != null && parent.is("a") && linkToMatcher.find()) {
                    String linkToValue = linkToMatcher.group(2);

                    switch (linkToValue) {
                        case "media":
                            parent.attr("href", mRemoteUrl);
                            break;
                        case "attachment":
                            parent.attr("href", mAttachmentPageUrl);
                            break;
                        default:
                            return block;
                    }
                }

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
        return PATTERN_TEMPLATE_GALLERY;
    }
}
