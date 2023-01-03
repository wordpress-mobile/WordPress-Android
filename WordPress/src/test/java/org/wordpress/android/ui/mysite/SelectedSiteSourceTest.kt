package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite

@ExperimentalCoroutinesApi
class SelectedSiteSourceTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var dispatcher: Dispatcher
    private lateinit var source: SelectedSiteSource
    private val siteLocalId = 1
    private val site = SiteModel()

    private lateinit var result: MutableList<SelectedSite>
    private val onSiteChange = MutableLiveData<SiteModel>()
    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() {
        site.id = siteLocalId
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(onSiteChange)
        initSelectedSiteSource()
        result = mutableListOf()
        isRefreshing = mutableListOf()
    }

    @Test
    fun `when a new site is selected, then source data is not null`() = test {
        onSiteChange.value = site

        source.build(testScope(), siteLocalId).observeForever { result.add(it) }

        assertThat(result.last().site).isNotNull
    }

    @Test
    fun `given selected site, when refresh is invoked, then remote request is dispatched`() = test {
        initSelectedSiteSource(hasSelectedSite = true)

        source.refresh()

        verify(dispatcher, times(1)).dispatch(any())
    }

    @Test
    fun `given no selected site, when refresh is invoked, then remote request is not dispatched`() = test {
        initSelectedSiteSource(hasSelectedSite = false)

        source.refresh()

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `given selected site, when build is invoked, then refresh changes from true to false`() = test {
        initSelectedSiteSource(hasSelectedSite = true)
        source.refresh.observeForever { isRefreshing.add(it) }

        assertThat(isRefreshing.last()).isTrue

        source.build(testScope(), siteLocalId).observeForever { result.add(it) }

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `given selected site, when refresh is invoked, then refresh is true`() = test {
        initSelectedSiteSource(hasSelectedSite = true)
        source.refresh.observeForever { isRefreshing.add(it) }

        source.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `given no selected site, when refresh is invoked, then refresh is false`() = test {
        initSelectedSiteSource(hasSelectedSite = false)
        source.refresh.observeForever { isRefreshing.add(it) }

        source.refresh()

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `when a onSiteChanged event received, then refresh changes from true to false`() = test {
        source.refresh.observeForever { isRefreshing.add(it) }
        source.refresh.value = true

        assertThat(isRefreshing.last()).isTrue

        source.onSiteChanged(OnSiteChanged(1, null))

        assertThat(isRefreshing.last()).isFalse
    }

    private fun initSelectedSiteSource(hasSelectedSite: Boolean = true) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(if (hasSelectedSite) site else null)
        whenever(selectedSiteRepository.hasSelectedSite()).thenReturn(hasSelectedSite)
        source = SelectedSiteSource(selectedSiteRepository, dispatcher)
    }
}
