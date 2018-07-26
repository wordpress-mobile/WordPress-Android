package org.wordpress.android.ui.screenshots;


import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import org.junit.ClassRule;
import org.wordpress.android.R;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.ui.WPLaunchActivity;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;


import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.wordpress.android.BuildConfig.SCREENSHOT_LOGINPASSWORD;
import static org.wordpress.android.BuildConfig.SCREENSHOT_LOGINUSERNAME;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WPScreenshotTest {
    private static final int ATTEMPTS = 20;
    private static final int WAITING_TIME = 300;

    @ClassRule
    public static final LocaleTestRule LOCALE_TEST_RULE = new LocaleTestRule();


    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class,
            false, false);

    @Test
    public void wPScreenshotTest() {
        mActivityTestRule.launchActivity(null);
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        wPLogin();
        navigateReader();
        editBlogPost();
        navigateNotifications();
        navigateStats();
        wPLogout();
    }

    private void wPLogin() {
        // Login
        ViewInteraction loginButton = onView(
                allOf(withId(R.id.login_button),
                        childAtPosition(allOf(withId(R.id.bottom_buttons),
                                childAtPosition(withClassName(is("android.widget.RelativeLayout")), 3)),
                        0)));
        waitForElementUntilDisplayed(loginButton).perform(click());

        // User name
        ViewInteraction userNameEditText = onView(
                allOf(withId(R.id.input),
                        childAtPosition(childAtPosition(withId(R.id.input_layout), 0), 0)));
        waitForElementUntilDisplayed(userNameEditText).perform(replaceText(SCREENSHOT_LOGINUSERNAME),
                closeSoftKeyboard());

        // Next Button
        ViewInteraction nextButton = onView(
                allOf(withId(R.id.primary_button),
                        childAtPosition(allOf(withId(R.id.bottom_buttons), childAtPosition(
                                withClassName(is("android.widget.RelativeLayout")), 2)), 1)));
        waitForElementUntilDisplayed(nextButton).perform(click());

        // Enter password button
        ViewInteraction enterPasswordButton = onView(
                allOf(withId(R.id.login_enter_password),
                        childAtPosition(childAtPosition(
                                withClassName(is("android.widget.ScrollView")), 0), 3)));
        waitForElementUntilDisplayed(enterPasswordButton).perform(scrollTo(), click());

        // Password
        ViewInteraction passwordEditText = onView(
                allOf(withId(R.id.input), childAtPosition(
                        childAtPosition(withId(R.id.input_layout), 0), 0)));
        waitForElementUntilDisplayed(passwordEditText).perform(replaceText(SCREENSHOT_LOGINPASSWORD),
                closeSoftKeyboard());

        // Next Button
        nextButton = onView(
                allOf(withId(R.id.primary_button), withText("Next"),
                        childAtPosition(allOf(withId(R.id.bottom_buttons),
                                childAtPosition(withClassName(is("android.widget.RelativeLayout")), 2)), 1)));
        waitForElementUntilDisplayed(nextButton).perform(click());


        // Continue with this log button
        ViewInteraction continueButton = onView(
                allOf(withId(R.id.primary_button), withText("Continue"),
                        childAtPosition(allOf(withId(R.id.bottom_buttons),
                                childAtPosition(withClassName(is("android.widget.RelativeLayout")), 3)), 1)));
        waitForElementUntilDisplayed(continueButton).perform(click());
    }

    private void wPLogout() {
        // Me button
        ViewInteraction meButton = onView(
                allOf(withId(R.id.nav_me),
                        childAtPosition(childAtPosition(withId(R.id.bottom_navigation), 0), 3)));
        waitForElementUntilDisplayed(meButton).perform(click());

        // Log out button
        ViewInteraction logoutButton = onView(
                allOf(withId(R.id.row_logout),
                        childAtPosition(childAtPosition(withId(R.id.scroll_view), 0), 11)));
        waitForElementUntilDisplayed(logoutButton).perform(scrollTo(), click());

        // Log out confirm button
        ViewInteraction logoutConfirmButton = onView(
                allOf(withId(android.R.id.button1), childAtPosition(
                        childAtPosition(withClassName(is("android.widget.ScrollView")), 0), 3)));
        waitForElementUntilDisplayed(logoutConfirmButton).perform(scrollTo(), click());
    }

    private void navigateReader() {
        ViewInteraction navReaderButton = onView(
                allOf(withId(R.id.nav_reader), childAtPosition(
                        childAtPosition(withId(R.id.bottom_navigation), 0), 1)));
        waitForElementUntilDisplayed(navReaderButton).perform(click());

        // Select Discover
        ViewInteraction spinner = onView(
                allOf(withId(R.id.filter_spinner), childAtPosition(
                        withId(R.id.toolbar_with_spinner), 0)));
        waitForElementUntilDisplayed(spinner).perform(click());

        ViewInteraction spinnerOption = onView(allOf(withId(R.id.text), withText("Discover")));
        waitForElementUntilDisplayed(spinnerOption).perform(click());

        // Waiting for the blog articles to load
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Screengrab.screenshot("screenshot_2");
    }

    private void editBlogPost() {
        // Blog button on Nav Bar
        ViewInteraction blogNavBar = onView(
                allOf(withId(R.id.nav_sites), childAtPosition(
                        childAtPosition(withId(R.id.bottom_navigation), 0), 0)));
        waitForElementUntilDisplayed(blogNavBar).perform(click());
        Screengrab.screenshot("screenshot_3");

        // Blog posts button
        ViewInteraction blogPostsButton = onView(
                allOf(withId(R.id.row_blog_posts), childAtPosition(
                        childAtPosition(withClassName(is("android.widget.LinearLayout")), 6), 0)));
        waitForElementUntilDisplayed(blogPostsButton).perform(scrollTo(), click());

        // Waiting for the blog articles to load
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Edit the first post
        ViewInteraction postCard = onView(
                allOf(withId(R.id.card_view), childAtPosition(
                        withId(R.id.recycler_view), 0)));
        waitForElementUntilDisplayed(postCard).perform(click());

        ViewInteraction aztecText = onView(
                allOf(withId(R.id.aztec), childAtPosition(
                        childAtPosition(withClassName(is("android.widget.LinearLayout")), 2), 0)));
        waitForElementUntilDisplayed(aztecText);

        ViewInteraction postTitle = onView(
                allOf(withId(R.id.title), childAtPosition(
                        withClassName(is("android.widget.RelativeLayout")), 0)));
        postTitle.perform(scrollTo(), click());

        // Wait a bit
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Screengrab.screenshot("screenshot_1");

        // Exit
        ViewInteraction navigateUpButton = onView(allOf(childAtPosition(
                allOf(withId(R.id.action_bar), childAtPosition(withId(R.id.action_bar_container), 0)), 1)));
        waitForElementUntilDisplayed(navigateUpButton).perform(click());

        navigateUpButton = onView(allOf(childAtPosition(
                allOf(withId(R.id.toolbar),
                        childAtPosition(withClassName(is("android.widget.LinearLayout")), 0)), 2)));
        waitForElementUntilDisplayed(navigateUpButton).perform(click());
    }

    private void navigateNotifications() {
        // Notification button
        ViewInteraction notificationButton = onView(
                allOf(withId(R.id.nav_notifications), childAtPosition(
                        childAtPosition(withId(R.id.bottom_navigation), 0), 4)));
        waitForElementUntilDisplayed(notificationButton).perform(click());
        Screengrab.screenshot("screenshot_5");
    }

    private void navigateStats() {
        // Stats button
        ViewInteraction statsButton = onView(
                allOf(withId(R.id.nav_sites), childAtPosition(
                        childAtPosition(withId(R.id.bottom_navigation), 0), 0)));
        waitForElementUntilDisplayed(statsButton).perform(click());

        ViewInteraction linearLayout2 = onView(allOf(withId(R.id.row_stats), childAtPosition(
                childAtPosition(withId(R.id.scroll_view), 0), 1)));
        waitForElementUntilDisplayed(linearLayout2).perform(scrollTo(), click());

        // Close the dialog
        ViewInteraction dialogButton = onView(allOf(withId(R.id.promo_dialog_button_positive),
                        childAtPosition(childAtPosition(
                                withClassName(is("android.widget.RelativeLayout")), 3), 1)));
        try {
            // It may open or not, so catch the error if it's not up
            waitForElementUntilDisplayed(dialogButton).perform(click());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Select Days view
        ViewInteraction spinner = onView(
                allOf(withId(R.id.filter_spinner), childAtPosition(
                        withId(R.id.toolbar_filter), 0)));
        waitForElementUntilDisplayed(spinner).perform(click());

        Context targetContext = InstrumentationRegistry.getTargetContext();
        String daysOptionsString = targetContext.getResources().getString(R.string.stats_timeframe_days);
        ViewInteraction spinnerOption = onView(allOf(withId(R.id.text), withText(daysOptionsString)));
        waitForElementUntilDisplayed(spinnerOption).perform(click());

        // Wait a bit
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Screengrab.screenshot("screenshot_4");

        // Navigate up
        ViewInteraction navUpButton = onView(allOf(childAtPosition(allOf(withId(R.id.toolbar),
                childAtPosition(withClassName(is("android.widget.LinearLayout")), 0)), 2)));
        waitForElementUntilDisplayed(navUpButton).perform(click());
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    private static ViewInteraction waitForElementUntilDisplayed(ViewInteraction element) {
        int i = 0;
        while (i++ < ATTEMPTS) {
            try {
                element.check(matches(isDisplayed()));
                return element;
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Thread.sleep(WAITING_TIME);
                } catch (Exception e1) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
