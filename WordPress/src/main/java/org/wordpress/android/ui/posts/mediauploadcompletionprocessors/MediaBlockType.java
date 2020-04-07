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

    static {
        for (MediaBlockType type : values()) {
            MAP.put(type.mName, type);
        }
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
        return StringUtils.join(Arrays.asList(MediaBlockType.values()), "|");
    }

    /**
     * Detects the media block type from the raw block contents
     *
     * @param block The raw block contents
     * @return The media block type or null if no match is found
     */
    static MediaBlockType detectBlockType(String block) {
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
