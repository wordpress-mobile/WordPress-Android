package org.wordpress.android.ui.newstats

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository

@RunWith(MockitoJUnitRunner::class)
class NewStatsSiteSelectionTest {
    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Test
    fun `leaves the selection alone when the Intent names no site`() {
        selectNewStatsSiteIfNeeded(0, siteStore, selectedSiteRepository)

        verify(selectedSiteRepository, never()).updateSite(any())
    }

    @Test
    fun `leaves the selection alone when the named site is already selected`() {
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(SITE_ID)

        selectNewStatsSiteIfNeeded(SITE_ID, siteStore, selectedSiteRepository)

        verify(selectedSiteRepository, never()).updateSite(any())
    }

    @Test
    fun `selects the named site when a different one is selected`() {
        val site = SiteModel().apply { id = SITE_ID }
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(OTHER_SITE_ID)
        whenever(siteStore.getSiteByLocalId(SITE_ID)).thenReturn(site)

        selectNewStatsSiteIfNeeded(SITE_ID, siteStore, selectedSiteRepository)

        verify(selectedSiteRepository).updateSite(site)
    }

    @Test
    fun `leaves the selection alone when the named site cannot be resolved`() {
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(OTHER_SITE_ID)
        whenever(siteStore.getSiteByLocalId(SITE_ID)).thenReturn(null)

        selectNewStatsSiteIfNeeded(SITE_ID, siteStore, selectedSiteRepository)

        verify(selectedSiteRepository, never()).updateSite(any())
    }

    companion object {
        private const val SITE_ID = 42
        private const val OTHER_SITE_ID = 7
    }
}
