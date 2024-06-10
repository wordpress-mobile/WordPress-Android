package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class ImageBlockProcessor(localId: String, mediaFile: MediaFile) :
    BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document): Boolean {
        // select image element with our local id
        val targetImg = document.select("img").first()

        // if a match is found, proceed with replacement
        if (targetImg != null) {
            // replace attributes
            targetImg.attr("src", remoteUrl)

            // replace class
            targetImg.removeClass("wp-image-$localId")
            targetImg.addClass("wp-image-$remoteId")

            return true
        }

        return false
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean {
        val id = jsonAttributes["id"]
        if (id != null && !id.isJsonNull && id.asString == localId) {
            addIntPropertySafely(jsonAttributes, "id", remoteId)
            return true
        }
        return false
    }
}
