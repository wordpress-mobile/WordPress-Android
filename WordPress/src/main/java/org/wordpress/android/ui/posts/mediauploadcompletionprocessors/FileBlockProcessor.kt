package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class FileBlockProcessor(localId: String?, mediaFile: MediaFile?) : BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document?): Boolean {
        val hyperLinkTarget = document?.select(HYPERLINK_TAG)?.first() ?: return false

        // replaces the href attribute's local url with the remote counterpart.
        hyperLinkTarget.attr(HREF_ATTRIBUTE, mRemoteUrl)
        return true
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject?): Boolean {
        val id = jsonAttributes?.get(ID_ATTRIBUTE)

        return if (id != null && !id.isJsonNull && id.asString == mLocalId) {
            jsonAttributes.addProperty(ID_ATTRIBUTE, Integer.parseInt(mRemoteId))
            jsonAttributes.addProperty(HREF_ATTRIBUTE, mRemoteUrl)
            true
        } else {
            false
        }
    }

    companion object {
        const val HYPERLINK_TAG = "a"
        const val HREF_ATTRIBUTE = "href"
        const val ID_ATTRIBUTE = "id"
    }
}