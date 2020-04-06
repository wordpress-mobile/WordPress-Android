package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK_HEADER;
import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_TEMPLATE_BLOCK_BOUNDARY;

public class MediaUploadCompletionProcessor {
    private final BlockProcessorFactory mBlockProcessorFactory;

    /**
     * Processor used for replacing local media id(s) and url(s) with their remote counterparts after an upload has
     * completed.
     *
     * @param localId The local media id that needs replacement
     * @param mediaFile The mediaFile containing the remote id and remote url
     * @param siteUrl The site url - used to generate the attachmentPage url
     */
    public MediaUploadCompletionProcessor(String localId, MediaFile mediaFile, String siteUrl) {
        mBlockProcessorFactory = new BlockProcessorFactory(this)
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
        Matcher headerMatcher = PATTERN_BLOCK_HEADER.matcher(postContent);

        int positionBlockStart, positionBlockEnd = 0;

        if (headerMatcher.find()) {
            positionBlockStart = headerMatcher.start();
            String blockType = headerMatcher.group(1);
            Matcher blockBoundaryMatcher = Pattern.compile(String.format(PATTERN_TEMPLATE_BLOCK_BOUNDARY, blockType),
                    Pattern.DOTALL).matcher(postContent.substring(headerMatcher.end()));

            int nestLevel = 1;

            while (0 < nestLevel && blockBoundaryMatcher.find()) {
                if (blockBoundaryMatcher.group(1).equals("/")) {
                    positionBlockEnd = headerMatcher.end() + blockBoundaryMatcher.end();
                    nestLevel--;
                } else {
                    nestLevel++;
                }
            }

            return new StringBuilder()
                    .append(postContent.substring(0, positionBlockStart))
                    .append(processBlock(postContent.substring(positionBlockStart, positionBlockEnd)))
                    .append(processPost(postContent.substring(positionBlockEnd)))
                    .toString();
        } else {
            return postContent;
        }
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
