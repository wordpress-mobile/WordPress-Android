package org.wordpress.android.ui.whatsnew

import org.wordpress.android.fluxc.utils.WhatsNewAppVersionUtils

data class FeatureAnnouncements(val announcements: List<FeatureAnnouncement>)

data class FeatureAnnouncement(
    val appVersionName: String,
    val announcementVersion: Int,
    val minimumAppVersion: String,
    val maximumAppVersion: String,
    val detailsUrl: String?,
    val isLocalized: Boolean = false,
    val features: List<FeatureAnnouncementItem>
) {
    private val openEndedVersionBracketIndicator = "-1.0"

    fun canBeDisplayedOnAppUpgrade(appVersionName: String): Boolean {
        val integerRepresentationOfVersionName = WhatsNewAppVersionUtils.versionNameToInt(appVersionName)

        if (integerRepresentationOfVersionName == -1) {
            return false
        }

        val minAppVersion = WhatsNewAppVersionUtils.versionNameToInt(minimumAppVersion)
        val maxAppVersion = WhatsNewAppVersionUtils.versionNameToInt(maximumAppVersion)

        val isWithinRange = when {
            minimumAppVersion == openEndedVersionBracketIndicator -> {
                integerRepresentationOfVersionName <= maxAppVersion
            }
            maximumAppVersion == openEndedVersionBracketIndicator -> {
                integerRepresentationOfVersionName >= minAppVersion
            }
            else -> {
                IntRange(
                        minAppVersion,
                        maxAppVersion
                ).contains(integerRepresentationOfVersionName)
            }
        }

        return isLocalized && features.isNotEmpty() && isWithinRange
    }
}

data class FeatureAnnouncementItem(
    val title: String,
    val subtitle: String,
    val iconBase64: String,
    val iconUrl: String
)
