package org.wordpress.android.ui.end2end.pages;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class MySitesPage {

    // Labels
    private static ViewInteraction siteTitleLabel = onView(withId(R.id.my_site_title_label));
    private static ViewInteraction siteAddressLabel = onView(withId(R.id.my_site_subtitle_label));

    // Buttons
    private static ViewInteraction newPostButton = onView(allOf(withId(R.id.fab_button), withContentDescription("Write")));

    public MySitesPage() {
        siteTitleLabel.check(matches(isDisplayed()));
    }

    public void startNewPost(String siteAddress) {
        siteAddressLabel.check(matches(withText(siteAddress)));
        newPostButton.perform(click());
    }
}
