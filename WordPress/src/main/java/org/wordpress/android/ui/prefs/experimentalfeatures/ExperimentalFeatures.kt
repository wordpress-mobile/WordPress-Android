package org.wordpress.android.ui.prefs.experimentalfeatures

import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

class ExperimentalFeatures @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper
) {
    fun isEnabled(feature: Feature): Boolean {
        // Experimental subscribers feature is only available in the Jetpack app but was originally available
        // in the WordPress app, so if this is the WordPress app make sure to disable it. This can be dropped
        // a few releases down the road (code written July 17, 2025)
        if (feature == Feature.EXPERIMENTAL_SUBSCRIBERS_FEATURE &&
            !BuildConfig.IS_JETPACK_APP &&
            appPrefsWrapper.getExperimentalFeatureConfig(feature.prefKey)
        ) {
            setEnabled(feature, false)
        }
        return appPrefsWrapper.getExperimentalFeatureConfig(feature.prefKey)
    }

    fun setEnabled(feature: Feature, isEnabled: Boolean) {
        appPrefsWrapper.setExperimentalFeatureConfig(isEnabled, feature.prefKey)
    }

    enum class Feature(
        val prefKey: String,
        val labelResId: Int,
        val descriptionResId: Int
    ) {
        DISABLE_EXPERIMENTAL_BLOCK_EDITOR(
            "disable_experimental_block_editor",
            R.string.disable_experimental_block_editor,
            R.string.disable_experimental_block_editor_description
        ),
        EXPERIMENTAL_BLOCK_EDITOR(
            "experimental_block_editor",
            R.string.experimental_block_editor,
            R.string.experimental_block_editor_description
        ),
        EXPERIMENTAL_BLOCK_EDITOR_THEME_STYLES(
            "experimental_block_editor_theme_styles",
            R.string.experimental_block_editor_theme_styles,
            R.string.experimental_block_editor_theme_styles_description
        ),
        EXPERIMENTAL_SUBSCRIBERS_FEATURE(
            "experimental_subscribers_feature",
            R.string.experimental_subscribers_feature,
            R.string.experimental_subscribers_feature_description
        ),
        EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE(
            "experimental_application_password_feature",
            R.string.experimental_application_password_feature,
            R.string.experimental_application_password_feature_description
        );
    }
}
