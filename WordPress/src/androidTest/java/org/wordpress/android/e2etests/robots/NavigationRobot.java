package org.wordpress.android.e2etests.robots;


import android.support.test.espresso.contrib.ViewPagerActions;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class NavigationRobot {
    public NavigationRobot selectMyWordpressSites() {
        onView(withId(R.id.viewpager_main))
                .perform(ViewPagerActions.scrollToFirst());
        return this;
    }

    public NavigationRobot selectMyPosts() {
        onView(withId(R.id.viewpager_main))
                .perform(ViewPagerActions.scrollToPage(1));
        return this;
    }

    public NavigationRobot selectProfile() {
        onView(withId(R.id.viewpager_main))
                .perform(ViewPagerActions.scrollToPage(2));
        return this;
    }

    public NavigationRobot selectNotifications() {
        onView(withId(R.id.viewpager_main))
                .perform(ViewPagerActions.scrollToLast());
        return this;
    }

    public static class ResultRobot {
        public void displaysPostsPageSucessfully() {
            onView(withId(R.id.title_empty))
                    .check(matches(isDisplayed()));
        }

        public void displaysSiteMenuPageSucessfully() {
            onView(withId(R.id.row_stats))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.my_site_plan_text_view))
                    .check(matches(isDisplayed()));
        }

        public void displaysProfilePageSucessfully() {
            onView(withId(R.id.row_my_profile))
                    .check(matches(isDisplayed()));
        }

        public void displaysNotificationsPageSucessfully() {
            onView(withId(R.id.notifications_filter))
                    .check(matches(isDisplayed()));
        }
    }
}

