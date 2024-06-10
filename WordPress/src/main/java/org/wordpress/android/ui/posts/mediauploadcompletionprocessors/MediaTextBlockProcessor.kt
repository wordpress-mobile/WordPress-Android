package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class MediaTextBlockProcessor(localId: String, mediaFile: MediaFile) : BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document): Boolean {
        // select image element with our local id
        val targetImg = document.select("img").first()

        // if a match is found for img, proceed with replacement
        return if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", remoteUrl)

            // replace class
            targetImg.removeClass("wp-image-$localId")
            targetImg.addClass("wp-image-$remoteId")

            // return injected block
            true
        } else { // try video
            // select video element with our local id
            val targetVideo = document.select("video").first()

            // if a match is found for video, proceed with replacement
            targetVideo?.let {
                // replace attribute
                targetVideo.attr("src", remoteUrl)

                // return injected block
                true
            } ?: false
        }
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean {
        val id = jsonAttributes["mediaId"]
        return if (id != null && !id.isJsonNull && id.asString == localId) {
            addIntPropertySafely(jsonAttributes, "mediaId", remoteId)
            true
        } else {
            false
        }
    }
}
