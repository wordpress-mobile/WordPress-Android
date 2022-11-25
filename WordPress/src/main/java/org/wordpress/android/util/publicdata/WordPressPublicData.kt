package org.wordpress.android.util.publicdata

import org.wordpress.android.BuildConfig
import org.wordpress.android.util.publicdata.WordPressPublicData.PackageName.Jalapeno
import org.wordpress.android.util.publicdata.WordPressPublicData.PackageName.Vanilla
import org.wordpress.android.util.publicdata.WordPressPublicData.PackageName.Wasabi
import javax.inject.Inject

class WordPressPublicData @Inject constructor(private val packageManagerWrapper: PackageManagerWrapper) {
    private sealed class PackageName(val type: String, val value: String) {
        object Jalapeno : PackageName("jalapeno", "org.wordpress.android.prealpha")

        object Vanilla : PackageName("vanilla", "org.wordpress.android")

        object Wasabi : PackageName("wasabi", "org.wordpress.android.beta")
    }

    fun currentPackageId(): String = when (BuildConfig.FLAVOR_buildType) {
        Jalapeno.type -> Jalapeno.value
        Vanilla.type -> Vanilla.value
        Wasabi.type -> Wasabi.value
        else -> throw IllegalArgumentException("Failed to get Jetpack package ID: build flavor not found.")
    }

    fun currentPackageVersion(): String? = packageManagerWrapper.getPackageInfo(currentPackageId())?.versionName

    fun nonSemanticPackageVersion(): String? {
        val rawVersion = currentPackageVersion()

        // Clean app semantic versioning info. E.g 21.2-rc-3 turns to 21.2
        val wordPressVersion = rawVersion?.split("-")?.getOrNull(0) ?: rawVersion ?: ""

        // Version is supported by org.wordpress.android.util.helpers.Version.Version
        val versionIsSupportedForComparison = Regex("[0-9]+(\\.[0-9]+)*").matchEntire(wordPressVersion) != null

        return if(versionIsSupportedForComparison) wordPressVersion else null
    }
}
