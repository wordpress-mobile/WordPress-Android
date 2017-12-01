package org.wordpress.android.ui.end2end.pages.login;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

public class LoginPasswordPage {

    // Text Fields
    private static ViewInteraction passwordField = onView(allOf(withClassName(endsWith("EditText")), isDescendantOfA(withId(R.id.login_password_row))));

    // Buttons
    private static ViewInteraction nextButton = onView(withId(R.id.primary_button));

    public LoginPasswordPage() {
        passwordField.check(matches(isDisplayed()));
    }

    public void enterPassword(String password) {
        passwordField.perform(replaceText(password));
        nextButton.perform(click());
    }
}
