package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.TestRemoteFeatureConfig.Companion.TEST_REMOTE_CONFIG__TOOL_COLOR_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the test remote config
 */
@Feature(TEST_REMOTE_CONFIG__TOOL_COLOR_REMOTE_FIELD, true)
class TestRemoteFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.TEST_REMOTE_CONFIG_TOOL_COLOR,
        TEST_REMOTE_CONFIG__TOOL_COLOR_REMOTE_FIELD
){
    companion object {
        const val TEST_REMOTE_CONFIG__TOOL_COLOR_REMOTE_FIELD = "wordpress_ios_unified_login_and_signup"
    }
}
