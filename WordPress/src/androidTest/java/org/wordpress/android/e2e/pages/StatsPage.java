package org.wordpress.android.e2e.pages;

import android.view.View;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import org.hamcrest.Matcher;
import org.wordpress.android.R;
import org.wordpress.android.util.StatsKeyValueData;
import org.wordpress.android.util.StatsVisitsData;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.scrollIntoView;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class StatsPage {
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
    private static Matcher<View> visibleCoordinatorLayout = allOf(
            withId(R.id.coordinator_layout),
            isDisplayed()
    );

    public StatsPage openDayStats() {
        ViewInteraction daysStatsTab = onView(allOf(
                isDescendantOfA(withId(R.id.tabLayout)),
                withText("Days")
        ));

        ViewInteraction postsAndPagesCard = onView(allOf(
                isDescendantOfA(visibleCoordinatorLayout),
                withText("Posts and Pages")
        ));

        waitForElementToBeDisplayed(daysStatsTab);
        daysStatsTab.perform(ViewActions.click());
        waitForElementToBeDisplayed(postsAndPagesCard);
        return this;
    }

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
                    isDescendantOfA(visibleCoordinatorLayout),
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
            // |--Is a descendant of `coordinator_layout` which `isDisplayed()`
            // |--Has child with text: e.g. "Posts and Pages"
            // |--Has descendant that both:
            //    |- Has text: post.title
            //    |- Has a sibling with post.views (which means they're shown on same row):
            ViewInteraction cardStructure = onView(allOf(
                    isDescendantOfA(visibleCoordinatorLayout),
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
        ViewInteraction card = onView(allOf(
                isDescendantOfA(visibleCoordinatorLayout),
                withId(R.id.stats_block_list),
                hasDescendant(withText(cardHeader))
                )
        );

        scrollIntoView(R.id.statsPager, card, (float) 0.5);
    }
}
