package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.wordpress.android.e2e.pages.MySitesPage
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.ComposeEspressoLink
import org.wordpress.android.test.BuildConfig

@HiltAndroidTest
class DashboardTests : BaseTest() {
    @Before
    fun setUp() {
        ComposeEspressoLink().unregister()
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

    @Test
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
