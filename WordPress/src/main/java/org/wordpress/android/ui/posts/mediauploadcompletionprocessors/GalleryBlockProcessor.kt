package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.jsoup.nodes.Document
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.helpers.MediaFile
import java.util.regex.Pattern

class GalleryBlockProcessor(
    localId: String,
    mediaFile: MediaFile,
    siteUrl: String,
    private val mediaUploadCompletionProcessor: MediaUploadCompletionProcessor
) : BlockProcessor(localId, mediaFile) {
    private val attachmentPageUrl = mediaFile.getAttachmentPageURL(siteUrl)
    private var linkTo: String? = null

    /**
     * Query selector for selecting the img element from gallery which needs processing
     */
    private val galleryImageQuerySelector = StringBuilder()
        .append("img[data-id=\"")
        .append(localId)
        .append("\"]")
        .toString()

    override fun processBlockContentDocument(document: Document): Boolean {
        // select image element with our local id
        val targetImg = document.select(galleryImageQuerySelector).first()

        // if a match is found, proceed with replacement
        return targetImg?.let {
            // replace attributes
            it.attr("src", remoteUrl)
            it.attr("data-id", remoteId)
            it.attr("data-full-url", remoteUrl)
            it.attr("data-link", attachmentPageUrl)

            // replace class
            it.removeClass("wp-image-$localId")
            it.addClass("wp-image-$remoteId")

            // set parent anchor href if necessary
            val parent = it.parent()
            if (parent != null && parent.`is`("a") && linkTo != null) {
                when (linkTo) {
                    "file" -> parent.attr("href", remoteUrl)
                    "post" -> parent.attr("href", attachmentPageUrl)
                    else -> return false
                }
            }

            // return injected block
            true
        } ?: false
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean {
        // The new format does not have an `ids` attributes, so returning false here will defer to recursive processing
        val ids = jsonAttributes.getAsJsonArray("ids")

        if (ids != null && !ids.isJsonNull) {
            val linkTo = jsonAttributes["linkTo"]
            if (linkTo != null && !linkTo.isJsonNull) {
                this.linkTo = linkTo.asString
            }

            ids.forEachIndexed { index, jsonElement ->
                if (jsonElement != null && !jsonElement.isJsonNull && jsonElement.asString == localId) {
                    try {
                        ids[index] = JsonPrimitive(remoteId.toInt(RADIX))
                    } catch (e: NumberFormatException) {
                        AppLog.e(AppLog.T.MEDIA, e.message)
                    }
                    return true
                }
            }
        }
        return false
    }

    override fun processInnerBlock(block: String): String {
        val innerMatcher = PATTERN_GALLERY_INNER.matcher(block)
        val innerCapturesFound = innerMatcher.find()

        // process inner contents recursively
        if (innerCapturesFound) {
            val innerProcessed = mediaUploadCompletionProcessor.processContent(innerMatcher.group(2))
            return StringBuilder()
                .append(innerMatcher.group(1))
                .append(innerProcessed)
                .append(innerMatcher.group(GROUP_3))
                .toString()
        }

        return block
    }

    companion object {
        /**
         * Template pattern used to match and splice inner image blocks in the refactored gallery format
         */
        private val PATTERN_GALLERY_INNER = Pattern.compile(
            StringBuilder()
                .append("(^.*?<figure class=\"[^\"]*?wp-block-gallery[^\"]*?\">\\s*)")
                .append("(.*)") // inner block contents
                .append("(\\s*</figure>\\s*<!-- /wp:gallery -->.*)").toString(),
            Pattern.DOTALL
        )
        private const val RADIX = 10
        private const val GROUP_3 = 3
    }
}
