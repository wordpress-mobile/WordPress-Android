package org.wordpress.android.e2e.pages;

import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.test.espresso.ViewInteraction;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.longClickOn;

public class MySitesPage {
    private static ViewInteraction chooseSiteLabel =
            onView(allOf(isAssignableFrom(TextView.class), withParent(isAssignableFrom(Toolbar.class))));

    public MySitesPage() {
    }

    public MySitesPage go() {
        clickOn(R.id.nav_sites);

        return this;
    }

    public void switchSite() {
        clickOn(R.id.switch_site);
        chooseSiteLabel.check(matches(withText("Choose site")));
    }

    private void longClickSite(String siteName) {
        ViewInteraction siteRow = onView(withText(siteName));
        longClickOn(siteRow);
    }

    public void removeSite(String siteName) {
        switchSite();
        longClickSite(siteName);
        clickOn(android.R.id.button1);
    }

    public void startNewPost() {
        clickOn(R.id.fab_button);
    }

    public void gotoSiteSettings() {
        onView(withId(R.id.row_settings))
                .perform(scrollTo());
        clickOn(R.id.row_settings);
    }
}
