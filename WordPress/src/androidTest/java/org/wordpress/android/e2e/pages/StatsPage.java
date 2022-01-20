package org.wordpress.android.e2e.pages;

import androidx.test.espresso.ViewInteraction;

import org.wordpress.android.R;
import org.wordpress.android.util.StatsKeyValueData;
import org.wordpress.android.util.StatsVisitsData;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.isElementCompletelyDisplayed;
import static org.wordpress.android.support.WPSupportUtils.swipeUpOnView;

public class StatsPage {
    public StatsPage scrollToPosts() {
        scrollToCard("Posts and Pages");
        return this;
    }

    public StatsPage scrollToReferrers() {
        scrollToCard("Referrers");
        return this;
    }

    public StatsPage scrollToClicks() {
        scrollToCard("Clicks");
        return this;
    }

    public StatsPage scrollToAuthors() {
        scrollToCard("Authors");
        return this;
    }

    public StatsPage scrollToCountries() {
        scrollToCard("Countries");
        return this;
    }

    public StatsPage scrollToVideos() {
        scrollToCard("Videos");
        return this;
    }

    public StatsPage scrollToFileDownloads() {
        scrollToCard("File downloads");
        return this;
    }

    public StatsPage assertVisits(StatsVisitsData visitsData) {
            ViewInteraction cardStructure = onView(allOf(
                    withId(R.id.stats_block_list),
                    hasDescendant(allOf(
                            withText("Views"),
                            hasSibling(withText(visitsData.getViews()))
                            )
                    ),
                    hasDescendant(allOf(
                            withText("Visitors"),
                            hasSibling(withText(visitsData.getVisitors()))
                            )
                    ),
                    hasDescendant(allOf(
                            withText("Likes"),
                            hasSibling(withText(visitsData.getLikes()))
                            )
                    ),
                    hasDescendant(allOf(
                            withText("Comments"),
                            hasSibling(withText(visitsData.getComments()))
                            )
                    )
            ));

            cardStructure.check(matches(isCompletelyDisplayed()));
            return this;
    }

    public void assertKeyValuePairs(String cardHeader, List<StatsKeyValueData> list) {
        for (StatsKeyValueData item : list) {
            // Element with ID = stats_block_list
            // |--Has child with text: e.g. "Posts and Pages"
            // |--Has descendant that both:
            //    |- Has text: post.title
            //    |- Has a sibling with post.views (which means they're shown on same row):
            ViewInteraction cardStructure = onView(allOf(
                    withId(R.id.stats_block_list),
                    hasDescendant(withText(cardHeader)),
                    hasDescendant(allOf(
                            withText(item.getKey()),
                            hasSibling(withText(item.getValue()))
                            )
                    )
            ));

            cardStructure.check(matches(isCompletelyDisplayed()));
        }
    }

    public StatsPage assertPosts(List<StatsKeyValueData> list) {
        assertKeyValuePairs("Posts and Pages", list);
        return this;
    }

    public StatsPage assertReferrers(List<StatsKeyValueData> list) {
        assertKeyValuePairs("Referrers", list);
        return this;
    }

    public StatsPage assertClicks(List<StatsKeyValueData> list) {
        assertKeyValuePairs("Clicks", list);
        return this;
    }

    public StatsPage assertAuthors(List<StatsKeyValueData> list) {
        assertKeyValuePairs("Authors", list);
        return this;
    }

    public StatsPage assertCountries(List<StatsKeyValueData> list) {
        assertKeyValuePairs("Countries", list);
        return this;
    }

    public StatsPage assertVideos(List<StatsKeyValueData> list) {
        assertKeyValuePairs("Videos", list);
        return this;
    }

    public StatsPage assertDownloads(List<StatsKeyValueData> list) {
        assertKeyValuePairs("File downloads", list);
        return this;
    }

    private void scrollToCard(String cardHeader) {
        // A card that has "stats_block_list"
        // and additionally contains a descendant
        // with needed text:
        ViewInteraction card = onView(allOf(
                withId(R.id.stats_block_list),
                hasDescendant(withText(cardHeader))
                )
        );

        scrollIntoView(card);
    }

    public static void scrollIntoView(ViewInteraction postContainer) {
        int swipeCount = 0;
        while (!isElementCompletelyDisplayed(postContainer) && swipeCount < 15) {
            swipeUpOnView(R.id.statsPager, (float) 1);
            swipeCount += 1;
        }
    }
}
