package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import android.net.Uri
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

    /**
     * Build VideoPress URL based on the values of the block's various settings.
     *
     */
    fun getVideoPressURL(guid: String): String {
        val queryArgs = getDefaultQueryArgs()
        getBlockSettingsQueryArgs(queryArgs, mBlockSettings)

        val encodedQueryArgs = queryArgs.entries.joinToString("&") {
            "${Uri.encode(it.key)}=${Uri.encode(it.value)}"
        }

        return "https://videopress.com/v/$guid?$encodedQueryArgs"
    }

    private fun getDefaultQueryArgs(): MutableMap<String, String> {
        return mutableMapOf(
            "resizeToParent" to "true",
            "cover" to "true"
        )
    }

    /**
     * In order to have a cleaner URL, only the options differing from the default settings are added.
     *
     * Turned OFF by default: Autoplay, Loop, Muted, Plays Inline
     * Turned ON by default: Controls, UseAverageColor
     * No values by default: Poster, SeekbarColor, SeekbarPlayerColor, SeekbarLoadingColor
     * Set to "metadata" by default: Preload
     *
     * Matches logic in Jetpack.
     * Ref: https://github.com/Automattic/jetpack/blob/b1b826ab38690c5fad18789301ac81297a458878/projects/packages/videopress/src/client/lib/url/index.ts#L19-L67
     *
     */
    private fun getBlockSettingsQueryArgs(
        queryArgs: MutableMap<String, String>,
        blockSettings: VideoPressBlockSettings
    ) {
        with(blockSettings) {
            autoplay?.let { if (it) queryArgs["autoPlay"] = "true" }
            controls?.let { if (!it) queryArgs["controls"] = "false" }
            loop?.let { if (it) queryArgs["loop"] = "true" }
            muted?.let {
                if (it) {
                    queryArgs["muted"] = "true"; queryArgs["persistVolume"] = "false"
                }
            }
            playsinline?.let { if (it) queryArgs["playsinline"] = "true" }
            poster?.let { queryArgs["posterUrl"] = it }
            preload?.let { if (it.isNotEmpty()) queryArgs["preloadContent"] = it }
            seekbarColor?.let { if (it.isNotEmpty()) queryArgs["sbc"] = it }
            seekbarPlayedColor?.let { if (it.isNotEmpty()) queryArgs["sbpc"] = it }
            seekbarLoadingColor?.let { if (it.isNotEmpty()) queryArgs["sblc"] = it }
            useAverageColor?.let { if (it) queryArgs["useAverageColor"] = "true" }
        }
    }

    override fun processBlockContentDocument(document: Document?): Boolean {
        val videoPressElements = document?.select("figure")

        if (videoPressElements != null) {
            val url = getVideoPressURL(mRemoteGuid)
            videoPressElements.append("<div class='jetpack-videopress-player__wrapper'>$url</div>")
            return true
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
