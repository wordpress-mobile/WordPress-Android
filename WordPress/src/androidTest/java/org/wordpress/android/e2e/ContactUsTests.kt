package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.BuildConfig
import org.wordpress.android.e2e.flows.LoginFlow
import org.wordpress.android.e2e.pages.ContactSupportScreen
import org.wordpress.android.support.BaseTest

@HiltAndroidTest
class ContactUsTests : BaseTest() {
    @Before
    fun setUp() {
        logoutIfNecessary()
    }

    @Test
    @Ignore("This test is temporarily disabled as it takes too long to complete.")
    fun e2eSendButtonEnabledWhenTextIsEntered() {
        try {
            LoginFlow()
                .chooseContinueWithWpCom(super.mComposeTestRule)
                .tapHelp()
                .assertHelpScreenLoaded()
                .openContactUs()
                .assertContactSupportScreenLoaded()
                .assertSendButtonDisabled()
                .setMessageText("Hello")
                .assertSendButtonEnabled()
                .setMessageText("")
                .assertSendButtonDisabled()
        } finally {
            ContactSupportScreen().goBackAndDeleteUnsentMessageIfNeeded()
        }
    }

    @Test
    fun e2eHelpCanBeOpenedWhileEnteringEmail() {
        LoginFlow()
            .chooseContinueWithWpCom(super.mComposeTestRule)
            .tapHelp()
            .assertHelpScreenLoaded()
    }

    @Test
    fun e2eHelpCanBeOpenedWhileEnteringPassword() {
        LoginFlow()
            .chooseContinueWithWpCom(super.mComposeTestRule)
            .enterEmailAddress(BuildConfig.E2E_WP_COM_USER_EMAIL)
            .tapHelp()
            .assertHelpScreenLoaded()
    }
}
