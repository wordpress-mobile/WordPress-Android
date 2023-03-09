package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val APP_ICON_SETTING_REMOTE_FIELD = "app_icon_setting"

@Feature(APP_ICON_SETTING_REMOTE_FIELD, false)
class AppIconSettingFeatureConfig @Inject constructor(
    appConfig: AppConfig,
) : FeatureConfig(
    appConfig,
    BuildConfig.APP_ICON_SETTING,
    APP_ICON_SETTING_REMOTE_FIELD,
)
