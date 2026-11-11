package org.wordpress.android.ui.jetpackoverlay

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackFeatureOverlayShownTracker @Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun setFeatureCollectionOverlayShown() {
        sharedPrefs.edit { putBoolean(KEY_FEATURE_COLLECTION_OVERLAY_SHOWN_KEY, true) }
    }

    fun getFeatureCollectionOverlayShown() =
        sharedPrefs.getBoolean(KEY_FEATURE_COLLECTION_OVERLAY_SHOWN_KEY, false)

    companion object {
        // The phase name is part of the stored key; see JETPACK_REMOVAL_TRACKING_NAME.
        private const val KEY_FEATURE_COLLECTION_OVERLAY_SHOWN_KEY =
            "KEY_FEATURE_COLLECTION_OVERLAY_SHOWN" + JETPACK_REMOVAL_TRACKING_NAME
    }
}
