package org.wordpress.android.ui.stats.refresh.utils

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged

class StatsSiteProviderTest : BaseUnitTest() {
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var localSite: SiteModel
    @Mock lateinit var remoteSite: SiteModel
    private lateinit var statsSiteProvider: StatsSiteProvider
    private val localId = 1
    private val remoteId = 2L
    @Before
    fun setUp() {
        statsSiteProvider = StatsSiteProvider(siteStore, dispatcher)
        whenever(localSite.id).thenReturn(localId)
        whenever(localSite.siteId).thenReturn(0L)
        whenever(remoteSite.siteId).thenReturn(remoteId)
    }

    @Test
    fun `on start attaches provider to dispatcher and sets default site`() {
        statsSiteProvider.start(localSite)

        verify(dispatcher).register(statsSiteProvider)
        assertThat(statsSiteProvider.siteModel).isEqualTo(localSite)
    }

    @Test
    fun `hasLoadedSite returns false when current site ID == 0`() {
        statsSiteProvider.start(localSite)

        assertThat(statsSiteProvider.hasLoadedSite()).isFalse()
        assertThat(statsSiteProvider.siteModel).isEqualTo(localSite)
    }

    @Test
    fun `hasLoadedSite returns true when current site ID != 0`() {
        statsSiteProvider.start(remoteSite)

        assertThat(statsSiteProvider.hasLoadedSite()).isTrue()
        assertThat(statsSiteProvider.siteModel).isEqualTo(remoteSite)
    }

    @Test
    fun `updates site onSiteChange and triggers live data update`() {
        statsSiteProvider.start(localSite)

        whenever(siteStore.getSiteByLocalId(localId)).thenReturn(remoteSite)
        statsSiteProvider.onSiteChanged(OnSiteChanged(1))

        assertThat(statsSiteProvider.siteModel).isEqualTo(remoteSite)
    }
}
