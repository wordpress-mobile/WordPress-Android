package org.wordpress.android.e2e.pages;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.test.espresso.ViewInteraction;

import org.hamcrest.Matcher;
import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
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
        if (isElementDisplayed(R.id.design_bottom_sheet)) {
            // If Stories are enabled, FAB opens a bottom sheet with options - select the 'Blog post' option
            clickOn(onView(withText(R.string.my_site_bottom_sheet_add_post)));
        }
    }

    public void clickSettingsItem() {
        clickItemWithText(R.string.my_site_btn_site_settings);
    }

    public void clickBlogPostsItem() {
        clickItemWithText(R.string.my_site_btn_blog_posts);
    }

    public void clickActivityLog() {
        clickItemWithText(R.string.activity_log);
    }

    public void clickScan() {
        clickItemWithText(R.string.scan);
    }

    public void clickBackup() {
        clickItemWithText(R.string.backup);
    }

    public void clickStats() {
        if (isElementDisplayed(R.id.recycler_view)) {
            // If My Site Improvements are enabled, we reach the item in a different way
            onView(withId(R.id.recycler_view))
                    .perform(actionOnItem(hasDescendant(withText(R.string.stats)), click()));
        }
    }

    private void clickItemWithText(int stringResId) {
        clickItem(withText(stringResId));
    }

    private void clickItem(final Matcher<View> itemViewMatcher) {
        if (isElementDisplayed(R.id.recycler_view)) {
            // If My Site Improvements are enabled, we reach the item in a different way
            onView(withId(R.id.recycler_view))
                    .perform(actionOnItem(hasDescendant(itemViewMatcher), click()));
        }
    }
}
