package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val PLANS_IN_SITE_CREATION_REMOTE_FIELD = "plans_in_site_creation"

@Feature(PLANS_IN_SITE_CREATION_REMOTE_FIELD, false)
class PlansInSiteCreationFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.PLANS_IN_SITE_CREATION,
    PLANS_IN_SITE_CREATION_REMOTE_FIELD
)
