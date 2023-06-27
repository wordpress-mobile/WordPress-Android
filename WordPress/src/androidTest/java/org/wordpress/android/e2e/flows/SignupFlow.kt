package org.wordpress.android.e2e.flows

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.e2e.pages.LandingPage.tapContinueWithWpCom
import org.wordpress.android.support.WPSupportUtils
import org.wordpress.android.login.R as LoginR

class SignupFlow {
    fun chooseContinueWithWpCom(composeTestRule: ComposeTestRule?): SignupFlow {
        // Login Prologue – We want to Continue with WordPress.com, not a site address
        tapContinueWithWpCom(composeTestRule!!)
        return this
    }

    fun enterEmail(email: String?): SignupFlow {
        // Email file = id/input
        WPSupportUtils.populateTextField(Espresso.onView(ViewMatchers.withId(R.id.input)), email)
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withId(LoginR.id.login_continue_button)))
        return this
    }

    fun openMagicLink(): SignupFlow {
        // Should see "Check email" button
        // See SignupMagicLinkFragment
        WPSupportUtils.waitForElementToBeDisplayed(LoginR.id.signup_magic_link_button)

        // Follow the magic link to continue login
        // Intent is invoked directly rather than through a browser as WireMock is unavailable once in the background
        val appVariant = BuildConfig.FLAVOR_app // Either "wordpress" or "jetpack"
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("$appVariant://magic-login?token=valid_token&new_user=1")
        ).setPackage(ApplicationProvider.getApplicationContext<Context>().packageName)
        ActivityScenario.launch<Activity>(intent)
        return this
    }

    fun checkEpilogue(displayName: String?, username: String?): SignupFlow {
        // Check Epilogue data
        val emailHeaderView =
            Espresso.onView(ViewMatchers.withId(R.id.login_epilogue_header_subtitle))
        WPSupportUtils.waitForElementToBeDisplayed(emailHeaderView)
        val displayNameField = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.input),
                ViewMatchers.withText(displayName)
            )
        )
        val usernameField = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.input),
                ViewMatchers.withText(username)
            )
        )
        WPSupportUtils.waitForElementToBeDisplayed(displayNameField)
        WPSupportUtils.waitForElementToBeDisplayed(usernameField)
        return this
    }

    fun enterPassword(password: String?): SignupFlow {
        // Enter Password
        val passwordField = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.input),
                ViewMatchers.withHint("Password (optional)")
            )
        )
        WPSupportUtils.waitForElementToBeDisplayed(passwordField)
        WPSupportUtils.populateTextField(passwordField, password)

        // Click continue
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withId(R.id.bottom_button)))
        return this
    }

    fun dismissInterstitial(): SignupFlow {
        // Dismiss post-signup interstitial
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withId(R.id.dismiss_button)))
        return this
    }

    fun dismissJetpackAd(): SignupFlow {
        WPSupportUtils.dismissJetpackAdIfPresent()
        return this
    }

    fun confirmSignup() {
        // Confirm signup
        WPSupportUtils.waitForElementToBeDisplayed(R.id.nav_sites)
    }
}
