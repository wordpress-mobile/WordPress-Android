package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.JetpackInstallFullPluginFeatureConfig.Companion.JETPACK_INSTALL_FULL_PLUGIN_REMOTE_FIELD
import javax.inject.Inject

@Feature(JETPACK_INSTALL_FULL_PLUGIN_REMOTE_FIELD, true)
class JetpackInstallFullPluginFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.JETPACK_INSTALL_FULL_PLUGIN,
    JETPACK_INSTALL_FULL_PLUGIN_REMOTE_FIELD
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.IS_JETPACK_APP
    }

    companion object {
        const val JETPACK_INSTALL_FULL_PLUGIN_REMOTE_FIELD = "jetpack_install_full_plugin_remote_field"
    }
}
