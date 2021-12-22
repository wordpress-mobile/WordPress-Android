package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;

public class ContactSupportScreen {
    static ViewInteraction textInput = onView(withId(R.id.message_composer_input_text));
    static ViewInteraction sendButton = onView(withId(R.id.message_composer_send_btn));
    static ViewInteraction attachButton = onView(withId(R.id.attachments_indicator_icon));

    // Actions:
    public ContactSupportScreen tapSendButton() {
        sendButton.perform(ViewActions.click());
        return this;
    }

    public ContactSupportScreen setMessageText(String text) {
        populateTextField(textInput, text);
        return this;
    }

    public HelpAndSupportScreen deleteUnsentMessageIfNeeded() {
        ViewInteraction unsentMessageAlert = onView(
                withText("Going back will delete your message. "
                         + "Are you sure you want to delete it?"
                ));

        if (waitForElementToBeDisplayedWithoutFailure(unsentMessageAlert)) {
            onView(withText("Delete"))
                    .perform(ViewActions.click());
        }

        return new HelpAndSupportScreen();
    }

    // Assertions:
    public ContactSupportScreen assertContactSupportScreenLoaded() {
        textInput.check(matches(isCompletelyDisplayed()));
        sendButton.check(matches(isCompletelyDisplayed()));
        attachButton.check(matches(isCompletelyDisplayed()));
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

    public ContactSupportScreen assertUserMessageDelivered(String messageText) {
        ViewInteraction userMessageContainer = onView(allOf(
                withId(R.id.request_user_message_container),
                hasDescendant(allOf(
                        withId(R.id.request_user_message_text),
                        withText(messageText)
                )),
                hasDescendant(allOf(
                        withId(R.id.request_user_message_status),
                        withText("Delivered")
                ))
        ));

        waitForElementToBeDisplayed(userMessageContainer);
        userMessageContainer.check(matches(isCompletelyDisplayed()));
        return this;
    }

    public ContactSupportScreen assertSystemMessageReceived(String messageText) {
        ViewInteraction systemResponseBubble = onView(allOf(
                withId(R.id.request_system_message_text),
                withText(messageText)));

        waitForElementToBeDisplayed(systemResponseBubble);
        systemResponseBubble.check(matches(isCompletelyDisplayed()));
        return this;
    }
}
