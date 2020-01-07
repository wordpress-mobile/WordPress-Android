package org.wordpress.android.ui.posts;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.ui.posts.MediaUploadCompletionProcessorPatterns.Helpers;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wordpress.android.ui.posts.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK;
import static org.wordpress.android.ui.posts.MediaUploadCompletionProcessorPatterns.PATTERN_GALLERY_LINK_TO;

public class MediaUploadCompletionProcessor {
    private String mLocalId;
    private String mRemoteId;
    private String mRemoteUrl;
    private String mAttachmentPageUrl;

    /**
     * Query selector for selecting the img element from gallery which needs processing
     */
    private String mGalleryImageQuerySelector;

    /**
     * A {@link Pattern} to match image blocks with the following capture groups:
     *
     * <ol>
     * <li>Block header before id</li>
     * <li>The mLocalId (to be replaced)</li>
     * <li>Block header after id</li>
     * <li>Block contents</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     */
    private Pattern mImageBlockPattern;
    /**
     * A {@link Pattern} to match media-text blocks with the following capture groups:
     *
     * <ol>
     * <li>Block header before id</li>
     * <li>The mLocalId (to be replaced)</li>
     * <li>Block header after id</li>
     * <li>Block contents</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     */
    private Pattern mVideoBlockPattern;
    /**
     * A {@link Pattern} to match media-text blocks with the following capture groups:
     *
     * <ol>
     * <li>Block header before id</li>
     * <li>The mLocalId (to be replaced)</li>
     * <li>Block header after id</li>
     * <li>Block contents</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     */
    private Pattern mMediaTextBlockPattern;
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

    /**
     * HTML output used by the parser
     */
    @SuppressWarnings("checkstyle:LineLength") private static final OutputSettings OUTPUT_SETTINGS = new OutputSettings()
            .outline(false)
//          .syntax(Syntax.xml)
//            Do we want xml or html here (e.g. self closing tags, boolean attributes)?
//            https://stackoverflow.com/questions/26584974/keeping-html-boolean-attributes-in-their-original-form-when-parsing-with-jsoup
            .prettyPrint(false);

    MediaUploadCompletionProcessor(String localId, MediaFile mediaFile, String siteUrl) {
        mLocalId = localId;
        mRemoteId = mediaFile.getMediaId();
        mRemoteUrl = org.wordpress.android.util.StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));
        mAttachmentPageUrl = mediaFile.getAttachmentPageURL(siteUrl);
        mImageBlockPattern = Helpers.getImageBlockPattern(mLocalId);
        mVideoBlockPattern = Helpers.getVideoBlockPattern(mLocalId);
        mMediaTextBlockPattern = Helpers.getMediaTextBlockPattern(mLocalId);
        mGalleryBlockPattern = Helpers.getGalleryBlockPattern(localId);
        mGalleryImageQuerySelector = Helpers.getGalleryImgSelector(mLocalId);
    }

    /**
     * Processes a post to replace the local ids and local urls of media with remote ids and remote urls. This matches
     * media-containing blocks and delegates further processing to {@link #processBlock(String)}
     *
     * @param postContent The post content to be processed
     * @return A string containing the processed post, or the original content if no match was found
     */
    public String processPost(String postContent) {
        Matcher matcher = PATTERN_BLOCK.matcher(postContent);
        ArrayList<String> chunks = new ArrayList<>();

        int position = 0;

        while (matcher.find()) {
            chunks.add(postContent.substring(position, matcher.start()));
            chunks.add(processBlock(matcher.group()));
            position = matcher.end();
        }

        chunks.add(postContent.substring(position));

        return StringUtils.join(chunks, "");
    }


    /**
     * Processes a media block returning a raw content replacement string
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    private String processBlock(String block) {
        switch (Helpers.detectBlockType(block)) {
            case IMAGE:
                return processImageBlock(block);
            case VIDEO:
                return processVideoBlock(block);
            case MEDIA_TEXT:
                return processMediaTextBlock(block);
            case GALLERY:
                return processGalleryBlock(block);
            default:
                return block;
        }
    }

    /**
     * Processes an image block returning a raw content replacement string
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    public String processImageBlock(String block) {
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

    /**
     * Processes a video block returning a raw content replacement string
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    public String processVideoBlock(String block) {
        // TODO: process block header JSON in a more robust way (current processing uses RexEx)
        Matcher matcher = mVideoBlockPattern.matcher(block);

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

    /**
     * Processes a media-text block returning a raw content replacement string
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    public String processMediaTextBlock(String block) {
        Matcher matcher = mMediaTextBlockPattern.matcher(block);

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

    /**
     * Processes a gallery block returning a raw content replacement string
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    public String processGalleryBlock(String block) {
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

