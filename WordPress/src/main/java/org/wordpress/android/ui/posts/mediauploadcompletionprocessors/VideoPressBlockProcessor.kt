package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.google.gson.JsonObject
import org.jsoup.nodes.Document
import org.wordpress.android.util.UriEncoder
import org.wordpress.android.util.helpers.MediaFile

class VideoPressBlockProcessor(
    localId: String?,
    mediaFile: MediaFile?,
    private val uriEncoder: UriEncoder = UriEncoder()
) : BlockProcessor(localId, mediaFile) {
    class VideoPressBlockSettings(
        var autoplay: Boolean? = null,
        var controls: Boolean? = null,
        var loop: Boolean? = null,
        var muted: Boolean? = null,
        private var persistVolume: Boolean? = null,
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
            playsinline = jsonAttributes["playsinline"]?.asBoolean ?: false
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
     * In order to have a cleaner URL, only the options differing from the default settings are added.
     * Matches logic in Jetpack.
     * Ref: https://github.com/Automattic/jetpack/blob/b1b826ab38690c5fad18789301ac81297a458878/projects/packages/videopress/src/client/lib/url/index.ts#L19-L67
     *
     */
    private fun getVideoPressURL(guid: String): String {
        val queryArgs = getDefaultQueryArgs()
        getBlockSettingsQueryArgs(queryArgs, mBlockSettings)

        val encodedQueryArgs = queryArgs.entries.joinToString("&") {
            val encodedValue = it.value.removeSurrounding("\"")
            "${uriEncoder.encode(it.key)}=${uriEncoder.encode(encodedValue)}"
        }

        return "https://videopress.com/v/$guid?$encodedQueryArgs"
    }

    private fun getDefaultQueryArgs(): MutableMap<String, String> {
        return mutableMapOf(
            "resizeToParent" to "true",
            "cover" to "true"
        )
    }

    private fun getBlockSettingsQueryArgs(
        queryArgs: MutableMap<String, String>,
        blockSettings: VideoPressBlockSettings
    ) {
        with(blockSettings) {
            addAutoplayArg(queryArgs, autoplay)
            addControlsArg(queryArgs, controls)
            addLoopArg(queryArgs, loop)
            addMutedAndPersistVolumeArgs(queryArgs, muted)
            addPlaysinlineArg(queryArgs, playsinline)
            addPosterArg(queryArgs, poster)
            addPreloadArg(queryArgs, preload)
            addSeekbarArgs(queryArgs, seekbarColor, seekbarPlayedColor, seekbarLoadingColor)
            addUseAverageColorArg(queryArgs, useAverageColor)
        }
    }

    // Adds AutoPlay option. Turned OFF by default.
    private fun addAutoplayArg(queryArgs: MutableMap<String, String>, autoplay: Boolean?) {
        if (autoplay == true) {
            queryArgs["autoPlay"] = "true"
        }
    }

    // Adds Controls option. Turned ON by default.
    private fun addControlsArg(queryArgs: MutableMap<String, String>, controls: Boolean?) {
        if (controls == false) {
            queryArgs["controls"] = "false"
        }
    }

    // Adds Loops option. Turned OFF by default.
    private fun addLoopArg(queryArgs: MutableMap<String, String>, loop: Boolean?) {
        if (loop == true) {
            queryArgs["loop"] = "true"
        }
    }

    // Adds Volume-related options. Muted: Turned OFF by default.
    private fun addMutedAndPersistVolumeArgs(queryArgs: MutableMap<String, String>, muted: Boolean?) {
        if (muted == true) {
            queryArgs["muted"] = "true"
            queryArgs["persistVolume"] = "false"
        }
    }

    // Adds PlaysInline option. Turned OFF by default.
    private fun addPlaysinlineArg(queryArgs: MutableMap<String, String>, playsinline: Boolean?) {
        if (playsinline == true) {
            queryArgs["playsinline"] = "true"
        }
    }

    // Adds Poster option. No image by default.
    private fun addPosterArg(queryArgs: MutableMap<String, String>, poster: String?) {
        poster?.let { queryArgs["posterUrl"] = it }
    }

    // Adds Preload option. Metadata by default.
    private fun addPreloadArg(queryArgs: MutableMap<String, String>, preload: String?) {
        preload?.let { if (it.isNotEmpty()) queryArgs["preloadContent"] = it }
    }

    /**
     * Adds Seekbar options.
     * - SeekbarColor: No color by default.
     * - SeekbarPlayerColor: No color by default.
     * - SeekbarLoadingColor: No color by default.
     */
    private fun addSeekbarArgs(
        queryArgs: MutableMap<String, String>,
        seekbarColor: String?,
        seekbarPlayedColor: String?,
        seekbarLoadingColor: String?
    ) {
        seekbarColor?.let { if (it.isNotEmpty()) queryArgs["sbc"] = it }
        seekbarPlayedColor?.let { if (it.isNotEmpty()) queryArgs["sbpc"] = it }
        seekbarLoadingColor?.let { if (it.isNotEmpty()) queryArgs["sblc"] = it }
    }

    // Adds UseAverageColor option. Turned ON by default.
    private fun addUseAverageColorArg(queryArgs: MutableMap<String, String>, useAverageColor: Boolean?) {
        if (useAverageColor == true) {
            queryArgs["useAverageColor"] = "true"
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
        val src = jsonAttributes?.get(SRC_ATTRIBUTE)

        return if (id != null && !id.isJsonNull && id.asString == mLocalId) {
            jsonAttributes.apply {
                addProperty(ID_ATTRIBUTE, Integer.parseInt(mRemoteId))
                addProperty(GUID_ATTRIBUTE, mRemoteGuid)
                when (src) {
                    null -> Unit
                    else -> jsonAttributes.addProperty(SRC_ATTRIBUTE, mRemoteUrl)
                }
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
        const val SRC_ATTRIBUTE = "src"
    }
}
