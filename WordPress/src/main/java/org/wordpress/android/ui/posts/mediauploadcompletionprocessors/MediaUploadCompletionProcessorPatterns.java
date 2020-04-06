package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import java.util.regex.Pattern;

public class MediaUploadCompletionProcessorPatterns {
    // TODO: these patterns can be DRYed up after implementing JSON handling for the block header
    public static final String PATTERN_BLOCK_PREFIX = "<!-- wp:(";
    public static final String PATTERN_BLOCK_SUFFIX = "<!-- /wp:\\1 -->";

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
     * A {@link Pattern} to match headers for Gutenberg media-containing blocks with a capture group for the block type
     */
    public static final Pattern PATTERN_BLOCK_HEADER = Pattern.compile(new StringBuilder()
            .append(PATTERN_BLOCK_PREFIX)
            .append(MediaBlockType.getMatchingGroup())
            .append(").*? -->\n?")
            .toString(), Pattern.DOTALL);

    /**
     * A {@link Pattern} to match headers for Gutenberg media-containing blocks with a capture group for the block type
     */
    public static final String PATTERN_TEMPLATE_BLOCK_BOUNDARY = "<!-- (/?)wp:%1$s.*? -->\n?";


    /**
     * A {@link Pattern} to match Gutenberg media-containing blocks with a capture group for the block type
     *
     * <ol>
     * <li>Block type</li>
     * <li>Block header attributes json</li>
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
            .append("(.*\n?)") // group: html content
            .append("(") // start-of-group: closing-comment
            .append("<!-- /wp:\\1 -->\n?") // name must match group 1: block type
            .append(")") // end-of-group: closing-comment
            .toString(), Pattern.DOTALL);
}
