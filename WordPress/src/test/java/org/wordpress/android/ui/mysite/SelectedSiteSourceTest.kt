package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite

class SelectedSiteSourceTest : BaseUnitTest() {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    private lateinit var source: SelectedSiteSource
    private val siteLocalId = 1
    private val site = SiteModel()

    private lateinit var result: MutableList<SelectedSite>
    private val onSiteChange = MutableLiveData<SiteModel>()

    @Before
    fun setUp() {
        site.id = siteLocalId
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(onSiteChange)
        source = SelectedSiteSource(selectedSiteRepository)
        result = mutableListOf()
    }

    @Test
    fun `when a new site is selected, then source data is not null`() = test {
        onSiteChange.value = site

        source.buildSource(testScope(), siteLocalId).observeForever { result.add(it) }

        assertThat(result.last().site).isNotNull
    }
}
