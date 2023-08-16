package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

@FeatureInDevelopment
class SiteCreationDomainPurchasingFeatureConfig
@Inject constructor(
    appConfig: AppConfig,
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_SITE_CREATION_DOMAIN_PURCHASING,
)
