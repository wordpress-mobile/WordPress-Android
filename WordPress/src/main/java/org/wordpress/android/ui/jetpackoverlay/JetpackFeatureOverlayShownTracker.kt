package org.wordpress.android.ui.jetpackoverlay

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackFeatureOverlayShownTracker @Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun setFeatureCollectionOverlayShown(phase: JetpackFeatureRemovalPhase) {
        sharedPrefs.edit { putBoolean(buildFeatureCollectionOverlayShownKey(phase), true) }
        if (phase == JetpackFeatureRemovalPhase.PhaseFour) {
            setPhaseFourOverlayShownTimeStamp(System.currentTimeMillis())
        }
    }

    fun getFeatureCollectionOverlayShown(phase: JetpackFeatureRemovalPhase) =
        sharedPrefs.getBoolean(buildFeatureCollectionOverlayShownKey(phase), false)

    private fun buildFeatureCollectionOverlayShownKey(phase: JetpackFeatureRemovalPhase) =
        KEY_FEATURE_COLLECTION_OVERLAY_SHOWN.plus(phase.trackingName)

    fun getPhaseFourOverlayShownTimeStamp(): Long? {
        val overlayShownTime = sharedPrefs.getLong(KEY_PHASE_FOUR_OVERLAY_SHOWN_TIME_STAMP, 0L)
        if (overlayShownTime == 0L) return null
        return overlayShownTime
    }

    private fun setPhaseFourOverlayShownTimeStamp(timeStamp: Long) {
        sharedPrefs.edit { putLong(KEY_PHASE_FOUR_OVERLAY_SHOWN_TIME_STAMP, timeStamp) }
    }

    companion object {
        const val KEY_FEATURE_COLLECTION_OVERLAY_SHOWN = "KEY_FEATURE_COLLECTION_OVERLAY_SHOWN"
        const val KEY_PHASE_FOUR_OVERLAY_SHOWN_TIME_STAMP = "KEY_PHASE_FOUR_OVERLAY_SHOWN_TIME_STAMP"
    }
}
