package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val WP_IN_APP_UPDATE_BLOCKING_VERSION_REMOTE_FIELD = "wp_in_app_update_blocking_version_android"
const val WP_IN_APP_UPDATE_BLOCKING_VERSION_DEFAULT = "0"

@RemoteFieldDefaultGenerater(
    remoteField = WP_IN_APP_UPDATE_BLOCKING_VERSION_REMOTE_FIELD,
    defaultValue = WP_IN_APP_UPDATE_BLOCKING_VERSION_DEFAULT
)

class WordPressInAppUpdateBlockingVersionConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<Int>(
        appConfig,
        WP_IN_APP_UPDATE_BLOCKING_VERSION_REMOTE_FIELD
    )
