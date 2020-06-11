package org.wordpress.android.ui.whatsnew

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.wordpress.android.WordPress
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.StringUtils
import java.io.FileNotFoundException
import javax.inject.Inject

class FeatureAnnouncementProvider @Inject constructor(val localeManagerWrapper: LocaleManagerWrapper) {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.create()
    }

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

        val parsedFeatureAnnouncement: ParsedFeatureAnnouncements = gson.fromJson(
                featureAnnouncementFileContent,
                ParsedFeatureAnnouncements::class.java
        )

        for (parsedAnnouncement in parsedFeatureAnnouncement.announcements) {
            val isLocalized = parsedAnnouncement.features.all { feature ->
                feature.localizedContent.containsKey(currentLanguage)
            }

            val mappedFeatures = arrayListOf<FeatureAnnouncementItem>()
            for (parsedFeature in parsedAnnouncement.features) {
                val localizedFeatureData = parsedFeature.localizedContent.getOrElse(currentLanguage) {
                    parsedFeature.localizedContent[defaultLanguage]
                }

                mappedFeatures.add(
                        FeatureAnnouncementItem(
                                StringUtils.notNullStr(localizedFeatureData?.title),
                                StringUtils.notNullStr(localizedFeatureData?.subtitle),
                                parsedFeature.iconBase64,
                                parsedFeature.iconUrl
                        )
                )
            }

            val mappedAnnouncement = FeatureAnnouncement(
                    parsedAnnouncement.appVersionName,
                    parsedAnnouncement.announcementVersion,
                    parsedAnnouncement.detailsUrl,
                    isLocalized,
                    mappedFeatures
            )

            featureAnnouncements.add(mappedAnnouncement)
        }

        return featureAnnouncements
    }

    fun isAnnouncementOnUpgradeAvailable(): Boolean {
        val announcements = getFeatureAnnouncements()
        return announcements.isNotEmpty() && announcements[0].isLocalized && announcements[0].features.isNotEmpty()
    }

    private data class ParsedFeatureAnnouncements(val announcements: List<ParsedFeatureAnnouncement>)

    private data class ParsedFeatureAnnouncement(
        val appVersionName: String,
        val announcementVersion: Int,
        val detailsUrl: String,
        val features: List<ParsedFeatureAnnouncementItem>
    )

    private data class ParsedFeatureAnnouncementItem(
        val localizedContent: HashMap<String, ParsedFeatureAnnouncementItemLocalizedContent>,
        val iconBase64: String,
        val iconUrl: String
    )

    private data class ParsedFeatureAnnouncementItemLocalizedContent(
        val title: String,
        val subtitle: String
    )
}
