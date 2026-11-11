package org.wordpress.android.e2e.flows

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import org.wordpress.android.R
import org.wordpress.android.e2e.pages.HelpScreen
import org.wordpress.android.e2e.pages.LandingPage.tapContinueWithWpCom
import org.wordpress.android.e2e.pages.LandingPage.tapEnterYourSiteAddress
import org.wordpress.android.support.WPSupportUtils

class LoginFlow {
    fun chooseContinueWithWpCom(composeTestRule: ComposeTestRule?): LoginFlow {
        // Login Prologue – We want to Continue with WordPress.com, not a site address
        // Note: WP.com login now uses web-based OAuth flow via Custom Tabs
        tapContinueWithWpCom(composeTestRule!!)
        return this
    }

    fun chooseEnterYourSiteAddress(composeTestRule: ComposeTestRule?): LoginFlow {
        // Login Prologue – We want to continue with a site address not a WordPress.com account
        tapEnterYourSiteAddress(composeTestRule!!)
        return this
    }

    fun tapHelp(): HelpScreen {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withId(R.id.help)))
        return HelpScreen()
    }
}
