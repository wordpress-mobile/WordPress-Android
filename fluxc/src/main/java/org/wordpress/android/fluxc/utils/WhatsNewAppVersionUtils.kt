package org.wordpress.android.fluxc.utils

class WhatsNewAppVersionUtils {
    companion object {
        val versionRegex = Regex("(\\d+\\.)(\\d+)")

        fun versionNameToInt(appVersion: String): Int {
            val majorMinor = Regex("(\\d+\\.)(\\d+)").find(appVersion, 0)?.value

            majorMinor?.let {
                return it.replace(".", "").toInt()
            }
            return -1
        }
    }
}
