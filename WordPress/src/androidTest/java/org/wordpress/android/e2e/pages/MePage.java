package org.wordpress.android.e2e.pages;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.scrollToThenClickOn;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class MePage {
    // Labels
    private static ViewInteraction displayName = onView(withId(R.id.me_display_name));
    private static ViewInteraction usernameLabel = onView(withId(R.id.me_username));

    // Buttons
    private static ViewInteraction appSettings = onView(withId(R.id.row_app_settings));

    public MePage() {
    }

    public MePage go() {
        // Using the settings button as a marker for successfully navigating to the page
        while (!isElementDisplayed(appSettings)) {
            clickOn(R.id.nav_sites);
            clickOn(onView(allOf(withId(R.id.me_item), withEffectiveVisibility(Visibility.VISIBLE))));
        }

        if (!isSelfHosted()) {
            displayName.check(matches(isDisplayed()));
        }

        return this;
    }

    public void goBack() {
        Espresso.pressBack();
    }

    public MePage verifyUsername(String username) {
        String atUsername = "@" + username;
        usernameLabel.check(matches(withText(atUsername)));

        return this;
    }

    public boolean isSelfHosted() {
        waitForElementToBeDisplayed(R.id.row_logout);
        return isElementDisplayed(onView(withText(R.string.sign_in_wpcom)));
    }

    public void openAppSettings() {
        appSettings.perform(click());
    }

    public void logout() {
        ViewInteraction logOutButton = onView(allOf(
                withId(R.id.me_login_logout_text_view),
                withText(getCurrentActivity().getString(R.string.me_disconnect_from_wordpress_com))));
        waitForElementToBeDisplayed(logOutButton);
        while (!isElementDisplayed(android.R.id.button1)) {
            scrollToThenClickOn(logOutButton);
        }
        clickOn(android.R.id.button1);
    }
}
