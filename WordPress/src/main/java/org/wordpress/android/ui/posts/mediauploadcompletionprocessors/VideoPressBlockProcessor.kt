package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class VideoPressBlockProcessor(
    localId: String?,
    mediaFile: MediaFile?
) : BlockProcessor(localId, mediaFile) {
    override fun processBlockContentDocument(document: Document?): Boolean {
        return false
    }

    override fun processBlockJsonAttributes(jsonAttributes: JsonObject?): Boolean {
        val id = jsonAttributes?.get(ID_ATTRIBUTE)
        val src = jsonAttributes?.get(SRC_ATTRIBUTE)?.asString

        return if (id != null && !id.isJsonNull && id.asString == mLocalId) {
            jsonAttributes.apply {
                addProperty(ID_ATTRIBUTE, Integer.parseInt(mRemoteId))
                addProperty(GUID_ATTRIBUTE, mRemoteGuid)
                if (src?.startsWith("file:") == true) {
                    remove(SRC_ATTRIBUTE)
                }
            }
            true
        } else {
            false
        }
    }

    companion object {
        const val ID_ATTRIBUTE = "id"
        const val GUID_ATTRIBUTE = "guid"
        const val SRC_ATTRIBUTE = "src"
    }
}
