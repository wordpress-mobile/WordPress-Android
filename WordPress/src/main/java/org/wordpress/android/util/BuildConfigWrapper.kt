package org.wordpress.android.util

import org.wordpress.android.BuildConfig
import javax.inject.Inject

class BuildConfigWrapper @Inject constructor() {
    fun getAppVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    fun getAppVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    fun isDebug(): Boolean {
        return BuildConfig.DEBUG
    }

    fun getApplicationId(): String {
        return BuildConfig.APPLICATION_ID
    }

    fun isDebugSettingsEnabled(): Boolean = BuildConfig.ENABLE_DEBUG_SETTINGS

    val isJetpackApp = BuildConfig.IS_JETPACK_APP

    val isSiteCreationEnabled = BuildConfig.ENABLE_SITE_CREATION

    val isSignupEnabled = BuildConfig.ENABLE_SIGNUP

    val isCreateFabEnabled = BuildConfig.ENABLE_CREATE_FAB

    val isQuickActionEnabled = BuildConfig.ENABLE_QUICK_ACTION

    val isFollowedSitesSettingsEnabled = BuildConfig.ENABLE_FOLLOWED_SITES_SETTINGS

    val isWhatsNewFeatureEnabled = BuildConfig.ENABLE_WHATS_NEW_FEATURE

    val isMySiteTabsEnabled = BuildConfig.ENABLE_MY_SITE_DASHBOARD_TABS
}
