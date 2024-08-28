package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile
import java.util.Locale
import java.util.regex.Pattern

class CoverBlockProcessor(
    localId: String,
    mediaFile: MediaFile,
    private val mediaUploadCompletionProcessor: MediaUploadCompletionProcessor
) : BlockProcessor(localId, mediaFile) {
    private var hasVideoBackground = false

    override fun processInnerBlock(block: String): String {
        val innerMatcher = PATTERN_COVER_INNER.matcher(block)
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

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean {
        val id = jsonAttributes["id"]
        if (id != null && !id.isJsonNull && id.asInt == localId.toInt(RADIX)) {
            addIntPropertySafely(jsonAttributes, "id", remoteId)

            jsonAttributes.addProperty("url", remoteUrl)

            // check if background type is video
            val backgroundType = jsonAttributes["backgroundType"]
            hasVideoBackground = backgroundType != null &&
                !backgroundType.isJsonNull &&
                "video" == backgroundType.asString
            return true
        }

        return false
    }

    override fun processBlockContentDocument(document: Document): Boolean {
        // select cover block div
        val targetDiv = document.selectFirst(".wp-block-cover")

        // if a match is found, proceed with replacement
        return targetDiv?.let { targetDivElement ->
            if (hasVideoBackground) {
                val videoElement = targetDivElement.selectFirst("video")
                videoElement?.attr("src", remoteUrl) ?: return false
            } else {
                // replace background-image url in style attribute
                val style = PATTERN_BACKGROUND_IMAGE_URL.matcher(targetDivElement.attr("style"))
                    .replaceFirst(String.format(Locale.getDefault(), "background-image:url(%1\$s)", remoteUrl))
                targetDivElement.attr("style", style)
            }

            // return injected block
            true
        } ?: false
    }

    companion object {
        /**
         * Template pattern used to match and splice cover inner blocks
         */
        private val PATTERN_COVER_INNER = Pattern.compile(
            StringBuilder()
                .append("(^.*?<div class=\"wp-block-cover__inner-container\">\\s*)")
                .append("(.*)") // inner block contents
                .append("(\\s*</div>\\s*</div>\\s*<!-- /wp:cover -->.*)")
                .toString(),
            Pattern.DOTALL
        )

        /**
         * Pattern to match background-image url in cover block html content
         */
        private val PATTERN_BACKGROUND_IMAGE_URL = Pattern.compile("background-image:\\s*url\\([^)]+\\)")
        private const val GROUP_3 = 3
        private const val RADIX = 10
    }
}
