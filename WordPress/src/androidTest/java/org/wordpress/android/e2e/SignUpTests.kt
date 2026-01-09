package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.wordpress.android.e2e.flows.SignupFlow
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.ComposeEspressoLink
import org.wordpress.android.support.E2ECredentials

@HiltAndroidTest
class SignUpTests : BaseTest() {
    @Before
    fun setUp() {
        ComposeEspressoLink().unregister()
        logoutIfNecessary()
    }

    @Test
    fun e2eSignUpWithMagicLink() {
        try {
            SignupFlow().chooseContinueWithWpCom(super.mComposeTestRule)
                .enterEmail(E2ECredentials.SIGNUP_EMAIL)
                .openMagicLink()
                .checkEpilogue(
                    E2ECredentials.SIGNUP_DISPLAY_NAME,
                    E2ECredentials.SIGNUP_USERNAME
                )
                .enterPassword(E2ECredentials.SIGNUP_PASSWORD)
                .dismissInterstitial()
                .dismissJetpackAd()
                .confirmSignup()
        } finally {
            logoutIfNecessary()
        }
    }
}
