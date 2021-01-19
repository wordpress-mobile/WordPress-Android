package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class AudioBlockProcessor(localId: String?, mediaFile: MediaFile?) : BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document?): Boolean {
        val audioElements = document?.select(AUDIO_TAG)

        audioElements?.let { elements ->
            for (element in elements) {
                // replaces the src attribute's local url with the remote counterpart.
                element.attr(SRC_ATTRIBUTE, mRemoteUrl)
            }
            return true
        }
        return false
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject?): Boolean {
        val id = jsonAttributes?.get(ID_ATTRIBUTE)

        return if (id != null && !id.isJsonNull && id.asString == mLocalId) {
            jsonAttributes.apply {
                addProperty(ID_ATTRIBUTE, Integer.parseInt(mRemoteId))
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
