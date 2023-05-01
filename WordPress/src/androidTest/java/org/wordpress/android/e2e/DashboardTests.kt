package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.wordpress.android.e2e.pages.MySitesPage
import org.wordpress.android.support.BaseTest

@HiltAndroidTest
class DashboardTests : BaseTest() {
    @Before
    fun setUp() {
        logoutIfNecessary()
        wpLogin()
    }

    @Test
    fun e2ePublishSimplePost() {
        MySitesPage()
            .go()
            .scrollToDomainsCard()
            .assertDomainsCard()
    }
}
