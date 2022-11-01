package org.wordpress.android.ui.prefs.accountsettings

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.SETTINGS_DID_CHANGE
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.EMAIL_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.PASSWORD_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.PRIMARY_SITE_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.USERNAME_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.USERNAME_CHANGE_SCREEN_DISMISSED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.USERNAME_CHANGE_SCREEN_DISPLAYED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.WEB_ADDRESS_CHANGED
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

private const val SOURCE = "source"
private const val SOURCE_ACCOUNT_SETTINGS = "account_settings"
private const val TRACK_PROPERTY_FIELD_NAME = "field_name"
private const val TRACK_PROPERTY_PAGE = "page"
private const val TRACK_PROPERTY_PAGE_ACCOUNT_SETTINGS = "account_settings"

enum class AccountSettingsEvent(val trackProperty: String? = null) {
    EMAIL_CHANGED("email"),
    PRIMARY_SITE_CHANGED("primary_site"),
    WEB_ADDRESS_CHANGED("web_address"),
    PASSWORD_CHANGED("password"),
    USERNAME_CHANGED("username"),
    USERNAME_CHANGE_SCREEN_DISPLAYED,
    USERNAME_CHANGE_SCREEN_DISMISSED
}

class AccountSettingsAnalyticsTracker @Inject constructor(private val analyticsTracker: AnalyticsTrackerWrapper) {
    fun track(action: AccountSettingsEvent) {
        when (action) {
            EMAIL_CHANGED, PRIMARY_SITE_CHANGED, WEB_ADDRESS_CHANGED, PASSWORD_CHANGED, USERNAME_CHANGED
            -> action.trackProperty?.let { trackSettingsDidChange(it) }
            USERNAME_CHANGE_SCREEN_DISMISSED -> trackUserNameChangeScreen(Stat.CHANGE_USERNAME_DISMISSED)
            USERNAME_CHANGE_SCREEN_DISPLAYED -> trackUserNameChangeScreen(Stat.CHANGE_USERNAME_DISPLAYED)
        }
    }

    private fun trackSettingsDidChange(fieldName: String) {
        val props = mutableMapOf<String, String?>()
        props[TRACK_PROPERTY_FIELD_NAME] = fieldName
        props[TRACK_PROPERTY_PAGE] = TRACK_PROPERTY_PAGE_ACCOUNT_SETTINGS
        analyticsTracker.track(SETTINGS_DID_CHANGE, props)
    }

    private fun trackUserNameChangeScreen(stat: Stat) {
        val props = mutableMapOf<String, String?>()
        props[SOURCE] = SOURCE_ACCOUNT_SETTINGS
        analyticsTracker.track(stat, props)
    }
}
