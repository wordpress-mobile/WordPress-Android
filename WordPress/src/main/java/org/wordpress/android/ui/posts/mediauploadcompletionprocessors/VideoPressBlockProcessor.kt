package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.helpers.MediaFile

class VideoPressBlockProcessor(localId: String?, mediaFile: MediaFile?) : BlockProcessor(localId, mediaFile) {
    class VideoPressBlockSettings(
        var autoplay: Boolean? = null,
        var controls: Boolean? = null,
        var loop: Boolean? = null,
        var muted: Boolean? = null,
        var persistVolume: Boolean? = null,
        var playsinline: Boolean? = null,
        var poster: String? = null,
        var preload: String? = null,
        var seekbarColor: String? = null,
        var seekbarPlayedColor: String? = null,
        var seekbarLoadingColor: String? = null,
        var useAverageColor: Boolean? = null
    ) {
        constructor(jsonAttributes: JsonObject) : this() {
            autoplay = jsonAttributes["autoplay"]?.asBoolean ?: false
            controls = jsonAttributes["controls"]?.asBoolean ?: true
            loop = jsonAttributes["loop"]?.asBoolean ?: false
            jsonAttributes["muted"]?.asBoolean?.let { isMuted ->
                muted = isMuted
                persistVolume = !isMuted
            }
            poster = jsonAttributes["poster"]?.toString()
            preload = jsonAttributes["preload"]?.toString() ?: "metadata"
            seekbarColor = jsonAttributes["seekbarColor"]?.toString()
            seekbarPlayedColor = jsonAttributes["seekbarPlayedColor"]?.toString()
            seekbarLoadingColor = jsonAttributes["seekbarLoadingColor"]?.toString()
            useAverageColor = jsonAttributes["useAverageColor"]?.asBoolean ?: true
        }
    }

    private var mBlockSettings = VideoPressBlockSettings()

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
                addProperty(GUID_ATTRIBUTE, mRemoteGuid)
            }

            mBlockSettings = VideoPressBlockSettings(jsonAttributes)

            true
        } else {
            false
        }
    }

    companion object {
        const val ID_ATTRIBUTE = "id"
        const val GUID_ATTRIBUTE = "guid"
    }
}
