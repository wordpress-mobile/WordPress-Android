package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.Helpers;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_GALLERY_LINK_TO;

public class MediaUploadCompletionProcessor {
    private String mLocalId;
    private String mRemoteId;
    private String mRemoteUrl;
    private String mAttachmentPageUrl;

    /**
     * HTML output used by the parser
     */
    @SuppressWarnings("checkstyle:LineLength") private static final OutputSettings OUTPUT_SETTINGS = new OutputSettings()
            .outline(false)
//          .syntax(Syntax.xml)
//            Do we want xml or html here (e.g. self closing tags, boolean attributes)?
//            https://stackoverflow.com/questions/26584974/keeping-html-boolean-attributes-in-their-original-form-when-parsing-with-jsoup
            .prettyPrint(false);

    private final ImageBlockProcessor mImageBlockProcessor;
    private final VideoBlockProcessor mVideoBlockProcessor;
    private final MediaTextBlockProcessor mMediaTextBlockProcessor;
    private final GalleryBlockProcessor mGalleryBlockProcessor;

    public MediaUploadCompletionProcessor(String localId, MediaFile mediaFile, String siteUrl) {
        mLocalId = localId;
        mRemoteId = mediaFile.getMediaId();
        mRemoteUrl = org.wordpress.android.util.StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));

        mImageBlockProcessor = new ImageBlockProcessor(localId, mediaFile);

        mVideoBlockProcessor = new VideoBlockProcessor(localId, mediaFile);

        mMediaTextBlockProcessor = new MediaTextBlockProcessor(localId, mediaFile);

        mGalleryBlockProcessor = new GalleryBlockProcessor(localId, mediaFile, siteUrl);
        mAttachmentPageUrl = mediaFile.getAttachmentPageURL(siteUrl);
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
        StringBuilder result = new StringBuilder();

        int position = 0;

        while (matcher.find()) {
            result.append(postContent.substring(position, matcher.start()));
            result.append(processBlock(matcher.group()));
            position = matcher.end();
        }

        result.append(postContent.substring(position));

        return result.toString();
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
                return mImageBlockProcessor.processBlock(block);
            case VIDEO:
                return mVideoBlockProcessor.processBlock(block);
            case MEDIA_TEXT:
                return mMediaTextBlockProcessor.processBlock(block);
            case GALLERY:
                return mGalleryBlockProcessor.processBlock(block);
            default:
                return block;
        }
    }

}

