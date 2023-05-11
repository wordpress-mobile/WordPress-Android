package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

class MePage {
    fun go(): MePage {
        // Using the settings button as a marker for successfully navigating to the page
        while (!WPSupportUtils.isElementClickable(appSettings)) {
            WPSupportUtils.clickOn(R.id.nav_sites)
            WPSupportUtils.clickOn(R.id.me_item)
        }
        if (!isSelfHosted) {
            displayName.check(ViewAssertions.matches(ViewMatchers.isClickable()))
        }
        return this
    }

    fun goBack() {
        Espresso.pressBack()
    }

    fun verifyUsername(username: String): MePage {
        val atUsername = "@$username"
        usernameLabel.check(ViewAssertions.matches(ViewMatchers.withText(atUsername)))
        return this
    }

    val isSelfHosted: Boolean
        get() {
            return WPSupportUtils.isElementClickable(Espresso.onView(ViewMatchers.withText(R.string.sign_in_wpcom)))
        }

    fun openAppSettings() {
        appSettings.perform(ViewActions.click())
    }

    fun logout() {
        val logOutButton = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.me_login_logout_text_view),
                ViewMatchers.withText(
                    WPSupportUtils.getCurrentActivity()
                        .getString(R.string.me_disconnect_from_wordpress_com)
                )
            )
        )
        while (!WPSupportUtils.isElementClickable(android.R.id.button1)) {
            WPSupportUtils.scrollToThenClickOn(logOutButton)
        }
        WPSupportUtils.clickOn(android.R.id.button1)
    }

    companion object {
        // Labels
        private val displayName = Espresso.onView(ViewMatchers.withId(R.id.me_display_name))
        private val usernameLabel = Espresso.onView(ViewMatchers.withId(R.id.me_username))

        // Buttons
        private val appSettings = Espresso.onView(ViewMatchers.withId(R.id.row_app_settings))
    }
}
