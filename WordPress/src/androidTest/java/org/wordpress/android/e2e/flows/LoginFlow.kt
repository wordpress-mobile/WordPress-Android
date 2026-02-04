package org.wordpress.android.e2e.flows

import android.widget.EditText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.e2e.pages.HelpScreen
import org.wordpress.android.e2e.pages.LandingPage.tapContinueWithWpCom
import org.wordpress.android.e2e.pages.LandingPage.tapEnterYourSiteAddress
import org.wordpress.android.support.WPSupportUtils
import org.wordpress.android.login.R as LoginR

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
        WPSupportUtils.clickOn(LoginR.id.login_continue_button)
        return this
    }

    fun enterUsernameAndPassword(username: String, password: String): LoginFlow {
        val usernameElement = Espresso.onView(
            CoreMatchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(LoginR.id.login_username_row)),
                Matchers.instanceOf(EditText::class.java)
            )
        )
        val passwordElement = Espresso.onView(
            CoreMatchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(LoginR.id.login_password_row)),
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
        // See LoginSiteApplicationPasswordFragment
        WPSupportUtils.populateTextField(R.id.input, siteAddress)
        WPSupportUtils.clickOn(R.id.bottom_button)
        return this
    }

    fun tapHelp(): HelpScreen {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withId(R.id.help)))
        return HelpScreen()
    }
}
