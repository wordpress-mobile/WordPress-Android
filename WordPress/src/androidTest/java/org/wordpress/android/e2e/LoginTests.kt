package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.wordpress.android.e2e.flows.LoginFlow
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.ComposeEspressoLink
import org.wordpress.android.support.E2ECredentials

@HiltAndroidTest
class LoginTests : BaseTest() {
    @Before
    fun setUp() {
        ComposeEspressoLink().unregister()
        logoutIfNecessary()
    }

    @Test
    fun e2eLoginWithEmailPassword() {
        LoginFlow().chooseContinueWithWpCom(super.mComposeTestRule)
            .enterEmailAddress(E2ECredentials.WP_COM_USER_EMAIL)
            .enterPassword(E2ECredentials.WP_COM_USER_PASSWORD)
            .confirmLogin()

        ComposeEspressoLink().unregister()
    }

    @Test
    fun e2eLoginWithPasswordlessAccount() {
        LoginFlow().chooseContinueWithWpCom(super.mComposeTestRule)
            .enterEmailAddress(E2ECredentials.WP_COM_PASSWORDLESS_USER_EMAIL)
            .openMagicLink()
            .confirmLogin()

        ComposeEspressoLink().unregister()
    }

    @Test
    fun e2eLoginWithSiteAddress() {
        LoginFlow().chooseEnterYourSiteAddress(super.mComposeTestRule)
            .enterSiteAddress(E2ECredentials.WP_COM_USER_SITE_ADDRESS)
            .enterEmailAddress(E2ECredentials.WP_COM_USER_EMAIL)
            .enterPassword(E2ECredentials.WP_COM_USER_PASSWORD)
            .confirmLogin()

        ComposeEspressoLink().unregister()
    }

    @Test
    fun e2eLoginWithMagicLink() {
        try {
            LoginFlow().chooseContinueWithWpCom(super.mComposeTestRule)
                .enterEmailAddress(E2ECredentials.WP_COM_USER_EMAIL)
                .chooseMagicLink()
                .openMagicLink()
                .confirmLogin()

            ComposeEspressoLink().unregister()
        } finally {
            logoutIfNecessary()
        }
    }

    @Test
    fun e2eLoginWithSelfHostedAccount() {
        LoginFlow().chooseEnterYourSiteAddress(super.mComposeTestRule)
            .enterSiteAddress(E2ECredentials.SELF_HOSTED_USER_SITE_ADDRESS)
            .enterUsernameAndPassword(
                E2ECredentials.SELF_HOSTED_USER_USERNAME,
                E2ECredentials.SELF_HOSTED_USER_PASSWORD
            )
            .confirmLogin()

        ComposeEspressoLink().unregister()
    }
}
