package org.wordpress.android.ui.end2end.components;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.startsWith;

public class MasterbarComponent {

    // Main Navigation Bar
    private static ViewInteraction mainNavBar = onView(withId(R.id.tab_layout));
    private static ViewInteraction mySitesButton = onView(allOf(withId(R.id.tab_icon), withContentDescription(startsWith("My Site"))));
    private static ViewInteraction meButton = onView(allOf(withId(R.id.tab_icon), withContentDescription(startsWith("Me"))));

    public MasterbarComponent() {
        mainNavBar.check(matches(isDisplayed()));
    }

    public void goToMySitesTab() {
        mySitesButton.perform(click());
    }

    public void goToMeTab() {
        meButton.perform(click());
    }
}
