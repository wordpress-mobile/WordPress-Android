package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.jsoup.nodes.Document
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.helpers.MediaFile
import java.util.regex.Pattern

class GalleryBlockProcessor(
    localId: String, mediaFile: MediaFile, siteUrl: String,
    private val mMediaUploadCompletionProcessor: MediaUploadCompletionProcessor
) : BlockProcessor(localId, mediaFile) {
    private val mAttachmentPageUrl: String? = mediaFile.getAttachmentPageURL(siteUrl)
    private var mLinkTo: String? = null

    /**
     * Query selector for selecting the img element from gallery which needs processing
     */
    private val mGalleryImageQuerySelector = StringBuilder()
        .append("img[data-id=\"")
        .append(localId)
        .append("\"]")
        .toString()

    override fun processBlockContentDocument(document: Document): Boolean {
        // select image element with our local id
        val targetImg = document.select(mGalleryImageQuerySelector).first()

        // if a match is found, proceed with replacement
        if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", remoteUrl)
            targetImg.attr("data-id", remoteId)
            targetImg.attr("data-full-url", remoteUrl)
            targetImg.attr("data-link", mAttachmentPageUrl)

            // replace class
            targetImg.removeClass("wp-image-$localId")
            targetImg.addClass("wp-image-$remoteId")

            // set parent anchor href if necessary
            val parent = targetImg.parent()
            if (parent != null && parent.`is`("a") && mLinkTo != null) {
                when (mLinkTo) {
                    "file" -> parent.attr("href", remoteUrl)
                    "post" -> parent.attr("href", mAttachmentPageUrl)
                    else -> return false
                }
            }

            // return injected block
            return true
        }

        return false
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean {
        // The new format does not have an `ids` attributes, so returning false here will defer to recursive processing
        val ids = jsonAttributes.getAsJsonArray("ids")
        if (ids == null || ids.isJsonNull) {
            return false
        }
        val linkTo = jsonAttributes["linkTo"]
        if (linkTo != null && !linkTo.isJsonNull) {
            mLinkTo = linkTo.asString
        }
        for (i in 0 until ids.size()) {
            val id = ids[i]
            if (id != null && !id.isJsonNull && id.asString == localId) {
                try {
                    ids[i] = JsonPrimitive(remoteId.toInt(10))
                } catch (e: NumberFormatException) {
                    AppLog.e(AppLog.T.MEDIA, e.message)
                }
                return true
            }
        }
        return false
    }

    override fun processInnerBlock(block: String): String {
        val innerMatcher = PATTERN_GALLERY_INNER.matcher(block)
        val innerCapturesFound = innerMatcher.find()

        // process inner contents recursively
        if (innerCapturesFound) {
            val innerProcessed =
                mMediaUploadCompletionProcessor.processContent(innerMatcher.group(2)) //
            return StringBuilder()
                .append(innerMatcher.group(1))
                .append(innerProcessed)
                .append(innerMatcher.group(3))
                .toString()
        }

        return block
    }

    companion object {
        /**
         * Template pattern used to match and splice inner image blocks in the refactored gallery format
         */
        private val PATTERN_GALLERY_INNER: Pattern = Pattern.compile(
            StringBuilder()
                .append("(^.*?<figure class=\"[^\"]*?wp-block-gallery[^\"]*?\">\\s*)")
                .append("(.*)") // inner block contents
                .append("(\\s*</figure>\\s*<!-- /wp:gallery -->.*)").toString(), Pattern.DOTALL
        )
    }
}
