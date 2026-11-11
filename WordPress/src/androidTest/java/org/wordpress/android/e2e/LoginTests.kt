package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.wordpress.android.e2e.flows.LoginFlow
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.ComposeEspressoLink

@HiltAndroidTest
class LoginTests : BaseTest() {
    @Before
    fun setUp() {
        ComposeEspressoLink().unregister()
        logoutIfNecessary()
    }

    @Test
    fun e2eLoginWithSiteAddress() {
        // Self-hosted login now uses application passwords via Custom Tabs,
        // so this test only verifies navigation to the site address screen
        LoginFlow().chooseEnterYourSiteAddress(super.mComposeTestRule)

        ComposeEspressoLink().unregister()
    }
}
