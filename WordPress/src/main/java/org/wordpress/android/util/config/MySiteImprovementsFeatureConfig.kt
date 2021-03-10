package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig.Companion.MY_SITE_IMPROVEMENTS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the my site infrastructure improvements
 */
@Feature(remoteField = MY_SITE_IMPROVEMENTS_REMOTE_FIELD, defaultValue = true)
class MySiteImprovementsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_IMPROVEMENTS,
        MY_SITE_IMPROVEMENTS_REMOTE_FIELD
) {
    companion object {
        const val MY_SITE_IMPROVEMENTS_REMOTE_FIELD = "my_site_improvements_enabled"
    }
}
