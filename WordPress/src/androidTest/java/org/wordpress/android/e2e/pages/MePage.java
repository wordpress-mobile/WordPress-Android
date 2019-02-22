package org.wordpress.android.e2e.pages;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.clickOn;

public class MePage {
    // Labels
    private static ViewInteraction displayName = onView(withId(R.id.me_display_name));
    private static ViewInteraction usernameLabel = onView(withId(R.id.me_username));

    // Buttons
    private static ViewInteraction appSettings = onView(withId(R.id.row_app_settings));
    private static ViewInteraction logoutButton = onView(withId(R.id.row_logout));
    private static ViewInteraction logoutConfirmationButton = onView(withId(android.R.id.button1));

    public MePage() {
    }

    public MePage go() {
        clickOn(R.id.nav_me);
        displayName.check(matches(isDisplayed()));

        return this;
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
        logoutButton.perform(scrollTo(), click());
        logoutConfirmationButton.perform(click());
    }
}
