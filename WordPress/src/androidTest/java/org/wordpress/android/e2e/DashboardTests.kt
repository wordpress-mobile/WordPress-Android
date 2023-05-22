package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.wordpress.android.e2e.pages.MySitesPage
import org.wordpress.android.support.BaseTest
import org.wordpress.android.test.BuildConfig

@HiltAndroidTest
class DashboardTests : BaseTest() {
    @Before
    fun setUp() {
        // We run the class for JP only (so far the class contains
        // only a test for Domains card, which in not valid for WP)
        assumeTrue(BuildConfig.IS_JETPACK_APP)

        logoutIfNecessary()
        wpLogin()
    }

    @Test
    fun e2eDomainsCardNavigation() {
        MySitesPage()
            .scrollToDomainsCard()
            .assertDomainsCard()
            .tapDomainsCard()
            .assertDomainsScreenLoaded()
    }
}
