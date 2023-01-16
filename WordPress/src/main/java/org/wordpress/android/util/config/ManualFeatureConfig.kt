package org.wordpress.android.util.config

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import javax.inject.Inject

class ManualFeatureConfig
@Inject constructor(private val appPrefsWrapper: AppPrefsWrapper, private val buildConfigWrapper: BuildConfigWrapper) {
    fun hasManualSetup(feature: FeatureConfig): Boolean =
        isConfigEnabled() && hasManualSetup(feature.toFeatureKey())

    fun hasManualSetup(featureKey: String): Boolean =
        isConfigEnabled() && appPrefsWrapper.hasManualFeatureConfig(featureKey)

    fun isManuallyEnabled(feature: FeatureConfig): Boolean =
        isConfigEnabled() && isManuallyEnabled(feature.toFeatureKey())

    fun isManuallyEnabled(featureKey: String): Boolean =
        isConfigEnabled() && appPrefsWrapper.getManualFeatureConfig(featureKey)

    fun setManuallyEnabled(featureKey: String, enabled: Boolean) {
        if (isConfigEnabled()) {
            appPrefsWrapper.setManualFeatureConfig(enabled, featureKey)
        }
    }

    private fun isConfigEnabled() = buildConfigWrapper.isDebugSettingsEnabled()

    private fun FeatureConfig.toFeatureKey() = this.remoteField ?: this.javaClass.toString().split(".").last()
}
