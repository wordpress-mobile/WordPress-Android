package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile
import java.util.regex.Pattern

class CoverBlockProcessor(
    localId: String, mediaFile: MediaFile,
    private val mMediaUploadCompletionProcessor: MediaUploadCompletionProcessor
) : BlockProcessor(localId, mediaFile) {
    private var mHasVideoBackground = false

    override fun processInnerBlock(block: String): String {
        val innerMatcher = PATTERN_COVER_INNER.matcher(block)
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

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean {
        val id = jsonAttributes["id"]
        if (id != null && !id.isJsonNull && id.asInt == localId.toInt(10)) {
            addIntPropertySafely(jsonAttributes, "id", remoteId)

            jsonAttributes.addProperty("url", remoteUrl)

            // check if background type is video
            val backgroundType = jsonAttributes["backgroundType"]
            mHasVideoBackground =
                backgroundType != null && !backgroundType.isJsonNull && "video" == backgroundType.asString
            return true
        }

        return false
    }

    override fun processBlockContentDocument(document: Document): Boolean {
        // select cover block div
        val targetDiv = document.selectFirst(".wp-block-cover")

        // if a match is found, proceed with replacement
        if (targetDiv != null) {
            if (mHasVideoBackground) {
                val videoElement = targetDiv.selectFirst("video")
                if (videoElement != null) {
                    videoElement.attr("src", remoteUrl)
                } else {
                    return false
                }
            } else {
                // replace background-image url in style attribute
                val style =
                    PATTERN_BACKGROUND_IMAGE_URL.matcher(targetDiv.attr("style")).replaceFirst(
                        String.format("background-image:url(%1\$s)", remoteUrl)
                    )
                targetDiv.attr("style", style)
            }

            // return injected block
            return true
        }

        return false
    }

    companion object {
        /**
         * Template pattern used to match and splice cover inner blocks
         */
        private val PATTERN_COVER_INNER: Pattern = Pattern.compile(
            StringBuilder()
                .append("(^.*?<div class=\"wp-block-cover__inner-container\">\\s*)")
                .append("(.*)") // inner block contents
                .append("(\\s*</div>\\s*</div>\\s*<!-- /wp:cover -->.*)").toString(), Pattern.DOTALL
        )

        /**
         * Pattern to match background-image url in cover block html content
         */
        private val PATTERN_BACKGROUND_IMAGE_URL: Pattern = Pattern.compile(
            "background-image:\\s*url\\([^\\)]+\\)"
        )
    }
}
