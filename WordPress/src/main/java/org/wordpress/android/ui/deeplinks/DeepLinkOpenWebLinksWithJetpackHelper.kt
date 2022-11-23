package org.wordpress.android.ui.deeplinks

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.FirebaseRemoteConfigWrapper
import org.wordpress.android.util.PackageManagerWrapper
import org.wordpress.android.util.config.OpenWebLinksWithJetpackFlowFeatureConfig
import java.util.Date
import javax.inject.Inject

class DeepLinkOpenWebLinksWithJetpackHelper @Inject constructor(
    private val openWebLinksWithJetpackFlowFeatureConfig: OpenWebLinksWithJetpackFlowFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val firebaseRemoteConfigWrapper: FirebaseRemoteConfigWrapper,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun shouldShowDeepLinkOpenWebLinksWithJetpackOverlay() = showOverlay()

    fun shouldShowAppSetting(): Boolean {
        return openWebLinksWithJetpackFlowFeatureConfig.isEnabled()
                && isJetpackInstalled()
    }

    private fun showOverlay() : Boolean {
        return openWebLinksWithJetpackFlowFeatureConfig.isEnabled()
                && isJetpackInstalled()
                && isWebDeepLinkHandlerComponentEnabled()
                && isValidOverlayFrequency()
    }

    private fun isJetpackInstalled() = packageManagerWrapper.isPackageInstalled(getPackageName())

    private fun isWebDeepLinkHandlerComponentEnabled() =
        packageManagerWrapper.isComponentEnabledSettingEnabled(DeepLinkingIntentReceiverActivity::class.java)

    private fun isValidOverlayFrequency() : Boolean {
        if (!hasOverlayBeenShown()) return true // short circuit if the overlay has never been shown

        return hasExceededOverlayFrequency()
    }

    private fun hasExceededOverlayFrequency() : Boolean {
        val frequency = firebaseRemoteConfigWrapper.getOpenWebLinksWithJetpackFlowFrequency()
        if (frequency == 0L) return false // Only show the overlay 1X and it's already been shown do not show again

        val lastShownDate = Date(getOpenWebLinksWithJetpackOverlayLastShownTimestamp())
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
                lastShownDate,
                getTodaysDate())
        return daysPastOverlayShown >= frequency
    }

    private fun hasOverlayBeenShown() = getOpenWebLinksWithJetpackOverlayLastShownTimestamp() > 0L

    private fun getOpenWebLinksWithJetpackOverlayLastShownTimestamp() =
            appPrefsWrapper.getOpenWebLinksWithJetpackOverlayLastShownTimestamp()

    private fun getTodaysDate() = Date(System.currentTimeMillis())

    private fun getPackageName(): String {
        val appSuffix = buildConfigWrapper.getApplicationId().split(".").last()
        val appPackage = if (appSuffix.isNotBlank()) {
            "$JETPACK_PACKAGE_NAME.${appSuffix}"
        } else {
            JETPACK_PACKAGE_NAME
        }
        return appPackage
    }

    companion object {
        const val JETPACK_PACKAGE_NAME = "com.jetpack.android"
    }
}
