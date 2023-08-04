package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val JETPACK_SOCIAL_REMOTE_FIELD = "jetpack_social_improvements_v1"

@Feature(JETPACK_SOCIAL_REMOTE_FIELD, defaultValue = true)
class JetpackSocialFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.JETPACK_SOCIAL,
    JETPACK_SOCIAL_REMOTE_FIELD,
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.IS_JETPACK_APP
    }
}
