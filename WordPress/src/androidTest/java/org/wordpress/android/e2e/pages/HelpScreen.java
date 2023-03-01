package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.hamcrest.Matchers.anyOf;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;


public class HelpScreen {
    static ViewInteraction contactUsButton = onView(withId(R.id.contact_us_button));
    static ViewInteraction faqButton = onView(withId(R.id.faq_button));
    static ViewInteraction ticketsButton = onView(withId(R.id.tickets_button));
    static ViewInteraction logsButton = onView(withId(R.id.logs_button));
    static ViewInteraction applicationVersionText = onView(withId(R.id.applicationVersion));
    static ViewInteraction emailAddressText = onView(withId(R.id.contactEmailAddress));


    public HelpScreen assertHelpScreenLoaded() {
        contactUsButton.check(matches(isCompletelyDisplayed()));
        faqButton.check(matches(isCompletelyDisplayed()));
        ticketsButton.check(matches(isCompletelyDisplayed()));
        logsButton.check(matches(isCompletelyDisplayed()));
        applicationVersionText.check(matches(isCompletelyDisplayed()));
        emailAddressText.check(matches(isCompletelyDisplayed()));
        return this;
    }

    public ContactSupportScreen openContactUs() {
        contactUsButton.perform(ViewActions.click());
        setEmailIfNeeded("WPcomTest@test.com", "TestUser");
        return new ContactSupportScreen();
    }

    public void setEmailIfNeeded(String emailAddress, String userName) {
        ViewInteraction emailInput = onView(withId(R.id.support_identity_input_dialog_email_edit_text));

        if (!waitForElementToBeDisplayedWithoutFailure(emailInput)) {
            return;
        }

        populateTextField(emailInput, emailAddress);
        ViewInteraction nameInput = onView(withId(R.id.support_identity_input_dialog_name_edit_text));
        populateTextField(nameInput, userName);

        onView(anyOf(
                withText(android.R.string.ok),
                withId(android.R.id.button1)
        ))
                .perform(ViewActions.click());
    }
}
