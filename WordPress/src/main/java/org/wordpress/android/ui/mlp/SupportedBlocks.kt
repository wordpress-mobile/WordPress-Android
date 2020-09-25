package org.wordpress.android.ui.mlp

import android.content.Context
import org.wordpress.android.util.BuildConfig
import org.wordpress.android.util.parseJsonFromAsset

data class SupportedBlocks(
    val common: List<String> = listOf(),
    val devOnly: List<String> = listOf(),
    val iOSOnly: List<String> = listOf(),
    val androidOnly: List<String> = listOf()
) {
    val supported: List<String>
        get() = (if (BuildConfig.DEBUG) listOf(common, devOnly, androidOnly) else listOf(common, androidOnly)).flatten()

    companion object {
        private const val assetFilename = "supported-blocks.json"

        @JvmStatic fun fromAssets(context: Context) =
                context.parseJsonFromAsset(assetFilename, SupportedBlocks::class.java) ?: SupportedBlocks()
    }
}
