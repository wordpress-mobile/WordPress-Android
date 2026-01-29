package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

/**
 * Configuration for the Navigation Menus feature for self-hosted sites
 */
private const val NAV_MENUS_REMOTE_FIELD = "nav_menus"

@Feature(NAV_MENUS_REMOTE_FIELD, false)
class NavMenusFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_NAV_MENUS,
    NAV_MENUS_REMOTE_FIELD
)
