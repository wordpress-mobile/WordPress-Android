package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val DOMAIN_MANAGEMENT_FEATURE_REMOTE_FIELD = "domain_management"

@Feature(DOMAIN_MANAGEMENT_FEATURE_REMOTE_FIELD, false)
class DomainManagementFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_DOMAIN_MANAGEMENT_FEATURE,
    DOMAIN_MANAGEMENT_FEATURE_REMOTE_FIELD,
)
