package org.wordpress.android.e2e;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.util.StatsKeyValueData;
import org.wordpress.android.util.StatsMocksReader;

import java.util.List;

public class StatsTests extends BaseTest {
    @Before
    public void setUp() {
        logoutIfNecessary();
        wpLogin();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void allDayStatsLoad() {
        List<StatsKeyValueData> postsList = new StatsMocksReader().readDayTopPostsToList();
        List<StatsKeyValueData> referrersList = new StatsMocksReader().readDayTopReferrersToList();
        List<StatsKeyValueData> clicksList = new StatsMocksReader().readDayClicksToList();
        List<StatsKeyValueData> authorsList = new StatsMocksReader().readDayAuthorsToList();
        List<StatsKeyValueData> countriesList = new StatsMocksReader().readDayCountriesToList();
        List<StatsKeyValueData> videosList = new StatsMocksReader().readDayVideoPlaysToList();
        List<StatsKeyValueData> downloadsList = new StatsMocksReader().readDayFileDownloadsToList();

        new MySitesPage()
                .go()
                .clickStats()
                .scrollToPosts().assertPosts(postsList)
                .scrollToReferrers().assertReferrers(referrersList)
                .scrollToClicks().assertClicks(clicksList)
                .scrollToAuthors().assertAuthors(authorsList)
                .scrollToCountries().assertCountries(countriesList)
                .scrollToVideos().assertVideos(videosList)
                .scrollToFileDownloads().assertDownloads(downloadsList);
    }
}
