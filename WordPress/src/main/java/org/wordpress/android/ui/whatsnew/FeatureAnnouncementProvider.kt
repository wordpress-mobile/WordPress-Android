package org.wordpress.android.ui.whatsnew

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.wordpress.android.WordPress
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.io.FileNotFoundException
import javax.inject.Inject

class FeatureAnnouncementProvider @Inject constructor() {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.create()
    }

    fun getLatestFeatureAnnouncement(): FeatureAnnouncement? {
        return getFeatureAnnouncements().firstOrNull()
    }

    fun getFeatureAnnouncements(): List<FeatureAnnouncement> {
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

        Gson().fromJson(
                featureAnnouncementFileContent,
                FeatureAnnouncements::class.java
        )
        val featureAnnouncement: FeatureAnnouncements = gson.fromJson(
                featureAnnouncementFileContent,
                FeatureAnnouncements::class.java
        )

        featureAnnouncements.addAll(featureAnnouncement.announcements)

        return featureAnnouncements
    }

    fun isFeatureAnnouncementAvailable(): Boolean {
        return getFeatureAnnouncements().isNotEmpty()
    }
}
