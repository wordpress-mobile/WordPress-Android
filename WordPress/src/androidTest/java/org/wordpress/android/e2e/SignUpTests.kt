package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BuildConfig
import org.wordpress.android.e2e.flows.SignupFlow
import org.wordpress.android.support.BaseTest

@HiltAndroidTest
class SignUpTests : BaseTest() {
    @Before
    fun setUp() {
        logoutIfNecessary()
    }

    @Test
    fun e2eSignUpWithMagicLink() {
        SignupFlow().chooseContinueWithWpCom(super.mComposeTestRule)
            .enterEmail(BuildConfig.E2E_SIGNUP_EMAIL)
            .openMagicLink()
            .checkEpilogue(
                BuildConfig.E2E_SIGNUP_DISPLAY_NAME,
                BuildConfig.E2E_SIGNUP_USERNAME
            )
            .enterPassword(BuildConfig.E2E_SIGNUP_PASSWORD)
            .dismissInterstitial()
            .dismissJetpackAd()
            .confirmSignup()
    }
}
