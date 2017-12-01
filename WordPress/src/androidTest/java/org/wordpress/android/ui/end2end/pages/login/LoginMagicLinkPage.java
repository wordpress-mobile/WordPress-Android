package org.wordpress.android.ui.end2end.pages.login;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class LoginMagicLinkPage {

    private static ViewInteraction sendMagicLinkButton = onView(withId(R.id.login_request_magic_link));
    private static ViewInteraction enterPasswordButton = onView(withId(R.id.login_enter_password));

    public LoginMagicLinkPage() {
        sendMagicLinkButton.check(matches(isDisplayed()));
    }

    public void selectPasswordOption() {
        enterPasswordButton.perform(click());
    }

}
