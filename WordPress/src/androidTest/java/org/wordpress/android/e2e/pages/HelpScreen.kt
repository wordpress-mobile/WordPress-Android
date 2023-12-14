package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils
import android.R as AndroidR

class HelpScreen {
    fun assertHelpScreenLoaded(): HelpScreen {
        // WordPress and Jetpack apps display different items on the Support Screen
        if (BuildConfig.IS_JETPACK_APP) {
            contactUsButton.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
            ticketsButton.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
            emailAddressText.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        } else {
            communityForumsButton.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        }

        faqButton.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        logsButton.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        applicationVersionText.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        return this
    }

    fun openContactUs(): ContactSupportScreen {
        contactUsButton.perform(ViewActions.click())
        setEmailIfNeeded("WPcomTest@test.com", "TestUser")
        return ContactSupportScreen()
    }

    fun setEmailIfNeeded(emailAddress: String?, userName: String?) {
        val emailInput =
            Espresso.onView(ViewMatchers.withId(R.id.support_identity_input_dialog_email_edit_text))
        if (!WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(emailInput)) {
            return
        }
        WPSupportUtils.populateTextField(emailInput, emailAddress)
        val nameInput =
            Espresso.onView(ViewMatchers.withId(R.id.support_identity_input_dialog_name_edit_text))
        WPSupportUtils.populateTextField(nameInput, userName)
        Espresso.onView(
            Matchers.anyOf(
                ViewMatchers.withText(AndroidR.string.ok),
                ViewMatchers.withId(AndroidR.id.button1)
            )
        )
            .perform(ViewActions.click())
    }

    companion object {
        var contactUsButton = Espresso.onView(ViewMatchers.withId(R.id.contact_us_button))
        var faqButton = Espresso.onView(ViewMatchers.withId(R.id.faq_button))
        var ticketsButton = Espresso.onView(ViewMatchers.withId(R.id.tickets_button))
        var logsButton = Espresso.onView(ViewMatchers.withId(R.id.logs_button))
        var applicationVersionText = Espresso.onView(ViewMatchers.withId(R.id.applicationVersion))
        var emailAddressText = Espresso.onView(ViewMatchers.withId(R.id.contactEmailAddress))
        var communityForumsButton = Espresso.onView(ViewMatchers.withId(R.id.support_forums_button))
    }
}
