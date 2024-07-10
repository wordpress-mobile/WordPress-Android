package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class VideoBlockProcessor(localId: String, mediaFile: MediaFile) : BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document): Boolean {
        // select video element with our local id
        val targetVideo = document.select("video").first()

        // if a match is found for video, proceed with replacement
        return targetVideo?.let {
            // replace attribute
            targetVideo.attr("src", remoteUrl)

            // return injected block
            true
        } ?: false
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean {
        val id = jsonAttributes["id"]
        return if (id != null && !id.isJsonNull && id.asString == localId) {
            addIntPropertySafely(jsonAttributes, "id", remoteId)
            true
        } else {
            false
        }
    }
}
