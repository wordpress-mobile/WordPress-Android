package org.wordpress.android.e2etests;


import android.support.test.espresso.contrib.ViewPagerActions;
import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.e2etests.utils.ToastMatcher;
import org.wordpress.android.ui.WPLaunchActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class ComposeTests {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    String mUsername = BuildConfig.ESPRESSO_USERNAME;
    String mPassword = BuildConfig.ESPRESSO_PASSWORD;

    @Test
    public void testBlogPosting() {
        onView(withText("Log In"))
                .perform(click());

        onView(withId(R.id.input))
                .perform(replaceText(mUsername), closeSoftKeyboard());

        onView(withText("Next"))
                .perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.login_enter_password))
                .perform(click());

        onView(withId(R.id.input))
                .perform(replaceText(mPassword), closeSoftKeyboard());

        onView(withText("Next"))
                .perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withText("Continue"))
                .perform(click());

        onView(withId(R.id.viewpager_main))
                .perform(ViewPagerActions.scrollToFirst());

        onView(withId(R.id.fab_button))
                .perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.title))
                .perform(replaceText("Hello"), closeSoftKeyboard());

        onView(withId(R.id.aztec))
                .perform(replaceText("World"), closeSoftKeyboard());

        onView(withId(R.id.menu_save_post))
                .perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.promo_dialog_button_positive))
                .perform(click());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withText(R.string.uploading_post)).inRoot(new ToastMatcher())
                                                 .check(matches(isDisplayed()));
    }
}
