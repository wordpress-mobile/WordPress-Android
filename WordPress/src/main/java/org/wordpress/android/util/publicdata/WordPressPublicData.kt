package org.wordpress.android.util.publicdata

import org.wordpress.android.BuildConfig
import javax.inject.Inject

class WordPressPublicData @Inject constructor(private val packageManagerWrapper: PackageManagerWrapper) {
    fun currentPackageId(): String = BuildConfig.APPLICATION_ID

    fun currentPackageVersion(): String? =
        packageManagerWrapper.getPackageInfo(currentPackageId())?.versionName

    fun nonSemanticPackageVersion(): String? {
        val rawVersion = currentPackageVersion() ?: return null

        // Clean app semantic versioning and keep ony major-minor version info.
        // E.g 21.2-rc-3 turns to 21.2
        val majorMinorRegex = "^(\\d*)(\\.(\\d*))".toRegex()
        val wordPressVersion = majorMinorRegex.find(rawVersion)?.value

        // Verify that the resulting version is supported by
        // org.wordpress.android.util.helpers.Version.Version
        val versionIsSupportedForComparison = wordPressVersion != null &&
            Regex("[0-9]+(\\.[0-9]+)*").matchEntire(wordPressVersion) != null

        return if (versionIsSupportedForComparison) wordPressVersion else null
    }
}
