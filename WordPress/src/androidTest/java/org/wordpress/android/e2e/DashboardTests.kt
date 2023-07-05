package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
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
    @Ignore
    fun e2eDomainsCardNavigation() {
        MySitesPage()
            .scrollToDomainsCard()
            .assertDomainsCard()
            .tapDomainsCard()
            .assertDomainsScreenLoaded()
    }

    @Test
    @Ignore
    fun e2ePagesCardNavigation() {
        MySitesPage()
            .scrollToPagesCard()
            .assertPagesCard()
            .assertPagesCardHasPage("Blog")
            .assertPagesCardHasPage("Cart")
            .assertPagesCardHasPage("Shop")
            .tapPagesCard()
            .assertPagesScreenLoaded()
            .assertPagesScreenHasPage("Blog")
            .assertPagesScreenHasPage("Cart")
            .assertPagesScreenHasPage("Shop")
    }

    @Test
    @Ignore
    fun e2eActivityLogCardNavigation() {
        MySitesPage()
            .scrollToActivityLogCard()
            .assertActivityLogCard()
            .assertActivityLogCardHasActivity("Enabled Jetpack Social")
            .assertActivityLogCardHasActivity("The Jetpack connection")
            .assertActivityLogCardHasActivity("This site is connected to")
            .tapActivity("The Jetpack connection")
            .assertEventScreenLoaded()
            .assertEventScreenHasActivity("The Jetpack connection is now complete. Welcome!")
    }
}
