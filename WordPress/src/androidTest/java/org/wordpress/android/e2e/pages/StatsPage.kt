package org.wordpress.android.e2e.pages

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.util.StatsKeyValueData
import org.wordpress.android.util.StatsVisitsData

class StatsPage {
    /**
     * Matcher to check that the right tabs exist.
     */
    fun hasNewStatTabs(): StatsPage {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.tabLayout)),
                ViewMatchers.withText("Traffic")
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.tabLayout)),
                ViewMatchers.withText("Insights")
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        return this
    }
    fun openDayStats(): StatsPage {
        val daysStatsTab = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.tabLayout)),
                ViewMatchers.withText("Days")
            )
        )
        val postsAndPagesCard = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(visibleCoordinatorLayout),
                ViewMatchers.withText("Posts and Pages")
            )
        )
        WPSupportUtils.waitForElementToBeDisplayed(daysStatsTab)
        daysStatsTab.perform(ViewActions.click())
        WPSupportUtils.waitForElementToBeDisplayed(postsAndPagesCard)
        return this
    }

    fun scrollToPosts(): StatsPage {
        scrollToCard(1, StatsListViewModel.StatsSection.DAYS)
        return this
    }

    fun scrollToReferrers(): StatsPage {
        scrollToCard(2, StatsListViewModel.StatsSection.DAYS)
        return this
    }

    fun scrollToClicks(): StatsPage {
        scrollToCard(3, StatsListViewModel.StatsSection.DAYS)
        return this
    }

    fun scrollToAuthors(): StatsPage {
        scrollToCard(4, StatsListViewModel.StatsSection.DAYS)
        return this
    }

    fun scrollToCountries(): StatsPage {
        scrollToCard(5, StatsListViewModel.StatsSection.DAYS)
        return this
    }

    fun scrollToVideos(): StatsPage {
        scrollToCard(7, StatsListViewModel.StatsSection.DAYS)
        return this
    }

    fun scrollToFileDownloads(): StatsPage {
        scrollToCard(8, StatsListViewModel.StatsSection.DAYS)
        return this
    }

    fun assertVisits(visitsData: StatsVisitsData): StatsPage {
        val cardStructure = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(visibleCoordinatorLayout),
                ViewMatchers.withId(R.id.stats_block_list),
                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText("Views"),
                        ViewMatchers.hasSibling(ViewMatchers.withText(visitsData.views))
                    )
                ),
                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText("Visitors"),
                        ViewMatchers.hasSibling(ViewMatchers.withText(visitsData.visitors))
                    )
                ),
                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText("Likes"),
                        ViewMatchers.hasSibling(ViewMatchers.withText(visitsData.likes))
                    )
                ),
                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText("Comments"),
                        ViewMatchers.hasSibling(ViewMatchers.withText(visitsData.comments))
                    )
                )
            )
        )
        cardStructure.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        return this
    }

    fun assertKeyValuePairs(cardHeader: String?, list: List<StatsKeyValueData>) {
        for ((key, value) in list) {
            // Element with ID = stats_block_list
            // |--Is a descendant of `coordinator_layout` which `isDisplayed()`
            // |--Has child with text: e.g. "Posts and Pages"
            // |--Has descendant that both:
            //    |- Has text: post.title
            //    |- Has a sibling with post.views (which means they're shown on same row):
            val cardStructure = Espresso.onView(
                Matchers.allOf(
                    ViewMatchers.isDescendantOfA(visibleCoordinatorLayout),
                    ViewMatchers.withId(R.id.stats_block_list),
                    ViewMatchers.hasDescendant(ViewMatchers.withText(cardHeader)),
                    ViewMatchers.hasDescendant(
                        Matchers.allOf(
                            ViewMatchers.withText(key),
                            ViewMatchers.hasSibling(ViewMatchers.withText(value))
                        )
                    )
                )
            )
            cardStructure.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
    }

    fun assertPosts(list: List<StatsKeyValueData>): StatsPage {
        assertKeyValuePairs("Posts and Pages", list)
        return this
    }

    fun assertReferrers(list: List<StatsKeyValueData>): StatsPage {
        assertKeyValuePairs("Referrers", list)
        return this
    }

    fun assertClicks(list: List<StatsKeyValueData>): StatsPage {
        assertKeyValuePairs("Clicks", list)
        return this
    }

    fun assertAuthors(list: List<StatsKeyValueData>): StatsPage {
        assertKeyValuePairs("Authors", list)
        return this
    }

    fun assertCountries(list: List<StatsKeyValueData>): StatsPage {
        assertKeyValuePairs("Countries", list)
        return this
    }

    fun assertVideos(list: List<StatsKeyValueData>): StatsPage {
        assertKeyValuePairs("Videos", list)
        return this
    }

    fun assertDownloads(list: List<StatsKeyValueData>): StatsPage {
        assertKeyValuePairs("File downloads", list)
        return this
    }

    private fun scrollToCard(viewholderPosition: Int, section: StatsListViewModel.StatsSection) {
        WPSupportUtils.idleFor(2000)
        Espresso.onView(Matchers.allOf(
            ViewMatchers.withTagValue(Matchers.`is`(section.name))
        )).perform(
            RecyclerViewActions.scrollToPosition<ViewHolder>(viewholderPosition)
        )
        WPSupportUtils.idleFor(2000)
    }

    companion object {
        // We are interested only in "Stats" screen elements that are descendants of
        // `coordinator_layout` which is actually shown on screen and contains user data.
        // For whatever reason, there's also an instance of "Stats" screen template,
        // which contains the cards headers, but has no user data loaded.
        // It is located at the "right" of the one we need to work with:
        //
        // |--------------------|  |------------------------|
        // | coordinator_layout |  |   coordinator_layout   |
        // |Visible Stats Screen|  | Invisible Stats Screen |
        // |--------------------|  |------------------------|
        //
        private val visibleCoordinatorLayout = Matchers.allOf(
            ViewMatchers.withId(R.id.coordinator_layout),
            ViewMatchers.isDisplayed()
        )
    }
}
