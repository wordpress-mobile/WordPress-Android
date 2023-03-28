package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class VideoPressBlockProcessor(localId: String?, mediaFile: MediaFile?) : BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document?): Boolean {
        val videoPressElements = document?.select("figure")

        if (videoPressElements != null) {
            // Functionality for populating the block's content will go here.
        }

        return false
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject?): Boolean {
        val id = jsonAttributes?.get(ID_ATTRIBUTE)

        return if (id != null && !id.isJsonNull && id.asString == mLocalId) {
            jsonAttributes.apply {
                addProperty(ID_ATTRIBUTE, Integer.parseInt(mRemoteId))
                // Functionality for populating the block's attributes will go here.
            }
            true
        } else {
            false
        }
    }

    companion object {
        const val ID_ATTRIBUTE = "id"
    }
}
