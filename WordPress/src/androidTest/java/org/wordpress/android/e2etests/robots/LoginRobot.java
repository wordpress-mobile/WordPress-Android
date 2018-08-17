package org.wordpress.android.e2etests.robots;


import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


public class LoginRobot {
    public LoginRobot selectLoginOption() {
        onView(withText("Log In"))
                .perform(click());
        return this;
    }

    public LoginRobot typeUsername(String username) {
        onView(withId(R.id.input))
                .perform(click())
                .perform(typeText(username), closeSoftKeyboard());
        return this;
    }

    public LoginRobot typePassword(String password) {
        onView(withId(R.id.input))
                .perform(click())
                .perform(typeText(password), closeSoftKeyboard());
        return this;
    }

    public LoginRobot tapNextButton() {
        onView(withText("Next"))
                .perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return this;
    }

    public LoginRobot tapToEnterPasswordInstead() {
        onView(withId(R.id.login_enter_password))
                .perform(click());
        return this;
    }

    public LoginRobot tapToContinueOnSiteSelection() {
        onView(withText("Continue"))
                .perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    public static class ResultRobot {
        public void isSucessfullyLoggedIn() {
            onView(withId(R.id.logged_in_as_heading))
                    .check(matches(isDisplayed()));
        }
    }
}
