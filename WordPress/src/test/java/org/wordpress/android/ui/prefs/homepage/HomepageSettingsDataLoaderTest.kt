package org.wordpress.android.ui.prefs.homepage

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.collect
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PageStore.OnPageChanged
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult

@RunWith(MockitoJUnitRunner::class)
class HomepageSettingsDataLoaderTest {
    @Mock lateinit var pageStore: PageStore
    private val siteModel = SiteModel()
    private lateinit var homepageSettingsDataLoader: HomepageSettingsDataLoader

    @Before
    fun setUp() {
        homepageSettingsDataLoader = HomepageSettingsDataLoader(pageStore)
    }

    @Test
    fun `loads PUBLISHED pages from DB and from API`() = test {
        val dbPage = mock<PageModel>()
        val remotePage = mock<PageModel>()
        whenever(dbPage.status).thenReturn(PUBLISHED)
        whenever(remotePage.status).thenReturn(PUBLISHED)
        val databasePages = listOf(dbPage)
        val remotePages = listOf(remotePage)
        whenever(pageStore.getPagesFromDb(siteModel)).thenReturn(databasePages, remotePages)
        whenever(pageStore.requestPagesFromServer(siteModel, false)).thenReturn(OnPageChanged.Success)
        val results = mutableListOf<LoadingResult>()

        homepageSettingsDataLoader.loadPages(siteModel).collect { results.add(it) }

        assertThat(results).containsExactly(
                LoadingResult.Loading,
                LoadingResult.Data(databasePages),
                LoadingResult.Data(remotePages)
        )
    }

    @Test
    fun `filters out non-published pages`() = test {
        val publishedPage = mock<PageModel>()
        whenever(publishedPage.status).thenReturn(PUBLISHED)
        val nonPublishedPage = mock<PageModel>()
        val expectedPages = listOf(publishedPage)
        whenever(pageStore.getPagesFromDb(siteModel)).thenReturn(listOf(publishedPage, nonPublishedPage))
        whenever(pageStore.requestPagesFromServer(siteModel, false)).thenReturn(OnPageChanged.Success)
        val results = mutableListOf<LoadingResult>()

        homepageSettingsDataLoader.loadPages(siteModel).collect { results.add(it) }

        assertThat(results).containsExactly(
                LoadingResult.Loading,
                LoadingResult.Data(expectedPages),
                LoadingResult.Data(expectedPages)
        )
    }

    @Test
    fun `returns error when remote call fails`() = test {
        val pages = listOf<PageModel>()
        whenever(pageStore.getPagesFromDb(siteModel)).thenReturn(pages)
        whenever(pageStore.requestPagesFromServer(siteModel, false)).thenReturn(
                OnPageChanged.Error(PostError(GENERIC_ERROR, "Generic error"))
        )
        val results = mutableListOf<LoadingResult>()

        homepageSettingsDataLoader.loadPages(siteModel).collect { results.add(it) }

        assertThat(results).containsExactly(
                LoadingResult.Loading,
                LoadingResult.Data(emptyList()),
                LoadingResult.Error(R.string.site_settings_failed_to_load_pages)
        )
    }
}
