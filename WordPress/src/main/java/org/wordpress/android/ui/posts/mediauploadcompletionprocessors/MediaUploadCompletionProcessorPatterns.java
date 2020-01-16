package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessor.MediaBlockType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaUploadCompletionProcessorPatterns {
    // TODO: these patterns can be DRYed up after implementing JSON handling for the block header
    private static final String PATTERN_BLOCK_PREFIX = "<!-- wp:(";
    private static final String PATTERN_BLOCK_SUFFIX = "<!-- /wp:\\1 -->";

    /**
     * A {@link Pattern} to match Gutenberg media-containing blocks with a capture group for the block type
     */
    public static final Pattern PATTERN_BLOCK = Pattern.compile(new StringBuilder()
            .append(PATTERN_BLOCK_PREFIX)
            .append(MediaBlockType.getMatchingGroup())
            .append(").*?")
            .append(PATTERN_BLOCK_SUFFIX)
            .toString(), Pattern.DOTALL);

    /**
     * Helper class with methods to generate patterns for extracting and splicing text from raw block contents
     */
    static class Helpers {
        /**
         * Detects the media block type from the raw block contents
         *
         * @param block The raw block contents
         * @return The media block type or null if no match is found
         */
        public static MediaBlockType detectBlockType(String block) {
            final Pattern pattern = Pattern.compile(new StringBuilder()
                    .append(PATTERN_BLOCK_PREFIX)
                    .append(MediaBlockType.getMatchingGroup())
                    .append(")")
                    .toString());
            Matcher matcher = pattern.matcher(block);

            if (matcher.find()) {
                return MediaBlockType.fromString(matcher.group(1));
            }

            return null;
        }
    }
}
