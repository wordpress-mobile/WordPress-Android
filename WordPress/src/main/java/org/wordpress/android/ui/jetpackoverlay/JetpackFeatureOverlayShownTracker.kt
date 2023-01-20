package org.wordpress.android.ui.jetpackoverlay

import android.content.SharedPreferences
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.NOTIFICATIONS
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.READER
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.STATS
import javax.inject.Inject

class JetpackFeatureOverlayShownTracker @Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun getTheLastShownOverlayTimeStamp(phase: JetpackFeatureRemovalOverlayPhase): Long? {
        val overlayShownTimeStampList: ArrayList<Long> = arrayListOf()
        getFeatureOverlayShownTimeStamp(STATS, phase)?.let { overlayShownTimeStampList.add(it) }
        getFeatureOverlayShownTimeStamp(NOTIFICATIONS, phase)?.let { overlayShownTimeStampList.add(it) }
        getFeatureOverlayShownTimeStamp(READER, phase)?.let { overlayShownTimeStampList.add(it) }
        // No jetpack connected feature is accessed yet
        if (overlayShownTimeStampList.isEmpty()) return null
        return overlayShownTimeStampList.maxOf { it }
    }

    fun getFeatureOverlayShownTimeStamp(
        jetpackOverlayConnectedFeature: JetpackOverlayConnectedFeature,
        phase: JetpackFeatureRemovalOverlayPhase
    ): Long? {
        val overlayShownTime = sharedPrefs.getLong(jetpackOverlayConnectedFeature.getPreferenceKey(phase), 0L)
        // jetpack connected feature is not accessed yet
        if (overlayShownTime == 0L) return null
        return overlayShownTime
    }

    fun setFeatureOverlayShownTimeStamp(
        jetpackOverlayConnectedFeature: JetpackOverlayConnectedFeature,
        phase: JetpackFeatureRemovalOverlayPhase,
        timeStamp: Long
    ) {
        sharedPrefs.edit().putLong(jetpackOverlayConnectedFeature.getPreferenceKey(phase), timeStamp).apply()
    }
}

enum class JetpackOverlayConnectedFeature(private val featureSpecificPreferenceKey: String) {
    STATS("STATS_OVERLAY_SHOWN_TIME_STAMP_KEY"),
    NOTIFICATIONS("READER_OVERLAY_SHOWN_TIME_STAMP_KEY"),
    READER("NOTIFICATIONS_OVERLAY_SHOWN_TIME_STAMP_KEY");
    fun getPreferenceKey(phase: JetpackFeatureRemovalOverlayPhase): String {
        return featureSpecificPreferenceKey.plus(phase.preferenceKey)
    }
}

enum class JetpackFeatureRemovalOverlayPhase(val preferenceKey: String) {
    PHASE_ONE("JETPACK_FEATURE_PHASE_ONE"),
    PHASE_TWO("JETPACK_FEATURE_PHASE_TWO"),
    PHASE_THREE("JETPACK_FEATURE_PHASE_THREE")
}
