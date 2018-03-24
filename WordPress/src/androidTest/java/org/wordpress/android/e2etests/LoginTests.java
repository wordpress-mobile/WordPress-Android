package org.wordpress.android.e2etests;


import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.R;
import org.wordpress.android.e2etests.utils.TestData;
import org.wordpress.android.ui.WPLaunchActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


@RunWith(AndroidJUnit4.class)
public class LoginTests {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    private TestData mData = new TestData();
    private String mUserEmail = mData.getEmail();
    private String mUserPassword = mData.getPassword();

    @Test
    public void testLoginSuccess() {
        onView(withText("Log In"))
                .perform(click());

        onView(withId(R.id.input))
                .perform(replaceText(mUserEmail), closeSoftKeyboard());

        onView(withText("Next"))
                .perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.login_enter_password))
                .perform(click());

        onView(withId(R.id.input))
                .perform(replaceText(mUserPassword), closeSoftKeyboard());

        onView(withText("Next"))
                .perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.logged_in_as_heading))
                .check(matches(isDisplayed()));
        }

    @After public void tearDown() {
        // do the logout steps
    }
}

