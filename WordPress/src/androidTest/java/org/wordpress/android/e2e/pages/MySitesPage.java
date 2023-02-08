package org.wordpress.android.e2e.pages;

import android.view.View;
import android.widget.Checkable;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.WPPreference;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static androidx.test.espresso.matcher.PreferenceMatchers.withKey;
import static androidx.test.espresso.matcher.PreferenceMatchers.withTitleText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isA;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.dismissJetpackAdIfPresent;
import static org.wordpress.android.support.WPSupportUtils.getTranslatedString;
import static org.wordpress.android.support.WPSupportUtils.idleFor;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.longClickOn;
import static org.wordpress.android.support.WPSupportUtils.selectItemWithTitleInTabLayout;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayedWithoutFailure;

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

    public void startNewSite() {
        switchSite();
        // If the device has a narrower display, the menu_add is hidden in the overflow
        if (isElementDisplayed(R.id.menu_add)) {
            clickOn(R.id.menu_add);
        } else {
            // open the overflow and then click on the item with text
            openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
            onView(withText(getTranslatedString(R.string.site_picker_add_site))).perform(click());
        }
    }

    public void goToSettings() {
        goToMenuTab();
        clickItemWithText(R.string.my_site_btn_site_settings);
    }

    public void goToPosts() {
        goToMenuTab();
        clickQuickActionOrSiteMenuItem(R.id.quick_action_posts_button, R.string.my_site_btn_blog_posts);
    }

    public void goToActivityLog() {
        goToMenuTab();
        clickItemWithText(R.string.activity_log);
    }

    public void goToScan() {
        goToMenuTab();
        clickItemWithText(R.string.scan);
    }

    public void goToBloggingReminders() {
        goToSettings();

        idleFor(4000);

        onData(allOf(
                instanceOf(WPPreference.class),
                withKey(getTranslatedString(R.string.pref_key_blogging_reminders)),
                withTitleText(getTranslatedString(R.string.site_settings_blogging_reminders_title))))
                .onChildView(withText(getTranslatedString(R.string.site_settings_blogging_reminders_title)))
                .perform(click());

        idleFor(4000);

        clickOn(onView(withText(getTranslatedString(R.string.set_your_blogging_reminders_button))));

        onView(withId(R.id.day_one))
                .perform(click());

        onView(withId(R.id.day_three))
                .perform(click());

        onView(withId(R.id.day_five))
                .perform(click());

        idleFor(3000);
    }

    public void addBloggingPrompts() {
        goToBloggingReminders();

        idleFor(4000);

        if (isElementDisplayed(R.id.content_recycler_view)) {
            onView(withId(R.id.content_recycler_view))
                    .perform(actionOnItem(hasDescendant(withId(R.id.include_prompt_switch)),
                            setChecked(true, R.id.include_prompt_switch)));
        }

        idleFor(4000);

        onView(withId(R.id.primary_button))
                .perform(click());

        onView(withId(R.id.primary_button))
                .perform(click());
    }

    public void goToBackup() {
        goToMenuTab();

        // Using RecyclerViewActions.click doesn't work for some reason when quick actions are displayed.
        if (isElementDisplayed(R.id.quick_actions_card)) {
            clickOn(onView(withText(R.string.backup)));
        } else {
            clickItemWithText(R.string.backup);
        }
    }

    public StatsPage goToStats() {
        goToMenuTab();
        clickQuickActionOrSiteMenuItem(R.id.quick_action_stats_button, R.string.stats);
        idleFor(4000);
        dismissJetpackAdIfPresent();
        waitForElementToBeDisplayedWithoutFailure(R.id.tabLayout);

        // Wait for the stats to load
        idleFor(8000);

        return new StatsPage();
    }

    public void goToMedia() {
        goToMenuTab();
        clickQuickActionOrSiteMenuItem(R.id.quick_action_media_button, R.string.media);
    }

    public void createPost() {
        // Choose the "sites" tab in the nav
        clickOn(R.id.fab_button);

        idleFor(2000);
    }

    public MySitesPage switchToSite(String siteUrl) {
        // Choose the "sites" tab in the nav
        clickOn(R.id.nav_sites);

        // Choose "Switch Site"
        clickOn(R.id.switch_site);

        (new SitePickerPage()).chooseSiteWithURL(siteUrl);

        return this;
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

    @SuppressWarnings("unused")
    public static void goToHomeTab() {
        selectItemWithTitleInTabLayout(getTranslatedString(R.string.my_site_dashboard_tab_title), R.id.tab_layout);
    }

    public static void goToMenuTab() {
        selectItemWithTitleInTabLayout(getTranslatedString(R.string.my_site_menu_tab_title), R.id.tab_layout);
    }

    /**
     * Clicks on the "Quick Action" item or the Site menu item if the quick actions card is hidden.
     * Needed because locating site menu items by text fails if the quick actions are available.
     * @param quickActionItemId Id of the quick actions menu item.
     * @param siteMenuItemString String resource id of the site menu item.
     */
    private void clickQuickActionOrSiteMenuItem(@IdRes int quickActionItemId, @StringRes int siteMenuItemString) {
        if (isElementDisplayed(quickActionItemId)) {
            clickOn(quickActionItemId);
        } else {
            clickItemWithText(siteMenuItemString);
        }
    }

    public static ViewAction setChecked(final boolean checked, final int id) {
        return new ViewAction() {
            @Override
            public BaseMatcher<View> getConstraints() {
                return new BaseMatcher<View>() {
                    @Override
                    public boolean matches(Object item) {
                        return isA(Checkable.class).matches(item);
                    }

                    @Override
                    public void describeMismatch(Object item, Description mismatchDescription) {
                    }

                    @Override
                    public void describeTo(Description description) {
                    }
                };
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                SwitchCompat checkableView = (SwitchCompat) view.findViewById(id);
                checkableView.setChecked(checked);
            }
        };
    }
}
