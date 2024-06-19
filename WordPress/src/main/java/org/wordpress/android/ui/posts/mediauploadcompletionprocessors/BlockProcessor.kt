package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.wordpress.android.editor.Utils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.helpers.MediaFile

/**
 * Abstract class to be extended for each enumerated [MediaBlockType].
 */
abstract class BlockProcessor internal constructor(@JvmField var localId: String, mediaFile: MediaFile) {
    @JvmField
    var remoteId: String = mediaFile.mediaId

    @JvmField
    var remoteUrl: String = StringUtils.notNullStr(Utils.escapeQuotes(mediaFile.optimalFileURL))
    var remoteGuid: String? = mediaFile.videoPressGuid

    private var blockName: String? = null
    private var jsonAttributes: JsonObject? = null
    private var blockContentDocument: Document? = null
    private var closingComment: String? = null

    private fun parseJson(blockJson: String) = JsonParser.parseString(blockJson).asJsonObject

    private fun parseHTML(blockContent: String): Document {
        // create document from block content
        val document = Jsoup.parse(blockContent)
        document.outputSettings(OUTPUT_SETTINGS)
        return document
    }

    private fun splitBlock(block: String, isSelfClosingTag: Boolean): Boolean {
        val pattern = if (isSelfClosingTag) {
            MediaUploadCompletionProcessorPatterns.PATTERN_SELF_CLOSING_BLOCK_CAPTURES
        } else {
            MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK_CAPTURES
        }
        val captures = pattern.matcher(block)

        val capturesFound = captures.find()

        return if (capturesFound) {
            blockName = captures.group(GROUP_BLOCK_NAME)
            jsonAttributes = captures.group(GROUP_JSON_ATTRIBUTES)?.let { parseJson(it) }
            blockContentDocument = if (isSelfClosingTag) {
                null
            } else {
                captures.group(GROUP_BLOCK_CONTENT_DOCUMENT)?.let { parseHTML(it) }
            }
            closingComment = if (isSelfClosingTag) null else captures.group(GROUP_CLOSING_COMMENT)
            true
        } else {
            blockName = null
            jsonAttributes = null
            blockContentDocument = null
            closingComment = null
            false
        }
    }

    /**
     * Processes a block returning a raw content replacement string. If a match is not found for the block content, this
     * method should return the original block contents unchanged.
     *
     * @param block The raw block contents
     * @param isSelfClosingTag True if the block tag is self-closing (e.g. )
     * @return A string containing content with ids and urls replaced
     */
    @JvmOverloads
    fun processBlock(block: String, isSelfClosingTag: Boolean = false) = when {
        !splitBlock(block, isSelfClosingTag) -> block // leave block unchanged
        jsonAttributes?.let { !processBlockJsonAttributes(it) } == true -> {
            // delegate to inner blocks if needed
            processInnerBlock(block)
        }
        isSelfClosingTag -> {
            // return injected block
            StringBuilder()
                .append("<!-- wp:")
                .append(blockName)
                .append(" ")
                .append(jsonAttributes) // json parser output
                .append(" /-->")
                .toString()
        }

        blockContentDocument?.let { processBlockContentDocument(it) } == true -> {
            // return injected block
            StringBuilder()
                .append("<!-- wp:")
                .append(blockName)
                .append(" ")
                .append(jsonAttributes) // json parser output
                .append(" -->\n")
                .append(blockContentDocument?.body()?.html()) // HTML parser output
                .append(closingComment)
                .toString()
        }

        else -> block // leave block unchanged
    }

    fun addIntPropertySafely(jsonAttributes: JsonObject, propertyName: String, value: String) = try {
        jsonAttributes.addProperty(propertyName, value.toInt())
    } catch (e: NumberFormatException) {
        AppLog.e(AppLog.T.MEDIA, e.message)
    }

    /**
     * All concrete implementations must implement this method for the particular block type. The document represents
     * the html contents of the block to be processed, and is to be mutated in place.<br></br>
     * <br></br>
     * This method should return true to indicate success. Returning false will result in the block contents being
     * unmodified.
     *
     * @param document The document to be mutated to make the necessary replacements
     * @return A boolean value indicating whether or not the block contents should be replaced
     */
    abstract fun processBlockContentDocument(document: Document): Boolean

    /**
     * All concrete implementations must implement this method for the particular block type. The jsonAttributes object
     * is a [JsonObject] parsed from the block header attributes. This object can be used to check for a match, and can
     * be directly mutated if necessary.<br></br>
     * <br></br>
     * This method should return true to indicate success. Returning false will result in the block contents being
     * unmodified.
     *
     * @param jsonAttributes the attributes object used to check for a match with the local id, and mutated if necessary
     * @return
     */
    abstract fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean

    /**
     * This method can be optionally overridden by concrete implementations to delegate further processing via recursion
     * when [BlockProcessor.processBlockJsonAttributes] returns false (i.e. the block did not match the local id being
     * replaced). This is useful for implementing mutual recursion with
     * [MediaUploadCompletionProcessor.processContent] for block types that have media-containing blocks within their
     * inner content.<br></br>
     * <br></br>
     * The default implementation provided is a NOOP that leaves the content of the block unchanged.
     *
     * @param block The raw block contents
     * @return A string containing content with ids and urls replaced
     */
    open fun processInnerBlock(block: String) = block

    companion object {
        /**
         * HTML output used by the parser
         */
        val OUTPUT_SETTINGS: Document.OutputSettings = Document.OutputSettings()
            .outline(false)
//          .syntax(Syntax.xml)
//          Do we want xml or html here (e.g. self closing tags, boolean attributes)?
//          https://stackoverflow.com/questions/26584974/keeping-html-boolean-attributes-in-their-original-form-when-parsing-with-jsoup
            .prettyPrint(false)
        private const val GROUP_BLOCK_NAME = 1
        private const val GROUP_JSON_ATTRIBUTES = 2
        private const val GROUP_BLOCK_CONTENT_DOCUMENT = 3
        private const val GROUP_CLOSING_COMMENT = 4
    }
}
