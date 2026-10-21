package org.wordpress.android.ui.mlp

import com.google.gson.annotations.SerializedName
import org.wordpress.android.util.BuildConfig
import org.wordpress.android.util.extensions.parseJsonFromAsset
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

private const val defaultAssetFilename = "supported-blocks.json"

class SupportedBlocksProvider @Inject constructor(private val contextProvider: ContextProvider) {
    fun fromAssets(assetFileName: String = defaultAssetFilename): SupportedBlocks {
        return contextProvider.getContext().parseJsonFromAsset(assetFileName, SupportedBlocks::class.java)
            ?: SupportedBlocks()
    }
}

// Parsed by Gson; @SerializedName keeps the fields through R8
data class SupportedBlocks(
    @SerializedName("common") val common: List<String> = listOf(),
    @SerializedName("devOnly") val devOnly: List<String> = listOf(),
    @SerializedName("iOSOnly") val iOSOnly: List<String> = listOf(),
    @SerializedName("androidOnly") val androidOnly: List<String> = listOf()
) {
    val supported: List<String>
        get() = (if (BuildConfig.DEBUG) listOf(common, devOnly, androidOnly) else listOf(common, androidOnly)).flatten()
}
