package org.wordpress.android.workers

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore

@RunWith(MockitoJUnitRunner::class)
class CreateSiteNotificationHandlerTest {
    @Mock
    lateinit var accountStore: AccountStore
    @Mock
    lateinit var siteStore: SiteStore

    @Before
    fun setUp() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSite()).thenReturn(false)
    }

    @Test
    fun verifyShouldShowNotification() {
        assertThat(accountStore.hasAccessToken() && !siteStore.hasSite()).isTrue
    }

    @Test
    fun verifyShouldNotShowNotification() {
        assertThat(accountStore.hasAccessToken() && siteStore.hasSite()).isFalse
    }

    @Test
    fun verifyDoesNotShowNotification() {
        assertThat(!accountStore.hasAccessToken() && !siteStore.hasSite()).isFalse
    }
}
