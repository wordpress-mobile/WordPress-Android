package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val CODEABLE_GET_FREE_ESTIMATE_URL_REMOTE_FIELD = "codeable_get_free_estimate_url"
const val CODEABLE_GET_FREE_ESTIMATE_URL_DEFAULT = "https://codeable.io/partners/jetpack-scan/"

@RemoteFieldDefaultGenerater(
    remoteField = CODEABLE_GET_FREE_ESTIMATE_URL_REMOTE_FIELD,
    defaultValue = CODEABLE_GET_FREE_ESTIMATE_URL_DEFAULT
)

class CodeableGetFreeEstimateUrlConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        CODEABLE_GET_FREE_ESTIMATE_URL_REMOTE_FIELD
    )
