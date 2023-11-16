package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

private const val WP_INDIVIDUAL_PLUGIN_OVERLAY_MAX_SHOWN_REMOTE_FIELD = "wp_plugin_overlay_max_shown"
private const val WP_INDIVIDUAL_PLUGIN_OVERLAY_MAX_SHOWN_DEFAULT = 3

@RemoteFieldDefaultGenerater(
    remoteField = WP_INDIVIDUAL_PLUGIN_OVERLAY_MAX_SHOWN_REMOTE_FIELD,
    defaultValue = WP_INDIVIDUAL_PLUGIN_OVERLAY_MAX_SHOWN_DEFAULT.toString()
)
class WPIndividualPluginOverlayMaxShownConfig @Inject constructor(
    appConfig: AppConfig,
) : RemoteConfigField<Int>(
    appConfig = appConfig,
    remoteField = WP_INDIVIDUAL_PLUGIN_OVERLAY_MAX_SHOWN_REMOTE_FIELD
)
