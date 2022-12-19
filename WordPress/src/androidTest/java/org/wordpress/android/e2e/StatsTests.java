package org.wordpress.android.e2e;

import androidx.test.espresso.Espresso;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.util.StatsKeyValueData;
import org.wordpress.android.util.StatsMocksReader;
import org.wordpress.android.util.StatsVisitsData;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class StatsTests extends BaseTest {
    @Before
    public void setUp() {
        logoutIfNecessary();
        wpLogin();
    }

    @After
    public void tearDown() {
        // "tabLayout" is a Tab switcher for stats.
        // We need to leave stats at the end of test.
        if (isElementDisplayed(onView(withId(R.id.tabLayout)))) {
            Espresso.pressBack();
        }
    }

    @Test
    public void e2eAllDayStatsLoad() {
        StatsVisitsData todayVisits = new StatsVisitsData("97", "28", "14", "11");
        List<StatsKeyValueData> postsList = new StatsMocksReader().readDayTopPostsToList();
        List<StatsKeyValueData> referrersList = new StatsMocksReader().readDayTopReferrersToList();
        List<StatsKeyValueData> clicksList = new StatsMocksReader().readDayClicksToList();
        List<StatsKeyValueData> authorsList = new StatsMocksReader().readDayAuthorsToList();
        List<StatsKeyValueData> countriesList = new StatsMocksReader().readDayCountriesToList();
        List<StatsKeyValueData> videosList = new StatsMocksReader().readDayVideoPlaysToList();
        List<StatsKeyValueData> downloadsList = new StatsMocksReader().readDayFileDownloadsToList();

        new MySitesPage()
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
                .scrollToFileDownloads().assertDownloads(downloadsList);
    }
}
