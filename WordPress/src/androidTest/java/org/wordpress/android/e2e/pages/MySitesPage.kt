package org.wordpress.android.e2e.pages

import android.view.View
import android.widget.Checkable
import android.widget.TextView
import androidx.annotation.IdRes
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
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.BetterScrollToAction.Companion.scrollTo
import org.wordpress.android.support.WPSupportUtils
import org.wordpress.android.ui.prefs.WPPreference

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
        WPSupportUtils.clickOn(android.R.id.button1)
    }

    fun startNewPost() {
        WPSupportUtils.clickOn(R.id.fab_button)
        if (WPSupportUtils.isElementDisplayed(R.id.design_bottom_sheet)) {
            // If Stories are enabled, FAB opens a bottom sheet with options - select the 'Blog post' option
            WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(R.string.my_site_bottom_sheet_add_post)))
        }
    }

    fun startNewSite() {
        switchSite()
        // If the device has a narrower display, the menu_add is hidden in the overflow
        if (WPSupportUtils.isElementDisplayed(R.id.menu_add)) {
            WPSupportUtils.clickOn(R.id.menu_add)
        } else {
            // open the overflow and then click on the item with text
            Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
            Espresso.onView(ViewMatchers.withText(WPSupportUtils.getTranslatedString(R.string.site_picker_add_site)))
                .perform(ViewActions.click())
        }
    }

    fun goToSettings() {
        goToMenuTab()
        clickItemWithText(R.string.my_site_btn_site_settings)
    }

    fun goToPosts() {
        goToMenuTab()
        clickQuickActionOrSiteMenuItem(
            R.id.quick_action_posts_button,
            R.string.my_site_btn_blog_posts
        )
    }

    fun goToActivityLog() {
        goToMenuTab()
        clickItemWithText(R.string.activity_log)
    }

    fun goToScan() {
        goToMenuTab()
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
        goToMenuTab()

        // Using RecyclerViewActions.click doesn't work for some reason when quick actions are displayed.
        if (WPSupportUtils.isElementDisplayed(R.id.quick_actions_card)) {
            WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(R.string.backup)))
        } else {
            clickItemWithText(R.string.backup)
        }
    }

    fun goToStats(): StatsPage {
        goToMenuTab()
        val statsButton = Espresso.onView(Matchers.allOf(
            ViewMatchers.withText(R.string.stats),
            ViewMatchers.withId(R.id.my_site_item_primary_text)
        ))
        WPSupportUtils.clickOn(statsButton)
        WPSupportUtils.idleFor(4000)
        WPSupportUtils.dismissJetpackAdIfPresent()
        WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(R.id.tabLayout)

        // Wait for the stats to load
        WPSupportUtils.idleFor(8000)
        return StatsPage()
    }

    fun goToMedia() {
        goToMenuTab()
        clickQuickActionOrSiteMenuItem(R.id.quick_action_media_button, R.string.media)
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
     * Clicks on the "Quick Action" item or the Site menu item if the quick actions card is hidden.
     * Needed because locating site menu items by text fails if the quick actions are available.
     * @param quickActionItemId Id of the quick actions menu item.
     * @param siteMenuItemString String resource id of the site menu item.
     */
    private fun clickQuickActionOrSiteMenuItem(
        @IdRes quickActionItemId: Int,
        @StringRes siteMenuItemString: Int
    ) {
        if (WPSupportUtils.isElementDisplayed(quickActionItemId)) {
            WPSupportUtils.clickOn(quickActionItemId)
        } else {
            clickItemWithText(siteMenuItemString)
        }
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

        fun goToHomeTab() {
            WPSupportUtils.selectItemWithTitleInTabLayout(
                WPSupportUtils.getTranslatedString(R.string.my_site_dashboard_tab_title),
                R.id.tab_layout
            )
        }

        fun goToMenuTab() {
            WPSupportUtils.selectItemWithTitleInTabLayout(
                WPSupportUtils.getTranslatedString(R.string.my_site_menu_tab_title),
                R.id.tab_layout
            )
        }

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
        WPSupportUtils.waitForElementToBeDisplayed(elementID)
        Espresso.onView(ViewMatchers.withId(elementID))
            .perform(scrollTo())

        return this
    }

    fun scrollToDomainsCard(): MySitesPage {
        return scrollToCard(R.id.dashboard_card_domain_cta)
    }

    private fun tapCard(elementID: Int) {
        WPSupportUtils.clickOn(elementID)
    }

    fun tapDomainsCard(): DomainsScreen {
        tapCard(R.id.dashboard_card_domain_cta)
        return DomainsScreen()
    }

    fun assertDomainsCard(): MySitesPage {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.dashboard_card_domain_cta),
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.dashboard_cards)),
                ViewMatchers.hasDescendant(ViewMatchers.withId(R.id.dashboard_domain_card_more)),
                ViewMatchers.hasDescendant(ViewMatchers.withId(R.id.dashboard_card_domain_image)),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(

                        ViewMatchers.withText(R.string.dashboard_card_domain_title),
                        ViewMatchers.withId(R.id.dashboard_card_domain_title),
                    )
                ),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText(R.string.dashboard_card_domain_sub_title),
                        ViewMatchers.withId(R.id.dashboard_card_domain_sub_title),
                    )
                ),

            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))

        return this
    }
}
