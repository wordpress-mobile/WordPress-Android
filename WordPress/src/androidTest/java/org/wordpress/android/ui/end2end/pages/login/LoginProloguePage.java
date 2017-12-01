package org.wordpress.android.ui.end2end.pages.login;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class LoginProloguePage {

    // App promos
    private static ViewInteraction introsPager = onView(withId(R.id.intros_pager));

    // Buttons
    private static ViewInteraction loginButton = onView(withId(R.id.login_button));
    private static ViewInteraction createSiteButton = onView(withId(R.id.create_site_button));

    public LoginProloguePage() {
        introsPager.check(matches(isDisplayed()));
        loginButton.check(matches(isDisplayed()));
        createSiteButton.check(matches(isDisplayed()));
    }

    public void createSite() {
        createSiteButton.perform(click());
    }

    public void login() {
        loginButton.perform(click());
    }
}
