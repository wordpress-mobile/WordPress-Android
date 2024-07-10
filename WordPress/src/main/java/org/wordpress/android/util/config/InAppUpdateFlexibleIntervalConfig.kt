package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val IN_APP_UPDATE_FLEXIBLE_INTERVAL_REMOTE_FIELD = "in_app_update_flexible_interval_in_days_android"
const val IN_APP_UPDATE_FLEXIBLE_INTERVAL_DEFAULT = "5"

@RemoteFieldDefaultGenerater(
    remoteField = IN_APP_UPDATE_FLEXIBLE_INTERVAL_REMOTE_FIELD,
    defaultValue = IN_APP_UPDATE_FLEXIBLE_INTERVAL_DEFAULT
)

class InAppUpdateFlexibleIntervalConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<Int>(
        appConfig,
        IN_APP_UPDATE_FLEXIBLE_INTERVAL_REMOTE_FIELD
    )
