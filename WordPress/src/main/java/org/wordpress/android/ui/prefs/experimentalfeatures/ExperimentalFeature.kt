package org.wordpress.android.ui.prefs.experimentalfeatures

import org.wordpress.android.R
import org.wordpress.android.ui.prefs.AppPrefs

enum class ExperimentalFeature(
    private val prefKey: String,
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
    );

    fun isEnabled() : Boolean {
        return AppPrefs.getExperimentalFeatureConfig(prefKey)
    }

    fun setEnabled(isEnabled: Boolean) {
        AppPrefs.setExperimentalFeatureConfig(isEnabled, prefKey)
    }
}
