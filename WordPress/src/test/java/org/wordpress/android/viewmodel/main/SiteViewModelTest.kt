package org.wordpress.android.viewmodel.main

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.main.SitePickerMode
import org.wordpress.android.ui.main.SiteViewModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: SiteViewModel

    @Mock
    private lateinit var siteModel1: SiteModel

    @Mock
    private lateinit var siteModel2: SiteModel

    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Before
    fun setUp() {
        viewModel = SiteViewModel(testDispatcher(), siteStore, appPrefsWrapper)

        // mock the SiteRecord
        whenever(siteModel1.id).thenReturn(PINNED_SITE_ID)
        whenever(siteModel1.name).thenReturn("Hello")
        whenever(siteModel1.url).thenReturn("abc.com")

        whenever(siteModel2.id).thenReturn(456)
        whenever(siteModel2.name).thenReturn("World")
        whenever(siteModel2.url).thenReturn("def.com")
    }

    @Test
    fun `given the mode WPCOM_SITES_ONLY, when load sites, then return WPCOM sites`() {
        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(listOf(siteModel2, siteModel1))
        whenever(appPrefsWrapper.pinnedSiteLocalIds).thenReturn(mutableSetOf(PINNED_SITE_ID))

        viewModel.loadSites(SitePickerMode.WPCOM_SITES_ONLY)

        verify(siteStore).sitesAccessedViaWPComRest
        verify(siteStore, never()).sites

        assertThat(viewModel.sites.value).hasSize(2)
        assertThat(viewModel.sites.value!![0].blogName).isEqualTo(siteModel1.name)
        assertThat(viewModel.sites.value!![1].blogName).isEqualTo(siteModel2.name)
    }

    @Test
    fun `given the mode DEFAULT, when load sites, then return all sites`() {
        whenever(siteStore.sites).thenReturn(listOf(siteModel2))
        whenever(appPrefsWrapper.pinnedSiteLocalIds).thenReturn(mutableSetOf(PINNED_SITE_ID))

        viewModel.loadSites(SitePickerMode.DEFAULT)

        verify(siteStore).sites
        verify(siteStore, never()).sitesAccessedViaWPComRest

        assertThat(viewModel.sites.value).hasSize(1)
        assertThat(viewModel.sites.value!![0].blogName).isEqualTo(siteModel2.name)
    }

    @Test
    fun `given the mode SIMPLE, when load sites, then return all sites`() {
        whenever(siteStore.sites).thenReturn(listOf(siteModel1, siteModel2))
        whenever(appPrefsWrapper.pinnedSiteLocalIds).thenReturn(mutableSetOf(PINNED_SITE_ID))

        viewModel.loadSites(SitePickerMode.SIMPLE)

        verify(siteStore).sites
        verify(siteStore, never()).sitesAccessedViaWPComRest

        assertThat(viewModel.sites.value).hasSize(2)
        assertThat(viewModel.sites.value!![0].blogName).isEqualTo(siteModel1.name)
        assertThat(viewModel.sites.value!![1].blogName).isEqualTo(siteModel2.name)
    }

    @Test
    fun `given an empty keyword and mode DEFAULT, when search the keyword, then return all sites`() {
        whenever(siteStore.sites).thenReturn(listOf(siteModel1, siteModel2, siteModel1))

        viewModel.loadSites(SitePickerMode.DEFAULT, "")

        assertThat(viewModel.sites.value).hasSize(3)
    }

    @Test
    fun `given an empty keyword and mode SIMPLE, when search the keyword, then return all sites`() {
        whenever(siteStore.sites).thenReturn(listOf(siteModel1, siteModel2))

        viewModel.loadSites(SitePickerMode.SIMPLE, "")

        assertThat(viewModel.sites.value).hasSize(2)
    }

    @Test
    fun `given an empty keyword and mode WPCOM_SITES_ONLY, when search the keyword, then return all of WPCOM sites`() {
        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(listOf(siteModel1))

        viewModel.loadSites(SitePickerMode.WPCOM_SITES_ONLY, "")

        assertThat(viewModel.sites.value).hasSize(1)
    }

    @Test
    fun `given an keyword and mode DEFAULT, when search the keyword, then return matched sites`() {
        whenever(siteStore.sites).thenReturn(listOf(siteModel1, siteModel1, siteModel2))

        viewModel.loadSites(SitePickerMode.DEFAULT, "he")

        assertThat(viewModel.sites.value).hasSize(2)
        assertThat(viewModel.sites.value!![0].blogName).isEqualTo(siteModel1.name)
        assertThat(viewModel.sites.value!![1].blogName).isEqualTo(siteModel1.name)
    }

    @Test
    fun `given an keyword and mode SIMPLE, when search the keyword, then return a matched site`() {
        whenever(siteStore.sites).thenReturn(listOf(siteModel1, siteModel2))

        viewModel.loadSites(SitePickerMode.SIMPLE, "ld")

        assertThat(viewModel.sites.value).hasSize(1)
        assertThat(viewModel.sites.value!![0].blogName).isEqualTo(siteModel2.name)
    }

    @Test
    fun `given an keyword and mode WPCOM_SITES_ONLY, when search the keyword, then return matched WPCOM sites`() {
        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(listOf(siteModel1, siteModel2, siteModel2))

        viewModel.loadSites(SitePickerMode.WPCOM_SITES_ONLY, "bar")

        assertThat(viewModel.sites.value).hasSize(0)
    }

    companion object {
        private const val PINNED_SITE_ID = 123
    }
}
