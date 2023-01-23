package org.wordpress.android.ui.stats.refresh.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider.SiteUpdateResult

@ExperimentalCoroutinesApi
class StatsSiteProviderTest : BaseUnitTest() {
    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository
    private lateinit var statsSiteProvider: StatsSiteProvider
    private lateinit var dispatchCaptor: KArgumentCaptor<Action<SiteModel>>
    private lateinit var firstSite: SiteModel
    private lateinit var secondSite: SiteModel
    private lateinit var selectedSite: SiteModel
    private val firstSiteLocalId = 1
    private val secondSiteLocalId = 2
    private val selectedSiteId = 3

    @Before
    fun setUp() {
        statsSiteProvider = StatsSiteProvider(siteStore, selectedSiteRepository, dispatcher)
        dispatchCaptor = argumentCaptor()
        firstSite = SiteModel()
        secondSite = SiteModel()
        selectedSite = SiteModel()
        firstSite.siteId = 1L
        firstSite.id = firstSiteLocalId
        secondSite.id = secondSiteLocalId
        selectedSite.id = selectedSiteId
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(selectedSite.id)
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
        assertThat(statsSiteProvider.hasLoadedSite()).isFalse
        assertThat(statsSiteProvider.siteModel.id).isEqualTo(0)
    }

    @Test
    fun `hasLoadedSite returns true when current site ID != 0`() {
        statsSiteProvider.start(firstSiteLocalId)

        assertThat(statsSiteProvider.hasLoadedSite()).isTrue
        assertThat(statsSiteProvider.siteModel).isEqualTo(firstSite)
    }

    @Test
    fun `updates site onSiteChange and triggers live data update`() {
        statsSiteProvider.start(secondSiteLocalId)

        var event: SiteUpdateResult? = null
        statsSiteProvider.siteChanged.observeForever {
            event = it.getContentIfNotHandled()
        }

        val siteId = 5L
        secondSite.siteId = siteId

        statsSiteProvider.onSiteChanged(OnSiteChanged(1))

        assertThat(statsSiteProvider.siteModel).isEqualTo(secondSite)
        assertThat(event).isEqualTo(SiteUpdateResult.SiteConnected(siteId))
    }

    @Test
    fun `tries to re-fetch when the updated site id == 0`() {
        statsSiteProvider.start(secondSiteLocalId)
        var event: SiteUpdateResult? = null
        statsSiteProvider.siteChanged.observeForever {
            event = it.getContentIfNotHandled()
        }
        secondSite.siteId = 0L

        statsSiteProvider.onSiteChanged(OnSiteChanged(1))

        verify(dispatcher).dispatch(dispatchCaptor.capture())

        assertThat(dispatchCaptor.firstValue.type).isEqualTo(SiteAction.FETCH_SITE)
        assertThat(dispatchCaptor.firstValue.payload).isEqualTo(secondSite)
        assertThat(event).isNull()
    }

    @Test
    fun `only tries to re-fetch 3 times when the updated site id == 0`() {
        secondSite.siteId = 0L
        statsSiteProvider.start(secondSiteLocalId)
        var event: SiteUpdateResult? = null
        statsSiteProvider.siteChanged.observeForever {
            event = it.getContentIfNotHandled()
        }

        (0..2).forEach {
            statsSiteProvider.onSiteChanged(OnSiteChanged(1))
        }

        verify(dispatcher, times(3)).dispatch(dispatchCaptor.capture())
        assertThat(dispatchCaptor.allValues).hasSize(3)
        assertThat(event).isNull()
    }

    @Test
    fun `fourth re-fetch finishes the activity`() {
        secondSite.siteId = 0L
        statsSiteProvider.start(secondSiteLocalId)
        var event: SiteUpdateResult? = null
        statsSiteProvider.siteChanged.observeForever {
            event = it.getContentIfNotHandled()
        }

        (0..3).forEach {
            statsSiteProvider.onSiteChanged(OnSiteChanged(1))
        }

        verify(dispatcher, times(3)).dispatch(dispatchCaptor.capture())
        assertThat(dispatchCaptor.allValues).hasSize(3)
        assertThat(event).isEqualTo(SiteUpdateResult.NotConnectedJetpackSite)
    }

    @Test
    fun `initialize a site, then reset it`() {
        statsSiteProvider.start(firstSiteLocalId)

        assertThat(statsSiteProvider.siteModel).isEqualTo(firstSite)

        statsSiteProvider.reset()

        assertThat(statsSiteProvider.siteModel).isEqualTo(selectedSite)
    }
}
