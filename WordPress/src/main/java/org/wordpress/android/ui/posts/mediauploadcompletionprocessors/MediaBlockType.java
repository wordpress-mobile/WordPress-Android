package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK_PREFIX;

enum MediaBlockType {
    IMAGE("image"),
    VIDEO("video"),
    MEDIA_TEXT("media-text"),
    GALLERY("gallery"),
    COVER("cover");

    private static final Map<String, MediaBlockType> MAP = new HashMap<>();
    private static final String MATCHING_GROUP;
    private static final Pattern PATTERN_MEDIA_BLOCK_TYPES;

    static {
        for (MediaBlockType type : values()) {
            MAP.put(type.mName, type);
        }

        MATCHING_GROUP = StringUtils.join(Arrays.asList(MediaBlockType.values()), "|");

        PATTERN_MEDIA_BLOCK_TYPES = Pattern.compile(new StringBuilder()
                .append(PATTERN_BLOCK_PREFIX)
                .append(MATCHING_GROUP)
                .append(")")
                .toString());
    }

    private final String mName;

    MediaBlockType(String name) {
        mName = name;
    }

    public String toString() {
        return mName;
    }

    static MediaBlockType fromString(String blockType) {
        return MAP.get(blockType);
    }

    /**
     * @return A string with the enumerated media block types separated by the pipe character (useful for creating a
     * regex capturing group pattern)
     */
    static String getMatchingGroup() {
        return MATCHING_GROUP;
    }

    /**
     * Detects the media block type from the raw block contents
     *
     * @param block The raw block contents
     * @return The media block type or null if no match is found
     */
    static MediaBlockType detectBlockType(String block) {
        Matcher matcher = PATTERN_MEDIA_BLOCK_TYPES.matcher(block);

        if (matcher.find()) {
            return MediaBlockType.fromString(matcher.group(1));
        }

        return null;
    }
}
