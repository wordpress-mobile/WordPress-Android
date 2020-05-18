package org.wordpress.android.ui.whatsnew

import android.text.TextUtils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.wordpress.android.WordPress
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.LocaleManagerWrapper
import java.io.FileNotFoundException
import javax.inject.Inject

class FeatureAnnouncementProvider @Inject constructor(val localeManagerWrapper: LocaleManagerWrapper) {
    fun getLatestFeatureAnnouncement(): FeatureAnnouncement? {
        return getFeatureAnnouncements().firstOrNull()
    }

    private fun getFeatureAnnouncements(): List<FeatureAnnouncement> {
        val defaultLanguage = "en"
        // LocaleManager might not return language code in 2 letter format, so trying to do the conversion just in case
        val currentLanguage = localeManagerWrapper.getLanguage().split("_")[0]

        val featureAnnouncements = arrayListOf<FeatureAnnouncement>()

        var featureAnnouncementFileContent: String? = null
        try {
            featureAnnouncementFileContent = WordPress.getContext().assets
                    .open("FEATURE_ANNOUNCEMENTS.json")
                    .bufferedReader().use { it.readText() }
        } catch (fileNotFound: FileNotFoundException) {
            AppLog.e(T.FEATURE_ANNOUNCEMENT, "File with feature announcements is missing")
            return featureAnnouncements
        }

        if (TextUtils.isEmpty(featureAnnouncementFileContent)) {
            AppLog.v(T.FEATURE_ANNOUNCEMENT, "No feature announcements found in FEATURE_ANNOUNCEMENTS.json")
            return featureAnnouncements
        }

        val rootAnnouncementObject = JsonParser().parse(featureAnnouncementFileContent).asJsonObject
        val announcementsArray = rootAnnouncementObject["announcements"].asJsonArray

        for (announcementElement in announcementsArray) {
            val announcementObject = announcementElement.asJsonObject

            val features = arrayListOf<FeatureAnnouncementItem>()

            var isLocalized = true

            val featuresArray = announcementObject.get("features").asJsonArray
            for (featureElement in featuresArray) {
                val featureObject = featureElement.asJsonObject

                val localizedDataObject: JsonObject

                if (featureObject.has(currentLanguage)) {
                    localizedDataObject = featureObject.get(currentLanguage).asJsonObject
                } else {
                    localizedDataObject = featureObject.get(defaultLanguage).asJsonObject
                    isLocalized = false
                }

                val title = localizedDataObject.get("title").asString
                val subtitle = localizedDataObject.get("subtitle").asString

                val iconBase64 = featureObject.get("iconBase64").asString
                val iconUrl = featureObject.get("iconUrl").asString

                features.add(FeatureAnnouncementItem(title, subtitle, iconBase64, iconUrl))
            }

            val featureAnnouncement = FeatureAnnouncement(
                    announcementObject.get("appVersionName").asString,
                    announcementObject.get("announceVersion").asInt,
                    announcementObject.get("detailsUrl").asString,
                    isLocalized,
                    features
            )

            featureAnnouncements.add(featureAnnouncement)
        }

        return featureAnnouncements
    }

    fun isAnnouncementOnUpgradeAvailable(): Boolean {
        val announcements = getFeatureAnnouncements()
        return announcements.isNotEmpty() && announcements[0].isLocalized && announcements[0].features.isNotEmpty()
    }
}
