package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BuildConfig
import org.wordpress.android.e2e.flows.LoginFlow
import org.wordpress.android.e2e.pages.ContactSupportScreen
import org.wordpress.android.e2e.pages.HelpScreen
import org.wordpress.android.support.BaseTest

@HiltAndroidTest
class ContactUsTests : BaseTest() {
    @Before
    fun setUp() {
        logoutIfNecessary()
    }

    @Test
    fun e2eSendButtonEnabledWhenTextIsEntered() {
        try {
            LoginFlow()
                .chooseContinueWithWpCom(super.mComposeTestRule)
                .tapHelp()
                .assertHelpScreenLoaded()

            // Contact Us is only available on Jetpack App, see: https://github.com/wordpress-mobile/WordPress-Android/pull/18818
            if (BuildConfig.IS_JETPACK_APP) {
                HelpScreen().openContactUs()
                    .assertContactSupportScreenLoaded()
                    .assertSendButtonDisabled()
                    .setMessageText("Hello")
                    .assertSendButtonEnabled()
                    .setMessageText("")
                    .assertSendButtonDisabled()
            }
        } finally {
            ContactSupportScreen().goBackAndDeleteUnsentMessageIfNeeded()
        }
    }

    @Test
    fun e2eHelpCanBeOpenedWhileEnteringEmail() {
        if (BuildConfig.IS_JETPACK_APP) {
            LoginFlow()
                .chooseContinueWithWpCom(super.mComposeTestRule)
                .tapHelp()
                .assertHelpScreenLoaded()
        }
    }

    @Test
    fun e2eHelpCanBeOpenedWhileEnteringPassword() {
        if (BuildConfig.IS_JETPACK_APP) {
            LoginFlow()
                .chooseContinueWithWpCom(super.mComposeTestRule)
                .enterEmailAddress(BuildConfig.E2E_WP_COM_USER_EMAIL)
                .tapHelp()
                .assertHelpScreenLoaded()
        }
    }
}
