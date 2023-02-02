package org.wordpress.android.e2e.pages

import androidx.compose.ui.test.junit4.ComposeTestRule
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils.clickOn
import org.wordpress.android.support.WPSupportUtils.getTranslatedString
import org.wordpress.android.ui.pages.LoginPage
import org.wordpress.android.util.compose.ComposeUiTestingUtils

object LandingPage {
    @JvmStatic
    fun tapContinueWithWpCom(composeTestRule: ComposeTestRule) {
        if (BuildConfig.IS_JETPACK_APP) {
            // See LoginPrologueRevampedFragment
            tapContinueWithWpComOnRevampedLandingScreen(composeTestRule)
        } else {
            // See LoginPrologueFragment
            tapContinueWithWpComOnOldLandingScreen()
        }
    }

    private fun tapContinueWithWpComOnOldLandingScreen() {
        clickOn(R.id.continue_with_wpcom_button)
    }

    private fun tapContinueWithWpComOnRevampedLandingScreen(composeTestRule: ComposeTestRule) {
        ComposeUiTestingUtils(composeTestRule)
            .performClickOnNodeWithText(getTranslatedString(LoginPage.continueWithWpComButtonStringRes))
    }

    @JvmStatic
    fun tapEnterYourSiteAddress(composeTestRule: ComposeTestRule) {
        if (BuildConfig.IS_JETPACK_APP) {
            // See LoginPrologueRevampedFragment
            tapEnterYourSiteAddressOnRevampedUi(composeTestRule)
        } else {
            // See LoginPrologueFragment
            tapEnterYourSiteAddressOnOldUi()
        }
    }

    private fun tapEnterYourSiteAddressOnOldUi() {
        clickOn(R.id.enter_your_site_address_button)
    }

    private fun tapEnterYourSiteAddressOnRevampedUi(composeTestRule: ComposeTestRule) {
        ComposeUiTestingUtils(composeTestRule)
            .performClickOnNodeWithText(getTranslatedString(R.string.enter_your_site_address))
    }
}
