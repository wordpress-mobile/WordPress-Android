package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val READER_DISCOVER_NEW_ENDPOINT_REMOTE_FIELD = "reader_discover_new_endpoint"

@Feature(
    remoteField = READER_DISCOVER_NEW_ENDPOINT_REMOTE_FIELD,
    defaultValue = true,
)
class ReaderDiscoverNewEndpointFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.READER_DISCOVER_NEW_ENDPOINT,
    READER_DISCOVER_NEW_ENDPOINT_REMOTE_FIELD
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.IS_JETPACK_APP
    }
}
