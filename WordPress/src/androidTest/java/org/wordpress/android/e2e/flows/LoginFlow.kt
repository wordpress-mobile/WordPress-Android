package org.wordpress.android.e2e.flows

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.e2e.pages.HelpScreen
import org.wordpress.android.e2e.pages.LandingPage.tapContinueWithWpCom
import org.wordpress.android.e2e.pages.LandingPage.tapEnterYourSiteAddress
import org.wordpress.android.support.WPSupportUtils

class LoginFlow {
    fun chooseContinueWithWpCom(composeTestRule: ComposeTestRule?): LoginFlow {
        // Login Prologue – We want to Continue with WordPress.com, not a site address
        tapContinueWithWpCom(composeTestRule!!)
        return this
    }

    fun enterEmailAddress(emailAddress: String?): LoginFlow {
        // Email Address Screen – Fill it in and click "Continue"
        // See LoginEmailFragment
        WPSupportUtils.populateTextField(R.id.input, emailAddress)
        WPSupportUtils.clickOn(R.id.login_continue_button)
        return this
    }

    fun enterPassword(password: String?): LoginFlow {
        // Password Screen – Fill it in and click "Continue"
        // See LoginEmailPasswordFragment
        WPSupportUtils.populateTextField(R.id.input, password)
        WPSupportUtils.clickOn(R.id.bottom_button)
        return this
    }

    fun confirmLogin(isSelfHosted: Boolean) {
        // If we get bumped to the "enter your username and password" screen, fill it in
        if (WPSupportUtils.atLeastOneElementWithIdIsDisplayed(R.id.login_password_row)) {
            enterUsernameAndPassword(
                BuildConfig.E2E_WP_COM_USER_USERNAME,
                BuildConfig.E2E_WP_COM_USER_PASSWORD
            )
        }

        // New Epilogue Screen - Choose the first site from the list of site.
        // See LoginEpilogueFragment
        val sitesList = Espresso.onView(ViewMatchers.withId(R.id.recycler_view))
        WPSupportUtils.waitForElementToBeDisplayed(sitesList)
        sitesList.perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                ViewActions.click()
            )
        )
        if (!isSelfHosted) {
            // Quick Start Prompt Dialog - Click the "No thanks" negative button to continue.
            // See QuickStartPromptDialogFragment
            WPSupportUtils.clickOn(R.id.quick_start_prompt_dialog_button_negative)
        }
        if (BuildConfig.IS_JETPACK_APP) {
            dismissNewFeaturesDialogIfDisplayed()
        }
        WPSupportUtils.waitForElementToBeDisplayed(R.id.nav_sites)
    }

    fun chooseMagicLink(): LoginFlow {
        // Password Screen – Choose "Get a login link by email"
        // See LoginEmailPasswordFragment
        WPSupportUtils.clickOn(R.id.login_get_email_link)
        return this
    }

    fun openMagicLink(): LoginFlow {
        // Magic Link Sent Screen – Should see "Check email" button
        // See LoginMagicLinkSentFragment
        WPSupportUtils.waitForElementToBeDisplayed(R.id.login_open_email_client)

        // Follow the magic link to continue login
        // Intent is invoked directly rather than through a browser as WireMock is unavailable once in the background
        val appVariant = BuildConfig.FLAVOR_app
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("$appVariant://magic-login?token=valid_token"))
                .setPackage(ApplicationProvider.getApplicationContext<Context>().packageName)
        ActivityScenario.launch<Activity>(intent)
        return this
    }

    fun enterUsernameAndPassword(username: String, password: String): LoginFlow {
        val usernameElement = Espresso.onView(
            CoreMatchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.login_username_row)),
                Matchers.instanceOf(EditText::class.java)
            )
        )
        val passwordElement = Espresso.onView(
            CoreMatchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.login_password_row)),
                Matchers.instanceOf(EditText::class.java)
            )
        )
        WPSupportUtils.populateTextField(
            usernameElement, """
     $username

     """.trimIndent()
        )
        WPSupportUtils.populateTextField(
            passwordElement, """
     $password

     """.trimIndent()
        )
        WPSupportUtils.clickOn(R.id.bottom_button)
        return this
    }

    fun chooseEnterYourSiteAddress(composeTestRule: ComposeTestRule?): LoginFlow {
        // Login Prologue – We want to continue with a site address not a WordPress.com account
        tapEnterYourSiteAddress(composeTestRule!!)
        return this
    }

    fun enterSiteAddress(siteAddress: String?): LoginFlow {
        // Site Address Screen – Fill it in and click "Continue"
        // See LoginSiteAddressFragment
        WPSupportUtils.populateTextField(R.id.input, siteAddress)
        WPSupportUtils.clickOn(R.id.bottom_button)
        return this
    }

    fun tapHelp(): HelpScreen {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withId(R.id.help)))
        return HelpScreen()
    }

    companion object {
        fun dismissNewFeaturesDialogIfDisplayed() {
            if (WPSupportUtils.isElementDisplayed(R.id.blogging_prompts_onboarding_button_container)) {
                WPSupportUtils.clickOn(R.id.close_button)
            }
        }
    }
}
