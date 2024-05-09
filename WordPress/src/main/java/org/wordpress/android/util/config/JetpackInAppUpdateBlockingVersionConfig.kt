package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val JETPACK_IN_APP_UPDATE_BLOCKING_VERSION_REMOTE_FIELD = "jp_in_app_update_blocking_version_android"
const val JETPACK_IN_APP_UPDATE_BLOCKING_VERSION_DEFAULT = "0"

@RemoteFieldDefaultGenerater(
    remoteField = JETPACK_IN_APP_UPDATE_BLOCKING_VERSION_REMOTE_FIELD,
    defaultValue = JETPACK_IN_APP_UPDATE_BLOCKING_VERSION_DEFAULT
)

class JetpackInAppUpdateBlockingVersionConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<Int>(
        appConfig,
        JETPACK_IN_APP_UPDATE_BLOCKING_VERSION_REMOTE_FIELD
    )
