package org.wordpress.android.ui.prefs.experimentalfeatures

import org.wordpress.android.R
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

class ExperimentalFeatures @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper
) {
    fun isEnabled(feature: Feature) : Boolean {
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
        EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE(
            "experimental_application_password_feature",
            R.string.experimental_application_password_feature,
            R.string.experimental_application_password_feature_description
        ),
        MODERN_SUPPORT(
            "modern_support",
            R.string.modern_support,
            R.string.modern_support_description
        );
    }
}
