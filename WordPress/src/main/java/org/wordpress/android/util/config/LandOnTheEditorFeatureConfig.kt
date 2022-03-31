package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration for landing on the editor at the end of the Site Creation flow
 */
@FeatureInDevelopment
class LandOnTheEditorFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.LAND_ON_THE_EDITOR
)
