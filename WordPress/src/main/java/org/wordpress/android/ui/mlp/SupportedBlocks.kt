package org.wordpress.android.ui.mlp

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

data class SupportedBlocks(
    val common: List<String> = listOf(),
    val devOnly: List<String> = listOf(),
    val iOSOnly: List<String> = listOf(),
    val androidOnly: List<String> = listOf()
) {
    val supported: List<String>
        get() = (if (BuildConfig.DEBUG) listOf(common, devOnly, androidOnly) else listOf(common, androidOnly)).flatten()
}
