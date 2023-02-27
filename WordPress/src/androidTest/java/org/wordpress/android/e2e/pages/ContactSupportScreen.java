package org.wordpress.android.e2e.pages;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.sleep;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;

public class ContactSupportScreen {
    // "Contact Support" screen looks differently depending on
    // "gradle.properties" content (default or from Mobile Secrets).
    // But the elements tree always contains all elements, some are
    // just hidden. Locators below attempt to support both variants.
    static ViewInteraction textInput = onView(allOf(
            isCompletelyDisplayed(),
            anyOf(
                    withId(R.id.message_composer_input_text),
                    withId(R.id.request_message_field)
            )
    ));
    static ViewInteraction sendButton = onView(allOf(
            isCompletelyDisplayed(),
            anyOf(
                    withId(R.id.message_composer_send_btn),
                    withId(R.id.request_conversations_disabled_menu_ic_send)
            )
    ));

    // Actions:
    public ContactSupportScreen setMessageText(String text) {
        populateTextField(textInput, text);
        // This sleep serves only one purpose: allowing human to notice
        // that text was really entered. Especially matters when watching
        // low-fps test video recordings from CI.
        sleep();
        return this;
    }

    public HelpScreen goBackAndDeleteUnsentMessageIfNeeded() {
        Espresso.pressBack();

        ViewInteraction unsentMessageAlert = onView(
                withText("Going back will delete your message. "
                         + "Are you sure you want to delete it?"
                ));

        if (waitForElementToBeDisplayedWithoutFailure(unsentMessageAlert)) {
            onView(withText("Delete"))
                    .perform(ViewActions.click());
        }

        return new HelpScreen();
    }

    // Assertions:
    public ContactSupportScreen assertContactSupportScreenLoaded() {
        waitForElementToBeDisplayed(textInput);
        textInput.check(matches(isCompletelyDisplayed()));
        sendButton.check(matches(isCompletelyDisplayed()));
        return this;
    }

    public ContactSupportScreen assertSendButtonDisabled() {
        sendButton.check(matches(not(isEnabled())));
        return this;
    }

    public ContactSupportScreen assertSendButtonEnabled() {
        sendButton.check(matches(isEnabled()));
        return this;
    }
}
