package org.wordpress.android.ui.jetpackoverlay

import android.content.SharedPreferences
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.Notification
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.Reader
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.Stats
import org.wordpress.android.ui.jetpackoverlay.JETPACKFEATUREOVERLAYPHASE.PHASE_ONE
import javax.inject.Inject

class JetpackFeatureOverlayShownTracker @Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun getEarliestOverlayShownTime(phase: JETPACKFEATUREOVERLAYPHASE): Long? {
        val overlayShownTimeStampList: ArrayList<Long> = arrayListOf()
        getFeatureOverlayShownTimeStamp(Stats, phase)?.let { overlayShownTimeStampList.add(it) }
        getFeatureOverlayShownTimeStamp(Notification, phase)?.let { overlayShownTimeStampList.add(it) }
        getFeatureOverlayShownTimeStamp(Reader, phase)?.let { overlayShownTimeStampList.add(it) }
        // No jetpack connected feature is accessed yet
        if (overlayShownTimeStampList.isEmpty()) return null
        return overlayShownTimeStampList.minOf { it }
    }

    fun getFeatureOverlayShownTimeStamp(
        jetpackOverlayConnectedFeature: JetpackOverlayConnectedFeature,
        phase: JETPACKFEATUREOVERLAYPHASE
    ): Long? {
        val overlayShownTime = sharedPrefs.getLong(jetpackOverlayConnectedFeature.getPreferenceKey(phase), 0L)
        // jetpack connected feature is not accessed yet
        if (overlayShownTime == 0L) return null
        return overlayShownTime
    }

    fun setFeatureOverlayShownTimeStamp(
        jetpackOverlayConnectedFeature: JetpackOverlayConnectedFeature,
        phase: JETPACKFEATUREOVERLAYPHASE
    ) {
        sharedPrefs.edit()
                .putLong(jetpackOverlayConnectedFeature.getPreferenceKey(phase), System.currentTimeMillis())
                .apply()
    }

    fun setFeatureOverlayShownTimeStamp(
        jetpackOverlayConnectedFeature: JetpackOverlayConnectedFeature,
        phase: JETPACKFEATUREOVERLAYPHASE,
        timeStamp: Long
    ) {
        sharedPrefs.edit().putLong(jetpackOverlayConnectedFeature.getPreferenceKey(phase), timeStamp).apply()
    }

    fun clear() {
        sharedPrefs.edit().remove(Stats.getPreferenceKey(PHASE_ONE)).commit()
        sharedPrefs.edit().remove(Notification.getPreferenceKey(PHASE_ONE)).commit()
        sharedPrefs.edit().remove(Reader.getPreferenceKey(PHASE_ONE)).commit()
    }
}

sealed class JetpackOverlayConnectedFeature(private val featureSpecificPreferenceKey: String) {
    object Stats : JetpackOverlayConnectedFeature("STATS_OVERLAY_SHOWN_TIME_STAMP_KEY")
    object Notification : JetpackOverlayConnectedFeature("READER_OVERLAY_SHOWN_TIME_STAMP_KEY")
    object Reader : JetpackOverlayConnectedFeature("NOTIFICATIONS_OVERLAY_SHOWN_TIME_STAMP_KEY")

    fun getPreferenceKey(phase: JETPACKFEATUREOVERLAYPHASE): String {
        return featureSpecificPreferenceKey.plus(phase.preferenceKey)
    }
}

enum class JETPACKFEATUREOVERLAYPHASE(val preferenceKey: String) {
    PHASE_ONE("JETPACK_FEATURE_PHASE_ONE"),
    PHASE_TWO("JETPACK_FEATURE_PHASE_TWO"),
    PHASE_THREE("JETPACK_FEATURE_PHASE_THREE")
}
