package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.BuildConfig
import org.wordpress.android.e2e.flows.LoginFlow
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.ComposeEspressoLink

@HiltAndroidTest
@Ignore("These tests will be re-enabled in a future PR")
class LoginTests : BaseTest() {
    @Before
    fun setUp() {
        ComposeEspressoLink().unregister()
        logoutIfNecessary()
    }

    @Test
    fun e2eLoginWithEmailPassword() {
        LoginFlow().chooseContinueWithWpCom(super.mComposeTestRule)
            .enterEmailAddress(BuildConfig.E2E_WP_COM_USER_EMAIL)
            .enterPassword(BuildConfig.E2E_WP_COM_USER_PASSWORD)
            .confirmLogin()

        ComposeEspressoLink().unregister()
    }

    @Test
    fun e2eLoginWithPasswordlessAccount() {
        LoginFlow().chooseContinueWithWpCom(super.mComposeTestRule)
            .enterEmailAddress(BuildConfig.E2E_WP_COM_PASSWORDLESS_USER_EMAIL)
            .openMagicLink()
            .confirmLogin()

        ComposeEspressoLink().unregister()
    }

    @Test
    fun e2eLoginWithSiteAddress() {
        LoginFlow().chooseEnterYourSiteAddress(super.mComposeTestRule)
            .enterSiteAddress(BuildConfig.E2E_WP_COM_USER_SITE_ADDRESS)
            .enterEmailAddress(BuildConfig.E2E_WP_COM_USER_EMAIL)
            .enterPassword(BuildConfig.E2E_WP_COM_USER_PASSWORD)
            .confirmLogin()

        ComposeEspressoLink().unregister()
    }

    @Test
    fun e2eLoginWithMagicLink() {
        try {
            LoginFlow().chooseContinueWithWpCom(super.mComposeTestRule)
                .enterEmailAddress(BuildConfig.E2E_WP_COM_USER_EMAIL)
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
            .enterSiteAddress(BuildConfig.E2E_SELF_HOSTED_USER_SITE_ADDRESS)
            .enterUsernameAndPassword(
                BuildConfig.E2E_SELF_HOSTED_USER_USERNAME,
                BuildConfig.E2E_SELF_HOSTED_USER_PASSWORD
            )
            .confirmLogin()

        ComposeEspressoLink().unregister()
    }
}
