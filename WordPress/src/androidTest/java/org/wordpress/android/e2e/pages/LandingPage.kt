package org.wordpress.android.e2e.pages

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.wordpress.android.R
import org.wordpress.android.support.ComposeEspressoLink
import org.wordpress.android.support.WPSupportUtils.getTranslatedString
import org.wordpress.android.ui.compose.TestTags

object LandingPage {
    @JvmStatic
    fun tapContinueWithWpCom(composeTestRule: ComposeTestRule) {
        // New UI - See LoginPrologueRevampedFragment
        composeTestRule
            .onNodeWithTag(TestTags.BUTTON_WPCOM_AUTH)
            .performClick()

        ComposeEspressoLink().unregister()
    }

    @JvmStatic
    fun tapEnterYourSiteAddress(composeTestRule: ComposeTestRule) {
        // New UI - See LoginPrologueRevampedFragment
        composeTestRule
            .onNodeWithText(getTranslatedString(R.string.enter_your_site_address))
            .performClick()
    }
}
