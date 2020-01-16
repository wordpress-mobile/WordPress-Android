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
}
