package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.Helpers;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_GALLERY_LINK_TO;

public class GalleryBlockProcessor extends BlockProcessor {
    private String mAttachmentPageUrl;

    /**
     * Query selector for selecting the img element from gallery which needs processing
     */
    private String mGalleryImageQuerySelector;

    /**
     * A {@link Pattern} to match gallery blocks with the following capture groups:
     *
     * <ol>
     * <li>Block header before id</li>
     * <li>The mLocalId (to be replaced)</li>
     * <li>Block header after id</li>
     * <li>Block contents</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     */
    private Pattern mGalleryBlockPattern;

    public GalleryBlockProcessor(String localId, MediaFile mediaFile, String siteUrl) {
        super(localId, mediaFile);
        mGalleryBlockPattern = Helpers.getGalleryBlockPattern(localId);
        mGalleryImageQuerySelector = Helpers.getGalleryImgSelector(localId);
        mAttachmentPageUrl = mediaFile.getAttachmentPageURL(siteUrl);
    }

    @Override public String processBlock(String block) {
        Matcher matcher = mGalleryBlockPattern.matcher(block);

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
                Matcher linkToMatcher = PATTERN_GALLERY_LINK_TO.matcher(headerComment);

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
