package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import java.util.regex.Pattern;

public class MediaUploadCompletionProcessorPatterns {
    public static final String PATTERN_BLOCK_PREFIX = "<!-- wp:(";

    /**
     * A {@link Pattern} to match headers for Gutenberg media-containing blocks with a capture group for the block type
     */
    public static final Pattern PATTERN_BLOCK_HEADER = Pattern.compile(new StringBuilder()
            .append(PATTERN_BLOCK_PREFIX)
            .append(MediaBlockType.getMatchingGroup())
            .append(").*? -->\n?")
            .toString(), Pattern.DOTALL);

    /**
     * A pattern template to match the block boundaries of a specific Gutenberg block type with a capture group to
     * identify the match as either the beginning or end of a block: group(1) == "/" for the end of a block
     */
    public static final String PATTERN_TEMPLATE_BLOCK_BOUNDARY = "<!-- (/?)wp:%1$s.*? -->\n?";

    /**
     * A {@link Pattern} to match Gutenberg media-containing blocks with the following capture groups:
     *
     * <ol>
     * <li>Block type</li>
     * <li>Block json attributes</li>
     * <li>Block html content</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     *
     */
    public static final Pattern PATTERN_BLOCK_CAPTURES = Pattern.compile(new StringBuilder()
            .append(PATTERN_BLOCK_PREFIX) // start-of-group: block type
            .append(MediaBlockType.getMatchingGroup())
            .append(")") // end-of-group: block type
            .append(" (\\{.*?\\}) -->\n?") // group: block header json
            .append("(.*)") // group: html content
            .append("(<!-- /wp:\\1 -->.*)") // group: closing-comment (name must match group 1: block type)
            .toString(), Pattern.DOTALL);
}
