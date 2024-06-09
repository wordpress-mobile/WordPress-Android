package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class AudioBlockProcessor(localId: String, mediaFile: MediaFile) : BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document): Boolean {
        val audioElements = document.select(AUDIO_TAG)

        for (element in audioElements) {
            // replaces the src attribute's local url with the remote counterpart.
            element.attr(SRC_ATTRIBUTE, remoteUrl)
        }
        return true
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject): Boolean {
        val id = jsonAttributes.get(ID_ATTRIBUTE)

        return if (id != null && !id.isJsonNull && id.asString == localId) {
            jsonAttributes.apply {
                addIntPropertySafely(this, ID_ATTRIBUTE, remoteId)
            }
            true
        } else {
            false
        }
    }

    companion object {
        const val AUDIO_TAG = "audio"
        const val SRC_ATTRIBUTE = "src"
        const val ID_ATTRIBUTE = "id"
    }
}
