package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.JetpackSharedLoginFeatureConfig.Companion.JETPACK_SHARED_LOGIN_REMOTE_FIELD
import javax.inject.Inject

@Feature(JETPACK_SHARED_LOGIN_REMOTE_FIELD, false)
class JetpackSharedLoginFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.JETPACK_SHARED_LOGIN,
        JETPACK_SHARED_LOGIN_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_SHARED_LOGIN_REMOTE_FIELD = "jetpack_shared_login_remote_field"
    }
}
