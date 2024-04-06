package org.wordpress.android.e2e

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.e2e.pages.MySitesPage
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.ComposeEspressoLink
import org.wordpress.android.support.WPSupportUtils
import org.wordpress.android.util.StatsKeyValueData
import org.wordpress.android.util.StatsMocksReader
import org.wordpress.android.util.StatsVisitsData

@HiltAndroidTest
class StatsGranularTabsTest : BaseTest() {
    @Before
    fun setUp() {
        assumeTrue(BuildConfig.IS_JETPACK_APP)
        ComposeEspressoLink().unregister()
        logoutIfNecessary()
        wpLogin()
    }

    @After
    fun tearDown() {
        // "tabLayout" is a Tab switcher for stats.
        // We need to leave stats at the end of test.
        if (WPSupportUtils.isElementDisplayed(Espresso.onView(ViewMatchers.withId(R.id.tabLayout)))) {
            Espresso.pressBack()
        }
    }

    @Test
    fun e2eAllDayStatsLoad() {
        val todayVisits = StatsVisitsData("97", "28", "14", "11")
        val postsList: List<StatsKeyValueData> = StatsMocksReader().readDayTopPostsToList()
        val referrersList: List<StatsKeyValueData> = StatsMocksReader().readDayTopReferrersToList()
        val clicksList: List<StatsKeyValueData> = StatsMocksReader().readDayClicksToList()
        val authorsList: List<StatsKeyValueData> = StatsMocksReader().readDayAuthorsToList()
        val countriesList: List<StatsKeyValueData> = StatsMocksReader().readDayCountriesToList()
        val videosList: List<StatsKeyValueData> = StatsMocksReader().readDayVideoPlaysToList()
        val downloadsList: List<StatsKeyValueData> = StatsMocksReader().readDayFileDownloadsToList()
        MySitesPage()
            .go()
            .goToStats()
            .openDayStats()
            .assertVisits(todayVisits)
            .scrollToPosts().assertPosts(postsList)
            .scrollToReferrers().assertReferrers(referrersList)
            .scrollToClicks().assertClicks(clicksList)
            .scrollToAuthors().assertAuthors(authorsList)
            .scrollToCountries().assertCountries(countriesList)
            .scrollToVideos().assertVideos(videosList)
            .scrollToFileDownloads().assertDownloads(downloadsList)
    }
}
