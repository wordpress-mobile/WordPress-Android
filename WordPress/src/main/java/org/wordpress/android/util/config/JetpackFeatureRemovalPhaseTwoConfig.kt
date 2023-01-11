package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseTwoConfig.Companion.JETPACK_FEATURE_REMOVAL_PHASE_TWO_REMOTE_FIELD
import javax.inject.Inject

@Feature(JETPACK_FEATURE_REMOVAL_PHASE_TWO_REMOTE_FIELD, false)
class JetpackFeatureRemovalPhaseTwoConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.JETPACK_FEATURE_REMOVAL_PHASE_TWO,
    JETPACK_FEATURE_REMOVAL_PHASE_TWO_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_FEATURE_REMOVAL_PHASE_TWO_REMOTE_FIELD = "jp_removal_two"
    }
}
