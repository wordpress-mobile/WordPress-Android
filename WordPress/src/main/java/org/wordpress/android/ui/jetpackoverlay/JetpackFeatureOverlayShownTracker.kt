package org.wordpress.android.ui.jetpackoverlay

import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject

const val STATS_OVERLAY_SHOWN_TIMESTAMP_KEY = "JETPACK_FEATURE_STATS_ACCESSED_TIME_KEY"
const val READER_OVERLAY_SHOWN_TIMESTAMP_KEY = "JETPACK_FEATURE_READER_ACCESSED_TIME_KEY"
const val NOTIFICATIONS_OVERLAY_SHOWN_TIMESTAMP_KEY = "JETPACK_FEATURE_NOTIFICATIONS_ACCESSED_TIME_KEY"

class JetpackFeatureOverlayShownTracker @Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun getEarliestOverlayShownTime(): Long {
        val overlayShownTimeStampList: ArrayList<Long> = arrayListOf()
        JetpackOverlayConnectedFeature.values().forEach {
            Log.e("feature", it.name)
            overlayShownTimeStampList.add(getFeatureOverlayShownTimeStamp(it))
        }
        Log.e("timeStamp list", overlayShownTimeStampList.sortedDescending().toString())
        return overlayShownTimeStampList.minOf { it }
    }

    fun clear() {
        JetpackOverlayConnectedFeature.values().forEach {
            Log.e("feature", it.name)
            sharedPrefs.edit().remove(it.preferenceKey).commit()
        }
    }

    fun getFeatureOverlayShownTimeStamp(jetpackOverlayConnectedFeature: JetpackOverlayConnectedFeature): Long {
        return sharedPrefs.getLong(jetpackOverlayConnectedFeature.preferenceKey, -1)
    }

    fun setFeatureOverlayShownTimeStamp(jetpackOverlayConnectedFeature: JetpackOverlayConnectedFeature) {
        sharedPrefs.edit().putLong(jetpackOverlayConnectedFeature.preferenceKey, System.currentTimeMillis()).apply()
    }

    fun setFeatureOverlayShownTimeStamp(
        jetpackOverlayConnectedFeature: JetpackOverlayConnectedFeature,
        timeStamp: Long
    ) {
        sharedPrefs.edit().putLong(jetpackOverlayConnectedFeature.preferenceKey, timeStamp).apply()
    }
}

enum class JetpackOverlayConnectedFeature(val preferenceKey: String) {
    STATS(STATS_OVERLAY_SHOWN_TIMESTAMP_KEY),
    NOTIFICATIONS(READER_OVERLAY_SHOWN_TIMESTAMP_KEY),
    READER(NOTIFICATIONS_OVERLAY_SHOWN_TIMESTAMP_KEY)
}
