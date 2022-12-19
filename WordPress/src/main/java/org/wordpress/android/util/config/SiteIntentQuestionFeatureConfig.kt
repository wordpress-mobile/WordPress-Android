package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Site Intent Question step in the Site Creation flow
 */
@FeatureInDevelopment
class SiteIntentQuestionFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.SITE_INTENT_QUESTION)
