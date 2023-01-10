package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.JetpackLocalUserFlagsFeatureConfig.Companion.JETPACK_LOCAL_USER_FLAGS_REMOTE_FIELD
import javax.inject.Inject

@Feature(JETPACK_LOCAL_USER_FLAGS_REMOTE_FIELD, false)
class JetpackLocalUserFlagsFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.JETPACK_LOCAL_USER_FLAGS,
    JETPACK_LOCAL_USER_FLAGS_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_LOCAL_USER_FLAGS_REMOTE_FIELD = "jetpack_local_user_flags_remote_field"
    }
}
