package org.wordpress.android.ui.deeplinks

import android.content.pm.PackageManager
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.FirebaseRemoteConfigWrapper
import org.wordpress.android.util.PackageManagerWrapper
import org.wordpress.android.util.config.OpenWebLinksWithJetpackFlowFeatureConfig
import java.util.Date
import javax.inject.Inject

@Suppress("TooManyFunctions")
class DeepLinkOpenWebLinksWithJetpackHelper @Inject constructor(
    private val openWebLinksWithJetpackFlowFeatureConfig: OpenWebLinksWithJetpackFlowFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val firebaseRemoteConfigWrapper: FirebaseRemoteConfigWrapper,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun shouldShowOpenLinksInJetpackOverlay() = showOverlay()

    fun shouldShowAppSetting(): Boolean {
        return openWebLinksWithJetpackFlowFeatureConfig.isEnabled()
                && isJetpackInstalled()
    }

    fun enableDisableOpenWithJetpackComponents(newValue: Boolean) {
        when (newValue) {
            true -> {
                packageManagerWrapper.disableReaderDeepLinks()
                packageManagerWrapper.disableComponentEnabledSetting(WEB_LINKS_DEEPLINK_ACTIVITY_ALIAS)
            }
            false -> {
                packageManagerWrapper.enableReaderDeeplinks()
                packageManagerWrapper.enableComponentEnableSetting(WEB_LINKS_DEEPLINK_ACTIVITY_ALIAS)
            }
        }
    }

    fun handleJetpackUninstalled() {
        resetAll()
    }

    fun resetAll() {
        enableDisableOpenWithJetpackComponents(false)
        appPrefsWrapper.setIsOpenWebLinksWithJetpack(false)
        appPrefsWrapper.setOpenWebLinksWithJetpackOverlayLastShownTimestamp(0L)
    }

    @Suppress("SwallowedException")
    fun handleOpenWebLinksWithJetpack() : Boolean {
        try {
            enableDisableOpenWithJetpackComponents(true)
            packageManagerWrapper.disableReaderDeepLinks()
            appPrefsWrapper.setIsOpenWebLinksWithJetpack(true)
            return true
        } catch (ex: PackageManager.NameNotFoundException) {
            AppLog.e(T.UTILS, "Unable to set open web links with Jetpack ${ex.message}")
        }
        return false
    }

    private fun showOverlay() : Boolean {
        return openWebLinksWithJetpackFlowFeatureConfig.isEnabled()
                && isJetpackInstalled()
                && !isOpenWebLinksWithJetpack()
                && isValidOverlayFrequency()
    }

    private fun isJetpackInstalled() = packageManagerWrapper.isPackageInstalled(getPackageName())

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

    private fun isOpenWebLinksWithJetpack() = appPrefsWrapper.getIsOpenWebLinksWithJetpack()

    private fun getTodaysDate() = Date(System.currentTimeMillis())

    private fun getPackageName(): String {
        val appSuffix = buildConfigWrapper.getApplicationId().split(".").last()
        val appPackage = if (appSuffix.isNotBlank() && !appSuffix.equals("ANDROID", ignoreCase = true)) {
            "$JETPACK_PACKAGE_NAME.${appSuffix}"
        } else {
            JETPACK_PACKAGE_NAME
        }
        return appPackage
    }

    fun onOverlayShown() =
            appPrefsWrapper.setOpenWebLinksWithJetpackOverlayLastShownTimestamp(System.currentTimeMillis())


    companion object {
        const val JETPACK_PACKAGE_NAME = "com.jetpack.android"
        const val WEB_LINKS_DEEPLINK_ACTIVITY_ALIAS =
                "org.wordpress.android.WebLinksDeepLinkingIntentReceiverActivity"
    }
}
