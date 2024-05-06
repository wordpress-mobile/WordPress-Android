package org.wordpress.android.e2e

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.e2e.pages.MySitesPage
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.ComposeEspressoLink
import org.wordpress.android.support.WPSupportUtils
import org.wordpress.android.wiremock.WireMockStub
import org.wordpress.android.wiremock.WireMockUrlPath

@HiltAndroidTest
class StatsTests : BaseTest(listOf(WireMockStub(urlPath = WireMockUrlPath.FEATURE_RESPONSE, fileName = "new-stats-feature-response.json"))) {
    @Before
    fun setUp() {
        Assume.assumeTrue(BuildConfig.IS_JETPACK_APP)
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
        MySitesPage()
            .go()
            .goToStats()
            .hasNewStatTabs()
    }
}
