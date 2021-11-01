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
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.ShowSiteIconProgressBar

class SiteIconProgressSourceTest : BaseUnitTest() {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    private lateinit var source: SiteIconProgressSource
    private val siteLocalId = 1
    private val site = SiteModel()

    private lateinit var result: MutableList<ShowSiteIconProgressBar>

    private var siteIconProgressBarVisible: Boolean = false
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()

    @Before
    fun setUp() {
        site.id = siteLocalId
        whenever(selectedSiteRepository.showSiteIconProgressBar).thenReturn(onShowSiteIconProgressBar)
        source = SiteIconProgressSource(selectedSiteRepository)
        result = mutableListOf()
    }

    @Test
    fun `when source site, then icon progress bar is not visible`() = test {
        onShowSiteIconProgressBar.value = false

        source.buildSource(testScope(), siteLocalId).observeForever { result.add(it) }

        assertThat(siteIconProgressBarVisible).isFalse
    }
}
