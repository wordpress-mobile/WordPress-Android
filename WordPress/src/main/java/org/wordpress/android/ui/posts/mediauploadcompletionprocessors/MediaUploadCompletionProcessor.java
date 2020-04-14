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
     * Processes content to replace the local ids and local urls of media with remote ids and remote urls. This method
     * delineates block boundaries for media-containing blocks and delegates further processing via itself and / or
     * {@link #processBlock(String)}, via direct and mutual recursion, respectively.
     *
     * @param content The content to be processed
     * @return A string containing the processed content, or the original content if no match was found
     */
    public String processContent(String content) {
        Matcher headerMatcher = PATTERN_BLOCK_HEADER.matcher(content);

        int positionBlockStart, positionBlockEnd = content.length();

        if (headerMatcher.find()) {
            positionBlockStart = headerMatcher.start();
            String blockType = headerMatcher.group(1);
            Matcher blockBoundaryMatcher = Pattern.compile(String.format(PATTERN_TEMPLATE_BLOCK_BOUNDARY, blockType),
                    Pattern.DOTALL).matcher(content.substring(headerMatcher.end()));

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
                    .append(content.substring(0, positionBlockStart))
                    .append(processBlock(content.substring(positionBlockStart, positionBlockEnd)))
                    .append(processContent(content.substring(positionBlockEnd)))
                    .toString();
        } else {
            return content;
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
