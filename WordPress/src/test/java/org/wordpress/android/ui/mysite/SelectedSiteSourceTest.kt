package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite

class SelectedSiteSourceTest : BaseUnitTest() {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var dispatcher: Dispatcher
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
        source = SelectedSiteSource(selectedSiteRepository, dispatcher)
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
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        source.refresh()

        verify(dispatcher, times(1)).dispatch(any())
    }

    @Test
    fun `given no selected site, when refresh is invoked, then remote request is not dispatched`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        source.refresh()

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `when buildSource is invoked, then refresh is false`() = test {
        source.refresh.observeForever { isRefreshing.add(it) }

        source.build(testScope(), siteLocalId).observeForever { result.add(it) }

        assertThat(isRefreshing.last()).isFalse
    }

    @Test
    fun `given selected site, when refresh is invoked, then refresh is true`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        source.refresh.observeForever { isRefreshing.add(it) }

        source.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `given no selected site, when refresh is invoked, then refresh is false`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
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
}
