package org.wordpress.android.ui.end2end.pages;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class MePage {

    // Labels
    private static ViewInteraction displayName = onView(withId(R.id.me_display_name));
    private static ViewInteraction usernameLabel = onView(withId(R.id.me_username));

    // Buttons
    private static ViewInteraction appSettings = onView(withId(R.id.row_app_settings));
    private static ViewInteraction logoutButton = onView(withId(R.id.row_logout));
    private static ViewInteraction logoutConfirmationButton = onView(withId(android.R.id.button1));

    public MePage() {
        displayName.check(matches(isDisplayed()));
    }

    public MePage verifyUsername(String username) {
        String atUsername = "@" + username;
        usernameLabel.check(matches(withText(atUsername)));

        return this;
    }

    public void openAppSettings() {
        appSettings.perform(click());
    }

    public void logout() {
        logoutButton.perform(click());
        logoutConfirmationButton.perform(click());
    }
}
