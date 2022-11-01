package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Site Name step in the Site Creation flow
 */
@FeatureInDevelopment
class BetaSiteDesignsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.BETA_SITE_DESIGNS)
