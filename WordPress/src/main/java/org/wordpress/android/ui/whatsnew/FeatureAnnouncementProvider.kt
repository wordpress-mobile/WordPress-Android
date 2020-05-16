package org.wordpress.android.ui.whatsnew

import android.text.TextUtils
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

    fun getFeatureAnnouncements(): List<FeatureAnnouncement> {
        val defaultLanguage = "en"
        val currentLanguage = localeManagerWrapper.getLanguage()

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

            val featuresArray = announcementObject.get("features").asJsonArray
            for (featureElement in featuresArray) {
                val featureObject = featureElement.asJsonObject

                val localizedDataObject = if (featureObject.has(currentLanguage)) {
                    featureObject.get(currentLanguage).asJsonObject
                } else {
                    featureObject.get(defaultLanguage).asJsonObject
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
                    features
            )

            featureAnnouncements.add(featureAnnouncement)
        }

        return featureAnnouncements
    }

    fun isFeatureAnnouncementAvailable(): Boolean {
        return getFeatureAnnouncements().isNotEmpty()
    }
}
