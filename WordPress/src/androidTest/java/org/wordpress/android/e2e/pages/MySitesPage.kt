package org.wordpress.android.e2e.pages

import android.view.View
import android.widget.Checkable
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.PreferenceMatchers
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.BetterScrollToAction.Companion.scrollTo
import org.wordpress.android.support.ComposeEspressoLink
import org.wordpress.android.support.WPSupportUtils
import org.wordpress.android.ui.prefs.WPPreference
import android.R as AndroidR
import com.google.android.material.R as MaterialR

class MySitesPage {
    fun go(): MySitesPage {
        WPSupportUtils.clickOn(R.id.nav_sites)
        return this
    }

    fun switchSite() {
        WPSupportUtils.clickOn(R.id.switch_site)
        chooseSiteLabel.check(ViewAssertions.matches(ViewMatchers.withText("Choose site")))
    }

    private fun longClickSite(siteName: String) {
        val siteRow = Espresso.onView(ViewMatchers.withText(siteName))
        WPSupportUtils.longClickOn(siteRow)
    }

    fun removeSite(siteName: String) {
        switchSite()
        longClickSite(siteName)
        WPSupportUtils.clickOn(AndroidR.id.button1)
    }

    fun startNewPost() {
        ComposeEspressoLink().unregister()

        WPSupportUtils.clickOn(R.id.fab_button)
        if (WPSupportUtils.isElementDisplayed(MaterialR.id.design_bottom_sheet)) {
            // If Stories are enabled, FAB opens a bottom sheet with options - select the 'Blog post' option
            WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(R.string.my_site_bottom_sheet_add_post)))
        }
    }

    fun startNewSite() {
        switchSite()
        WPSupportUtils.clickOn(R.id.button_add_site)
    }

    fun goToSettings() {
        clickItemWithText(R.string.my_site_btn_site_settings)
    }

    fun goToPosts() {
        clickSiteMenuItem(R.string.my_site_btn_blog_posts)
    }

    fun goToActivityLog() {
        clickItemWithText(R.string.activity_log)
    }

    fun goToScan() {
        clickItemWithText(R.string.scan)
    }

    fun goToBloggingReminders() {
        goToSettings()
        WPSupportUtils.idleFor(4000)
        Espresso.onData(
            Matchers.allOf(
                Matchers.instanceOf<Any>(WPPreference::class.java),
                PreferenceMatchers.withKey(WPSupportUtils.getTranslatedString(R.string.pref_key_blogging_reminders)),
                PreferenceMatchers.withTitleText(WPSupportUtils.getTranslatedString(R.string.site_settings_blogging_reminders_title))
            )
        )
            .onChildView(ViewMatchers.withText(WPSupportUtils.getTranslatedString(R.string.site_settings_blogging_reminders_title)))
            .perform(ViewActions.click())
        WPSupportUtils.idleFor(4000)
        WPSupportUtils.clickOn(
            Espresso.onView(
                ViewMatchers.withText(
                    WPSupportUtils.getTranslatedString(
                        R.string.set_your_blogging_reminders_button
                    )
                )
            )
        )
        Espresso.onView(ViewMatchers.withId(R.id.day_one))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.day_three))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.day_five))
            .perform(ViewActions.click())
        WPSupportUtils.idleFor(3000)
    }

    fun addBloggingPrompts() {
        goToBloggingReminders()
        WPSupportUtils.idleFor(4000)
        if (WPSupportUtils.isElementDisplayed(R.id.content_recycler_view)) {
            Espresso.onView(ViewMatchers.withId(R.id.content_recycler_view))
                .perform(
                    RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                        ViewMatchers.hasDescendant(ViewMatchers.withId(R.id.include_prompt_switch)),
                        setChecked(true, R.id.include_prompt_switch)
                    )
                )
        }
        WPSupportUtils.idleFor(4000)
        Espresso.onView(ViewMatchers.withId(R.id.primary_button))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.primary_button))
            .perform(ViewActions.click())
    }

    fun goToBackup() {
        // Using RecyclerViewActions.click doesn't work for some reason when quick actions are displayed.
        clickItemWithText(R.string.backup)
    }

    fun goToStats(): StatsPage {
        WPSupportUtils.idleFor(4000)
        WPSupportUtils.dismissJetpackAdIfPresent()
        WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(R.id.tabLayout)

        // Wait for the stats to load
        WPSupportUtils.idleFor(4000)
        return StatsPage()
    }

    fun goToMedia() {
        clickSiteMenuItem(R.string.media)
    }

    fun createPost() {
        // Choose the "sites" tab in the nav
        WPSupportUtils.clickOn(R.id.fab_button)
        WPSupportUtils.idleFor(2000)
    }

    fun switchToSite(siteUrl: String?): MySitesPage {
        // Choose the "sites" tab in the nav
        WPSupportUtils.clickOn(R.id.nav_sites)

        // Choose "Switch Site"
        WPSupportUtils.clickOn(R.id.switch_site)
        SitePickerPage().chooseSiteWithURL(siteUrl)
        return this
    }

    private fun clickItemWithText(stringResId: Int) {
        clickItem(ViewMatchers.withText(stringResId))
    }

    private fun clickItem(itemViewMatcher: Matcher<View>) {
        if (WPSupportUtils.isElementDisplayed(R.id.recycler_view)) {
            // If My Site Improvements are enabled, we reach the item in a different way
            Espresso.onView(ViewMatchers.withId(R.id.recycler_view))
                .perform(
                    RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                        ViewMatchers.hasDescendant(
                            itemViewMatcher
                        ), ViewActions.click()
                    )
                )
        }
    }

    /**
     * Clicks on the Site menu item
     * @param siteMenuItemString String resource id of the site menu item.
     */
    private fun clickSiteMenuItem(
        @StringRes siteMenuItemString: Int
    ) {
        clickItemWithText(siteMenuItemString)
    }

    companion object {
        private val chooseSiteLabel = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isAssignableFrom(
                    TextView::class.java
                ), ViewMatchers.withParent(
                    ViewMatchers.isAssignableFrom(
                        Toolbar::class.java
                    )
                )
            )
        )

        fun setChecked(checked: Boolean, id: Int): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): BaseMatcher<View?> {
                    return object : BaseMatcher<View?>() {
                        override fun matches(item: Any): Boolean {
                            return Matchers.isA<Any>(Checkable::class.java).matches(item)
                        }

                        override fun describeMismatch(
                            item: Any,
                            mismatchDescription: Description
                        ) {
                        }

                        override fun describeTo(description: Description) {}
                    }
                }

                override fun getDescription(): String? {
                    return null
                }

                override fun perform(uiController: UiController, view: View) {
                    val checkableView = view.findViewById<View>(id) as SwitchCompat
                    checkableView.isChecked = checked
                }
            }
        }
    }

    private fun scrollToCard(elementID: Int): MySitesPage {
        Espresso.onView(ViewMatchers.withId(R.id.recycler_view))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>
                (ViewMatchers.withId(elementID)))

        Espresso.onView(ViewMatchers.withId(elementID))
            .perform(scrollTo())

        WPSupportUtils.idleFor(2000)

        return this
    }

    private fun tapCard(elementID: Int) {
        WPSupportUtils.clickOn(elementID)
    }

    // "Pages" Dashboard Card

    fun scrollToPagesCard(): MySitesPage {
        return scrollToCard(R.id.dashboard_card_pages)
    }

    fun tapPagesCard(): PagesScreen {
        tapCard(R.id.dashboard_card_pages)
        return PagesScreen()
    }

    fun assertPagesCard(): MySitesPage {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.dashboard_card_pages),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.dashboard_pages_card_title),
                        ViewMatchers.withId(R.id.my_site_card_toolbar_title),
                    )
                ),

                ViewMatchers.hasDescendant(ViewMatchers.withId(R.id.my_site_card_toolbar_more)),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.dashboard_pages_card_create_another_page_button),
                        ViewMatchers.withId(R.id.link_label),
                    )
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))

        return this
    }

    fun assertPagesCardHasPage(pageTitle: String): MySitesPage {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.dashboard_card_pages),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText(pageTitle),
                        ViewMatchers.withId(R.id.title),
                    )
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))

        return this
    }

    // "Activity Log" Dashboard Card

    fun scrollToActivityLogCard(): MySitesPage {
        return scrollToCard(R.id.dashboard_card_activity_log)
    }

    fun tapActivity(activityPartial: String): EventScreen {
        val activityRow = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withText(containsString(activityPartial)),
                ViewMatchers.withId(R.id.activity_card_item_label),
            )
        )

        WPSupportUtils.clickOn(activityRow)
        return EventScreen()
    }

    fun assertActivityLogCard(): MySitesPage {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.dashboard_card_activity_log),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.dashboard_activity_card_title),
                        ViewMatchers.withId(R.id.my_site_card_toolbar_title),
                    )
                ),

                ViewMatchers.hasDescendant(ViewMatchers.withId(R.id.my_site_card_toolbar_more)),
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))

        return this
    }

    fun assertActivityLogCardHasActivity(activityPartial: String): MySitesPage {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.dashboard_card_activity_log),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText(containsString(activityPartial)),
                        ViewMatchers.withId(R.id.activity_card_item_label),
                    )
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))

        return this
    }
}
