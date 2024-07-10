package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val IN_APP_UPDATE_BLOCKING_VERSION_DEFAULT = "0"

@RemoteFieldDefaultGenerater(
    remoteField = IN_APP_UPDATE_BLOCKING_VERSION_REMOTE_FIELD,
    defaultValue = IN_APP_UPDATE_BLOCKING_VERSION_DEFAULT
)

class InAppUpdateBlockingVersionConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<Int>(
        appConfig,
        IN_APP_UPDATE_BLOCKING_VERSION_REMOTE_FIELD
    )
