package org.wordpress.android.e2e

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.e2e.pages.MySitesPage
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.WPSupportUtils
import org.wordpress.android.util.StatsKeyValueData
import org.wordpress.android.util.StatsMocksReader
import org.wordpress.android.util.StatsVisitsData

@HiltAndroidTest
class StatsTests : BaseTest() {
    @Before
    fun setUp() {
        // We're not running Stats tests for JP.
        // See https://github.com/wordpress-mobile/WordPress-Android/issues/18065
        assumeTrue(!BuildConfig.IS_JETPACK_APP)

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
    @Ignore("Skipped due to increased flakiness. See build-and-ship channel for 17.05.2023")
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
