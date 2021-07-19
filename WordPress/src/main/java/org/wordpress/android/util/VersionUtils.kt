package org.wordpress.android.util

import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.helpers.Version

object VersionUtils {
    /**
     * Checks if a given version [String] is equal to or higher than another given minimal version [String].
     *
     * Note: This method ignores "-beta", "-alpha" or "-RC" versions, meaning that this will return `true` for
     * a version "5.5-beta1" and `minVersion` "5.5", for example.
     *
     * @param version The version [String] to check.
     * @param minVersion A minimal acceptable version [String].
     * @return `true` if the version is equal to or higher than the `minVersion`; `false` otherwise.
     */
    @JvmStatic fun checkMinimalVersion(version: String?, minVersion: String?) =
            if (!version.isNullOrEmpty() && !minVersion.isNullOrEmpty()) {
                try {
                    Version(stripVersionSuffixes(version)) >= Version(stripVersionSuffixes(minVersion))
                } catch (e: IllegalArgumentException) {
                    AppLog.e(UTILS, "Invalid version $version, expected $minVersion", e)
                    false
                }
            } else false

    // Strip any trailing "-beta", "-alpha" or "-RC" suffixes from the version
    private fun stripVersionSuffixes(version: String) = version.substringBefore("-")
}
