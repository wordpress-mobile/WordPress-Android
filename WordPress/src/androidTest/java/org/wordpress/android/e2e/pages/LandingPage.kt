package org.wordpress.android.e2e.pages

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.support.ComposeEspressoLink
import org.wordpress.android.support.WPSupportUtils.clickOn
import org.wordpress.android.support.WPSupportUtils.getTranslatedString
import org.wordpress.android.ui.compose.TestTags

object LandingPage {
    private const val isNewUiEnabled = BuildConfig.IS_JETPACK_APP || BuildConfig.LANDING_SCREEN_REVAMP

    @JvmStatic
    fun tapContinueWithWpCom(composeTestRule: ComposeTestRule) {
        if (isNewUiEnabled) {
            // New UI - See LoginPrologueRevampedFragment
            composeTestRule
                .onNodeWithTag(TestTags.BUTTON_WPCOM_AUTH)
                .performClick()
        } else {
            // Old UI - See LoginPrologueFragment
            clickOn(R.id.continue_with_wpcom_button)
        }

        ComposeEspressoLink().unregister()
    }

    @JvmStatic
    fun tapEnterYourSiteAddress(composeTestRule: ComposeTestRule) {
        if (isNewUiEnabled) {
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
