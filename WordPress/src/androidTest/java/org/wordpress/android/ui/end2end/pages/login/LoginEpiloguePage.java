package org.wordpress.android.ui.end2end.pages.login;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class LoginEpiloguePage {

    // Labels
    private static ViewInteraction loggedInAsLabel = onView(withId(R.id.logged_in_as_heading));
    private static ViewInteraction usernameLabel = onView(withId(R.id.username));

    // Buttons
    private static ViewInteraction continueButton = onView(withId(R.id.primary_button));

    public LoginEpiloguePage() {
        loggedInAsLabel.check(matches(isDisplayed()));
    }

    public LoginEpiloguePage verifyUsername(String username) {
        String atUsername = "@" + username;
        usernameLabel.check(matches(withText(atUsername)));

        return this;
    }

    public void closeEpilogue() {
        continueButton.perform(click());
    }
}
