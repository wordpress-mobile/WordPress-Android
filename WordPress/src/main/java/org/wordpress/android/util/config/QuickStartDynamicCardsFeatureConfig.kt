package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Quick Start Dynamic cards within the My Site improvements
 */
@FeatureInDevelopment
class QuickStartDynamicCardsFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.QUICK_START_DYNAMIC_CARDS
)
