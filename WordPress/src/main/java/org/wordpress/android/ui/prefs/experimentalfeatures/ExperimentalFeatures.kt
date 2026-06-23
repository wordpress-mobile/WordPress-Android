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
        EXPERIMENTAL_BLOCK_EDITOR(
            "experimental_block_editor",
            R.string.experimental_block_editor,
            R.string.experimental_block_editor_description
        ),
        MODERN_SUPPORT(
            "modern_support",
            R.string.modern_support,
            R.string.modern_support_description
        ),
        NETWORK_DEBUGGING(
            "network_debugging",
            R.string.experimental_network_debugging,
            R.string.experimental_network_debugging_description
        ),
        EXPERIMENTAL_POST_TYPES(
            "experimental_post_types",
            R.string.experimental_post_types,
            R.string.experimental_post_types_description
        ),
        RS_PAGES_LIST(
            "rs_pages_list",
            R.string.experimental_rs_pages_list,
            R.string.experimental_rs_pages_list_description
        );
    }
}
