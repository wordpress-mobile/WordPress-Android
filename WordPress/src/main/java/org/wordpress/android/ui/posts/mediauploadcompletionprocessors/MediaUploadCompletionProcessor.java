package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.apache.commons.lang3.StringUtils;
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.Helpers;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.Arrays;
import java.util.regex.Matcher;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK;

public class MediaUploadCompletionProcessor {
    enum MediaBlockType {
        IMAGE("image"),
        VIDEO("video"),
        MEDIA_TEXT("media-text"),
        GALLERY("gallery");

        private final String mName;

        MediaBlockType(String name) {
            mName = name;
        }

        public String toString() {
            return mName;
        }

        public static MediaBlockType fromString(String blockType) {
            for (MediaBlockType mediaBlockType : MediaBlockType.values()) {
                if (mediaBlockType.mName.equals(blockType)) {
                    return mediaBlockType;
                }
            }
            return null;
        }

        static String getMatchingGroup() {
            return StringUtils.join(Arrays.asList(MediaBlockType.values()), "|");
        }
    }

    private final ImageBlockProcessor mImageBlockProcessor;
    private final VideoBlockProcessor mVideoBlockProcessor;
    private final MediaTextBlockProcessor mMediaTextBlockProcessor;
    private final GalleryBlockProcessor mGalleryBlockProcessor;

    public MediaUploadCompletionProcessor(String localId, MediaFile mediaFile, String siteUrl) {
        mImageBlockProcessor = new ImageBlockProcessor(localId, mediaFile);
        mVideoBlockProcessor = new VideoBlockProcessor(localId, mediaFile);
        mMediaTextBlockProcessor = new MediaTextBlockProcessor(localId, mediaFile);
        mGalleryBlockProcessor = new GalleryBlockProcessor(localId, mediaFile, siteUrl);
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

