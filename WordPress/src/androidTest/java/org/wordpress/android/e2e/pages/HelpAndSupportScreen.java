package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;


public class HelpAndSupportScreen {
    static ViewInteraction contactUsButton = onView(withId(R.id.contact_us_button));
    static ViewInteraction faqButton = onView(withId(R.id.faq_button));
    static ViewInteraction myTicketsButton = onView(withId(R.id.my_tickets_button));
    static ViewInteraction applicationLogButton = onView(withId(R.id.application_log_button));
    static ViewInteraction applicationVersionText = onView(withId(R.id.applicationVersion));
    static ViewInteraction emailAddressText = onView(withId(R.id.contactEmailAddress));


    public HelpAndSupportScreen assertHelpAndSupportScreenLoaded() {
        contactUsButton.check(matches(isCompletelyDisplayed()));
        faqButton.check(matches(isCompletelyDisplayed()));
        myTicketsButton.check(matches(isCompletelyDisplayed()));
        applicationLogButton.check(matches(isCompletelyDisplayed()));
        applicationVersionText.check(matches(isCompletelyDisplayed()));
        emailAddressText.check(matches(isCompletelyDisplayed()));
        return this;
    }

    public HelpAndSupportScreen setEmailIfNeeded(String emailAddress) {
        ViewInteraction emailNotSet = onView(allOf(withId(R.id.contactEmailAddress), withText("Not set")));

        if (!isElementDisplayed(emailNotSet)) return this;

        emailNotSet.perform(ViewActions.click());
        ViewInteraction emailInput = onView(withId(R.id.support_identity_input_dialog_email_edit_text));

        if (!waitForElementToBeDisplayedWithoutFailure(emailInput)) return this;

        populateTextField(emailInput, emailAddress);
        ViewInteraction okButton = onView(withText("OK"));
        waitForElementToBeDisplayedWithoutFailure(okButton);
        okButton.perform(ViewActions.click());

        return this;
    }

    public ContactSupportScreen openContactUs() {
        contactUsButton.perform(ViewActions.click());
        return new ContactSupportScreen();
    }
}
