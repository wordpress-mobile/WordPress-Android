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
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider.SelectedSiteStorage

class StatsSiteProviderTest : BaseUnitTest() {
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var firstSite: SiteModel
    @Mock lateinit var secondSite: SiteModel
    @Mock lateinit var selectedSite: SiteModel
    @Mock lateinit var siteStorage: SelectedSiteStorage
    private lateinit var statsSiteProvider: StatsSiteProvider
    private val firstSiteLocalId = 1
    private val secondSiteLocalId = 2
    private val selectedSiteId = 3

    @Before
    fun setUp() {
        statsSiteProvider = StatsSiteProvider(siteStore, siteStorage, dispatcher)
        whenever(firstSite.siteId).thenReturn(1L)
        whenever(siteStorage.currentLocalSiteId).thenReturn(selectedSiteId)
        whenever(siteStore.getSiteByLocalId(firstSiteLocalId)).thenReturn(firstSite)
        whenever(siteStore.getSiteByLocalId(secondSiteLocalId)).thenReturn(secondSite)
        whenever(siteStore.getSiteByLocalId(selectedSiteId)).thenReturn(selectedSite)
    }

    @Test
    fun `on start attaches provider to dispatcher and sets default site`() {
        statsSiteProvider.start(firstSiteLocalId)

        verify(dispatcher).register(statsSiteProvider)
        assertThat(statsSiteProvider.siteModel).isEqualTo(firstSite)
    }

    @Test
    fun `hasLoadedSite returns false when current site ID == 0`() {
        assertThat(statsSiteProvider.hasLoadedSite()).isFalse()
        assertThat(statsSiteProvider.siteModel.id).isEqualTo(0)
    }

    @Test
    fun `hasLoadedSite returns true when current site ID != 0`() {
        statsSiteProvider.start(firstSiteLocalId)

        assertThat(statsSiteProvider.hasLoadedSite()).isTrue()
        assertThat(statsSiteProvider.siteModel).isEqualTo(firstSite)
    }

    @Test
    fun `updates site onSiteChange and triggers live data update`() {
        statsSiteProvider.start(secondSiteLocalId)

        statsSiteProvider.onSiteChanged(OnSiteChanged(1))

        assertThat(statsSiteProvider.siteModel).isEqualTo(selectedSite)
    }

    @Test
    fun `initialize a site, then reset it`() {
        statsSiteProvider.start(firstSiteLocalId)

        assertThat(statsSiteProvider.siteModel).isEqualTo(firstSite)

        statsSiteProvider.reset()

        assertThat(statsSiteProvider.siteModel).isEqualTo(selectedSite)
    }
}
