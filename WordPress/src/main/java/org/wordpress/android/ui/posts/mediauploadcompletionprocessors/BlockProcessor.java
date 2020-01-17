package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import org.jsoup.nodes.Document.OutputSettings;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract class to be extended for each enumerated {@link MediaBlockType}.
 */
abstract class BlockProcessor {
    /**
     * HTML output used by the parser
     */
    @SuppressWarnings("checkstyle:LineLength") static final OutputSettings OUTPUT_SETTINGS = new OutputSettings()
            .outline(false)
//          .syntax(Syntax.xml)
//            Do we want xml or html here (e.g. self closing tags, boolean attributes)?
//            https://stackoverflow.com/questions/26584974/keeping-html-boolean-attributes-in-their-original-form-when-parsing-with-jsoup
            .prettyPrint(false);

    String mLocalId;
    String mRemoteId;
    String mRemoteUrl;

    private Pattern mBlockPattern;
    private String mHeaderComment;
    private String mBlockContent;
    private String mClosingComment;

    /**
     * @param localId The local media id that needs replacement
     * @param mediaFile The mediaFile containing the remote id and remote url
     */
    BlockProcessor(String localId, MediaFile mediaFile) {
        mLocalId = localId;
        mRemoteId = mediaFile.getMediaId();
        mRemoteUrl = org.wordpress.android.util.StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));
        mBlockPattern = Pattern.compile(String.format(getBlockPatternTemplate(), localId), Pattern.DOTALL);
    }

    // TODO: consider processing block header JSON in a more robust way (current processing uses RexEx)
    /**
     * @param block The raw block contents of the block to be matched
     * @return A {@link Matcher} to extract block contents and splice the header with a remote id. The matcher has the
     * following capture groups:
     *
     * <ol>
     * <li>Block header before id</li>
     * <li>The localId (to be replaced)</li>
     * <li>Block header after id</li>
     * <li>Block contents</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     */
    Matcher getMatcherForBlock(String block) {
        return mBlockPattern.matcher(block);
    }

    boolean matchAndSpliceBlockHeader(String block) {
        Matcher matcher = getMatcherForBlock(block);

        boolean matchFound = matcher.find();

        if (matchFound) {
            mHeaderComment = new StringBuilder()
                    .append(matcher.group(1))
                    .append(mRemoteId) // here we substitute remote id in place of the local id
                    .append(matcher.group(3))
                    .toString();
            mBlockContent = matcher.group(4);
            mClosingComment = matcher.group(5);
        } else {
            mHeaderComment = null;
            mBlockContent = null;
            mClosingComment = null;
        }

        return matchFound;
    }

    String getHeaderComment() {
        return mHeaderComment;
    }

    String getBlockContent() {
        return mBlockContent;
    }

    String getClosingComment() {
        return mClosingComment;
    }

    /**
     * Processes a block returning a raw content replacement string. If a match is not found for the block content, this
     * method should return the original block contents unchanged.
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    abstract String processBlock(String block);

    /**
     * All concrete implementations must implement this method to return a regex pattern template for the particular
     * block type.<br>
     * <br>
     * The pattern template should contain a format specifier for the local id that needs to be matched and
     * replaced in the block header, and the format specifier should be within its own capture group, e.g. `(%1$s)`.<br>
     * <br>
     * The pattern template should result in a matcher with the following capture groups:
     *
     * <ol>
     * <li>Block header before id</li>
     * <li>The format specifier for the local id (to be replaced by the local id when generating the pattern)</li>
     * <li>Block header after id</li>
     * <li>Block contents</li>
     * <li>Block closing comment and any following characters</li>
     * </ol>
     *
     * @return String with the regex pattern template
     */
    abstract String getBlockPatternTemplate();
}
