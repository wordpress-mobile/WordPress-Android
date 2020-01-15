package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaUploadCompletionProcessorPatterns {
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

    // TODO: these patterns can be DRYed up after implementing JSON handling for the block header
    /**
     * Template pattern used to match and splice image blocks
     */
    private static final String PATTERN_TEMPLATE_IMAGE = "(<!-- wp:image \\{[^\\}]*\"id\":)" // block
                                                         + "(%1$s)" // local id must match to be replaced
                                                         + "([,\\]][^>]*-->\n?)" // rest of header
                                                         + "(.*)" // block contents
                                                         + "(<!-- /wp:image -->\n?)"; // closing comment

    /**
     * Template pattern used to match and splice video blocks
     */
    private static final String PATTERN_TEMPLATE_VIDEO = "(<!-- wp:video \\{[^\\}]*\"id\":)" // block
                                                         + "(%1$s)" // local id must match to be replaced
                                                         + "([,\\}][^>]*-->\n?)" // rest of header
                                                         + "(.*)" // block contents
                                                         + "(<!-- /wp:video -->\n?)"; // closing comment

    /**
     * Template pattern used to match and splice media-text blocks
     */
    private static final String PATTERN_TEMPLATE_MEDIA_TEXT = "(<!-- wp:media-text \\{[^\\}]*\"mediaId\":)" // block
                                                              + "(%1$s)" // local id must match to be replaced
                                                              + "([,\\}][^>]*-->\n?)" // rest of header
                                                              + "(.*)" // block contents
                                                              + "(<!-- /wp:media-text -->\n?)"; // closing comment

    /**
     * Template pattern used to match and splice gallery blocks
     */
    private static final String PATTERN_TEMPLATE_GALLERY = "(<!-- wp:gallery \\{[^\\}]*\"ids\":\\[(?:\"?\\d+\"?,)*)"
                                                           + "(\"?%1$s\"?)" // local id must match to be replaced
                                                           + "([,\\]][^>]*-->\n?)" // rest of header
                                                           + "(.*)" // block contents
                                                           + "(<!-- /wp:gallery -->\n?)"; // closing comment


    /**
     * A {@link Pattern} to match and capture gallery linkTo property from block header
     *
     * <ol>
     *     <li>Block header before linkTo property</li>
     *     <li>The linkTo property</li>
     *     <li>Block header after linkTo property</li>
     * </ol>
     */
    public static final Pattern PATTERN_GALLERY_LINK_TO = Pattern.compile("(<!-- wp:gallery \\{[^\\}]*\"linkTo\":\")"
                                                           + "([^\"]*)" // linkTo value
                                                           + "([\"][^>]*-->\n?)"); // rest of header


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
         * Creates a pattern to extract image block contents and splice the header with a remote id
         *
         * @param localId the local image id to be matched
         * @return A {@link Pattern} with the following capture groups:
         *
         * <ol>
         *     <li>Block header before id</li>
         *     <li>The localId</li>
         *     <li>Block header after id</li>
         *     <li>Block contents</li>
         *     <li>Block closing comment and any following characters</li>
         * </ol>
         */
        public static Pattern getImageBlockPattern(String localId) {
            return Pattern.compile(String.format(PATTERN_TEMPLATE_IMAGE, localId), Pattern.DOTALL);
        }

        /**
         * Creates a pattern to extract video block contents and splice the header with a remote id
         *
         * @param localId the local image id to be matched
         * @return A {@link Pattern} with the following capture groups:
         *
         * <ol>
         *     <li>Block header before id</li>
         *     <li>The localId</li>
         *     <li>Block header after id</li>
         *     <li>Block contents</li>
         *     <li>Block closing comment and any following characters</li>
         * </ol>
         */
        public static Pattern getVideoBlockPattern(String localId) {
            return Pattern.compile(String.format(PATTERN_TEMPLATE_VIDEO, localId), Pattern.DOTALL);
        }

        /**
         * Creates a pattern to extract media-text block contents and splice the header with a remote id
         *
         * @param localId the local image id to be matched
         * @return A {@link Pattern} with the following capture groups:
         *
         * <ol>
         *     <li>Block header before id</li>
         *     <li>The localId</li>
         *     <li>Block header after id</li>
         *     <li>Block contents</li>
         *     <li>Block closing comment and any following characters</li>
         * </ol>
         */
        public static Pattern getMediaTextBlockPattern(String localId) {
            return Pattern.compile(String.format(PATTERN_TEMPLATE_MEDIA_TEXT, localId), Pattern.DOTALL);
        }

        /**
         * Creates a pattern to extract gallery block contents and splice the header with a remote id
         *
         * @param localId the local image id to be matched
         * @return A {@link Pattern} with the following capture groups:
         *
         * <ol>
         *     <li>Block header before id</li>
         *     <li>localId</li>
         *     <li>Block header after id</li>
         *     <li>Block contents</li>
         *     <li>Block closing comment and any following characters</li>
         * </ol>
         */
        public static Pattern getGalleryBlockPattern(String localId) {
            return Pattern.compile(String.format(PATTERN_TEMPLATE_GALLERY, localId), Pattern.DOTALL);
        }

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

        /**
         * @param localId The id to match the img element's data-id attribute
         * @return A query selector string
         */
        public static String getGalleryImgSelector(String localId) {
            return new StringBuilder()
                    .append("img[data-id=\"")
                    .append(localId)
                    .append("\"]")
                    .toString();
        }
    }
}
