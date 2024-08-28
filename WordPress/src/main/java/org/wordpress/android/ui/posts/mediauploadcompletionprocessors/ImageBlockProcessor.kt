package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class ImageBlockProcessor(localId: String, mediaFile: MediaFile) : BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document): Boolean {
        // select image element with our local id
        val targetImg = document.select("img").first()

        // if a match is found, proceed with replacement
        return targetImg?.let {
            // replace attributes
            it.attr("src", remoteUrl)

            // replace class
            it.removeClass("wp-image-$localId")
            it.addClass("wp-image-$remoteId")

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
