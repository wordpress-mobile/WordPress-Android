package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

class ContactSupportScreen {
    // Actions:
    fun setMessageText(text: String?): ContactSupportScreen {
        WPSupportUtils.populateTextField(textInput, text)
        // This sleep serves only one purpose: allowing human to notice
        // that text was really entered. Especially matters when watching
        // low-fps test video recordings from CI.
        WPSupportUtils.sleep()
        return this
    }

    // Assertions:
    fun assertContactSupportScreenLoaded(): ContactSupportScreen {
        WPSupportUtils.waitForElementToBeDisplayed(textInput)
        textInput.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        sendButton.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        return this
    }

    fun assertSendButtonDisabled(): ContactSupportScreen {
        sendButton.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
        return this
    }

    fun assertSendButtonEnabled(): ContactSupportScreen {
        sendButton.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
        return this
    }

    companion object {
        // "Contact Support" screen looks differently depending on
        // "gradle.properties" content (default or from Mobile Secrets).
        // But the elements tree always contains all elements, some are
        // just hidden. Locators below attempt to support both variants.
        var textInput = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isCompletelyDisplayed(),
                Matchers.anyOf(
                    ViewMatchers.withId(R.id.message_composer_input_text),
                    ViewMatchers.withId(R.id.request_message_field)
                )
            )
        )
        var sendButton = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isCompletelyDisplayed(),
                Matchers.anyOf(
                    ViewMatchers.withId(R.id.message_composer_send_btn),
                    ViewMatchers.withId(R.id.request_conversations_disabled_menu_ic_send)
                )
            )
        )
    }
}
