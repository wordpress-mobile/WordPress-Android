package org.wordpress.android.ui.end2end.pages.login;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

public class LoginEmailPage {

    // Text fields
    private static ViewInteraction emailField = onView(allOf(withClassName(endsWith("EditText")), isDescendantOfA(withId(R.id.login_email_row))));

    // Buttons and Links
    private static ViewInteraction nextButton = onView(withId(R.id.primary_button));

    public LoginEmailPage() {
        emailField.check(matches(isDisplayed()));
    }

    public void wpcomEmailLogin(String email) {
        emailField.perform(typeTextIntoFocusedView(email));
        nextButton.perform(click());
    }
}
