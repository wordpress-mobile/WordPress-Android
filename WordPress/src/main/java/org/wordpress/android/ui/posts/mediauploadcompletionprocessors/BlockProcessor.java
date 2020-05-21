package org.wordpress.android.ui.posts.mediauploadcompletionprocessors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.wordpress.android.editor.Utils;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.regex.Matcher;

import static org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK_CAPTURES;

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

    private String mBlockName;
    private JsonObject mJsonAttributes;
    private Document mBlockContentDocument;
    private String mClosingComment;


    /**
     * @param localId The local media id that needs replacement
     * @param mediaFile The mediaFile containing the remote id and remote url
     */
    BlockProcessor(String localId, MediaFile mediaFile) {
        mLocalId = localId;
        mRemoteId = mediaFile.getMediaId();
        mRemoteUrl = org.wordpress.android.util.StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.getFileURL()));
    }

    private JsonObject parseJson(String blockJson) {
        JsonParser parser = new JsonParser();
        return parser.parse(blockJson).getAsJsonObject();
    }

    private Document parseHTML(String blockContent) {
        // create document from block content
        Document document = Jsoup.parse(blockContent);
        document.outputSettings(OUTPUT_SETTINGS);
        return document;
    }

    private boolean splitBlock(String block) {
        Matcher captures = PATTERN_BLOCK_CAPTURES.matcher(block);

        boolean capturesFound = captures.find();

        if (capturesFound) {
            mBlockName = captures.group(1);
            mJsonAttributes = parseJson(captures.group(2));
            mBlockContentDocument = parseHTML(captures.group(3));
            mClosingComment = captures.group(4);
            return true;
        } else {
            mBlockName = null;
            mJsonAttributes = null;
            mBlockContentDocument = null;
            mClosingComment = null;
            return false;
        }
    }

    /**
     * Processes a block returning a raw content replacement string. If a match is not found for the block content, this
     * method should return the original block contents unchanged.
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    String processBlock(String block) {
        if (splitBlock(block)) {
            if (processBlockJsonAttributes(mJsonAttributes)) {
                if (processBlockContentDocument(mBlockContentDocument)) {
                    // return injected block
                    return new StringBuilder()
                            .append("<!-- wp:")
                            .append(mBlockName)
                            .append(" ")
                            .append(mJsonAttributes) // json parser output
                            .append(" -->\n")
                            .append(mBlockContentDocument.body().html()) // HTML parser output
                            .append(mClosingComment)
                            .toString();
                }
            } else {
                return processInnerBlock(block); // delegate to inner blocks if needed
            }
        }
        // leave block unchanged
        return block;
    }

    /**
     * All concrete implementations must implement this method for the particular block type. The document represents
     * the html contents of the block to be processed, and is to be mutated in place.<br>
     * <br>
     * This method should return true to indicate success. Returning false will result in the block contents being
     * unmodified.
     *
     * @param document The document to be mutated to make the necessary replacements
     * @return A boolean value indicating whether or not the block contents should be replaced
     */
    abstract boolean processBlockContentDocument(Document document);

    /**
     * All concrete implementations must implement this method for the particular block type. The jsonAttributes object
     * is a {@link JsonObject} parsed from the block header attributes. This object can be used to check for a match,
     * and can be directly mutated if necessary.<br>
     * <br>
     * This method should return true to indicate success. Returning false will result in the block contents being
     * unmodified.
     *
     * @param jsonAttributes the attributes object used to check for a match with the local id, and mutated if necessary
     * @return
     */
    abstract boolean processBlockJsonAttributes(JsonObject jsonAttributes);

    /**
     * This method can be optionally overriden by concrete implementations to delegate further processing via recursion
     * when {@link BlockProcessor#processBlockJsonAttributes(JsonObject)} returns false (i.e. the block did not match
     * the local id being replaced). This is useful for implementing mutual recursion with
     * {@link MediaUploadCompletionProcessor#processContent(String)} for block types that have media-containing blocks
     * within their inner content.<br>
     * <br>
     * The default implementation provided is a NOOP that leaves the content of the block unchanged.
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    String processInnerBlock(String block) {
        return block;
    }
}
