package org.wordpress.android.ui.whatsnew

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.editor.EditorImageMetaData
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureAnnouncementProvider @Inject constructor() {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.create()
    }

    fun getLatestFeatureAnnouncement(): FeatureAnnouncement? {
        var featureAnnouncementFileContent: String? = null
        try {
            featureAnnouncementFileContent = WordPress.getContext().assets
                    .open("FEATURE_ANNOUNCEMENTS.json")
                    .bufferedReader().use { it.readText() }
        } catch (fileNotFound: FileNotFoundException) {
            AppLog.e(T.FEATURE_ANNOUNCEMENT, "File with feature announcements is missing")
            return null
        }

        val featureAnnouncement: FeatureAnnouncements = gson.fromJson(
                featureAnnouncementFileContent,
                FeatureAnnouncements::class.java
        )

        return featureAnnouncement.announcements.firstOrNull()
    }

    fun isFeatureAnnouncementAvailable(): Boolean {
        return getLatestFeatureAnnouncement() != null
    }
}
