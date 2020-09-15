package org.wordpress.android.util.config.manual

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.FeatureConfig
import javax.inject.Inject

class ManualFeatureConfig
@Inject constructor(private val appPrefsWrapper: AppPrefsWrapper) {
    fun hasManualSetup(feature: FeatureConfig): Boolean = hasManualSetup(feature.toFeatureKey())
    fun hasManualSetup(featureKey: String): Boolean = appPrefsWrapper.hasManualFeatureConfig(featureKey)

    fun isManuallyEnabled(feature: FeatureConfig): Boolean =
            isManuallyEnabled(feature.toFeatureKey())

    fun isManuallyEnabled(featureKey: String): Boolean =
            appPrefsWrapper.getManualFeatureConfig(featureKey)

    fun setManuallyEnabled(featureKey: String, enabled: Boolean) =
            appPrefsWrapper.setManualFeatureConfig(enabled, featureKey)

    private fun FeatureConfig.toFeatureKey() = this.remoteField ?: this.javaClass.toString().split(".").last()
}
