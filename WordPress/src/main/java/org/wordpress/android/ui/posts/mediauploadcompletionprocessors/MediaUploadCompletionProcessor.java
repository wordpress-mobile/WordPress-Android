package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK;

public class MediaUploadCompletionProcessor {
    private final BlockProcessorFactory mBlockProcessorFactory;

    public MediaUploadCompletionProcessor(String localId, MediaFile mediaFile, String siteUrl) {
        mBlockProcessorFactory = new BlockProcessorFactory()
                .init(localId, mediaFile, siteUrl);
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
        final MediaBlockType blockType = MediaBlockType.detectBlockType(block);
        final BlockProcessor blockProcessor = mBlockProcessorFactory.getProcessorForMediaBlockType(blockType);

        if (blockProcessor != null) {
            return blockProcessor.processBlock(block);
        }

        return block;
    }
}

