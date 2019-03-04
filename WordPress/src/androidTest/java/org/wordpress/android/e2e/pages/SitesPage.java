package org.wordpress.android.e2e.pages;

import android.support.test.espresso.ViewInteraction;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.longClickOn;

public class SitesPage {
    private static ViewInteraction chooseSiteLabel =
            onView(allOf(isAssignableFrom(TextView.class), withParent(isAssignableFrom(Toolbar.class))));

    public SitesPage() {
    }

    public SitesPage go() {
        clickOn(R.id.switch_site);

        chooseSiteLabel.check(matches(withText("Choose site")));

        return this;
    }

    private void longClickSite(String siteName) {
        ViewInteraction siteRow = onView(withText(siteName));
        longClickOn(siteRow);
    }

    public void removeSite(String siteNmae) {
        longClickSite(siteNmae);
        clickOn(android.R.id.button1);
    }
}
