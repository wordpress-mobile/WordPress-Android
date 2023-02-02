package org.wordpress.android.e2e.pages

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils.clickOn
import org.wordpress.android.support.WPSupportUtils.getTranslatedString
import org.wordpress.android.ui.pages.LoginPage

object LandingPage {
    @JvmStatic
    fun tapContinueWithWpCom(composeTestRule: ComposeTestRule) {
        if (BuildConfig.IS_JETPACK_APP) {
            // New UI - See LoginPrologueRevampedFragment
            composeTestRule
                .onNodeWithText(getTranslatedString(LoginPage.continueWithWpComButtonStringRes))
                .performClick()
        } else {
            // Old UI - See LoginPrologueFragment
            clickOn(R.id.continue_with_wpcom_button)
        }
    }

    @JvmStatic
    fun tapEnterYourSiteAddress(composeTestRule: ComposeTestRule) {
        if (BuildConfig.IS_JETPACK_APP) {
            // New UI - See LoginPrologueRevampedFragment
            composeTestRule
                .onNodeWithText(getTranslatedString(R.string.enter_your_site_address))
                .performClick()
        } else {
            // Old UI - See LoginPrologueFragment
            clickOn(R.id.enter_your_site_address_button)
        }
    }
}
