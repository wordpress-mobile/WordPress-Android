package org.wordpress.android.viewmodel.pages

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import java.util.Date
import java.util.Locale

private const val HOUR_IN_MILLISECONDS = 3600000L

class PageListViewModelTest : BaseUnitTest() {
    @Mock lateinit var mediaStore: MediaStore
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var pagesViewModel: PagesViewModel
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    private lateinit var viewModel: PageListViewModel
    private val site = SiteModel()
    private val pageListState = MutableLiveData<PageListState>()
    @Before
    fun setUp() {
        viewModel = PageListViewModel(mediaStore, dispatcher, localeManagerWrapper, Dispatchers.Unconfined)

        whenever(pagesViewModel.arePageActionsEnabled).thenReturn(false)
        whenever(pagesViewModel.site).thenReturn(site)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.getDefault())
        site.id = 10
        pageListState.value = PageListState.DONE
    }

    @Test
    fun `on pages updates published model`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Pair<List<PageItem>, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val date = Date()

        pages.value = listOf(buildPageModel(1, date))

        assertThat(result).hasSize(1)
        val pageItems = result[0].first
        assertThat(pageItems).hasSize(3)
        val firstItem = pageItems[0] as PublishedPage
        assertThat(firstItem.title).isEqualTo("Title 01")
        assertThat(firstItem.date).isEqualTo(date)
        assertThat(firstItem.actionsEnabled).isEqualTo(false)
        assertDivider(pageItems[1])
        assertDivider(pageItems[2])
    }

    @Test
    fun `sorts 100 or more pages by date DESC`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Pair<List<PageItem>, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val earlyPages = (0..30).map { buildPageModel(it, Date(HOUR_IN_MILLISECONDS * it)) }
        val latePages = (31..60).map { buildPageModel(it, Date(100 * HOUR_IN_MILLISECONDS * it)) }
        val middlePages = (61..96).map { buildPageModel(it, Date(10 * HOUR_IN_MILLISECONDS * it)) }
        val earlyChild = buildPageModel(97, Date(40 * HOUR_IN_MILLISECONDS), earlyPages[0])
        val middleChild = buildPageModel(98, Date(1000 * HOUR_IN_MILLISECONDS), middlePages[0])
        val lateChild = buildPageModel(99, Date(7000 * HOUR_IN_MILLISECONDS), latePages[0])
        val children = listOf(middleChild, earlyChild, lateChild)

        val pageModels = mutableListOf<PageModel>()
        pageModels.addAll(middlePages)
        pageModels.addAll(latePages)
        pageModels.addAll(children)
        pageModels.addAll(earlyPages)
        pages.value = pageModels

        assertThat(result).hasSize(1)
        val pageItems = result[0].first
        assertThat(pageItems).hasSize(102)
        assertPublishedPage(pageItems[0], lateChild)
        for (index in 1..latePages.size) {
            assertPublishedPage(pageItems[index], latePages[latePages.size - index])
        }
        assertPublishedPage(pageItems[31], middleChild)
        for (index in 1..middlePages.size) {
            assertPublishedPage(pageItems[31 + index], middlePages[middlePages.size - index])
        }
        assertPublishedPage(pageItems[68], earlyChild)
        for (index in 1..earlyPages.size) {
            assertPublishedPage(pageItems[68 + index], earlyPages[earlyPages.size - index])
        }
        assertDivider(pageItems[100])
        assertDivider(pageItems[101])
    }

    @Test
    fun `sorts up to 99 pages topologically`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Pair<List<PageItem>, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val earlyPages = (0..30).map { buildPageModel(it, Date(HOUR_IN_MILLISECONDS * it)) }
        val latePages = (31..60).map { buildPageModel(it, Date(100 * HOUR_IN_MILLISECONDS * it)) }
        val middlePages = (61..95).map { buildPageModel(it, Date(10 * HOUR_IN_MILLISECONDS * it)) }
        val earlyChild = buildPageModel(96, Date(40 * HOUR_IN_MILLISECONDS), earlyPages[0])
        val middleChild = buildPageModel(97, Date(1000 * HOUR_IN_MILLISECONDS), middlePages[0])
        val lateChild = buildPageModel(98, Date(7000 * HOUR_IN_MILLISECONDS), latePages[0])
        val children = listOf(middleChild, earlyChild, lateChild)

        val pageModels = mutableListOf<PageModel>()
        pageModels.addAll(middlePages)
        pageModels.addAll(latePages)
        pageModels.addAll(children)
        pageModels.addAll(earlyPages)
        pages.value = pageModels

        assertThat(result).hasSize(1)
        val pageItems = result[0].first
        assertThat(pageItems).hasSize(101)
        assertPublishedPage(pageItems[0], earlyPages[0], 0)
        assertPublishedPage(pageItems[1], earlyChild, 1)
        for (index in 1 until earlyPages.size) {
            assertPublishedPage(pageItems[index + 1], earlyPages[index], 0)
        }
        assertPublishedPage(pageItems[32], latePages[0], 0)
        assertPublishedPage(pageItems[33], lateChild, 1)
        for (index in 1 until latePages.size) {
            assertPublishedPage(pageItems[index + 33], latePages[index], 0)
        }
        assertPublishedPage(pageItems[63], middlePages[0], 0)
        assertPublishedPage(pageItems[64], middleChild, 1)
        for (index in 1 until middlePages.size) {
            assertPublishedPage(pageItems[index + 64], middlePages[index], 0)
        }
        assertDivider(pageItems[99])
        assertDivider(pageItems[100])
    }

    @Test
    fun `sorts pages ignoring case`() {
        val pages = MutableLiveData<List<PageModel>>()
        whenever(pagesViewModel.pages).thenReturn(pages)

        viewModel.start(PUBLISHED, pagesViewModel)

        val result = mutableListOf<Pair<List<PageItem>, Boolean>>()

        viewModel.pages.observeForever { result.add(it) }

        val firstPage = buildPageModel(0, pageTitle = "ab")
        val secondPage = buildPageModel(0, pageTitle = "Ac")

        val pageModels = mutableListOf<PageModel>()
        pageModels += secondPage
        pageModels += firstPage
        pages.value = pageModels

        assertThat(result).hasSize(1)
        assertThat((result[0].first[0] as PublishedPage).title).isEqualTo(firstPage.title)
        assertThat((result[0].first[1] as PublishedPage).title).isEqualTo(secondPage.title)
    }

    private fun buildPageModel(
        id: Int,
        date: Date = Date(0),
        parent: PageModel? = null,
        pageTitle: String? = null
    ): PageModel {
        val title = pageTitle ?: if (id < 10) "Title 0$id" else "Title $id"
        return PageModel(site, id, title, PageStatus.PUBLISHED, date, false, id.toLong(), parent, id.toLong())
    }

    private fun assertDivider(pageItem: PageItem) {
        assertThat(pageItem is Divider).isTrue()
    }

    private fun assertPublishedPage(pageItem: PageItem, pageModel: PageModel, indent: Int = 0) {
        val publishedPage = pageItem as PublishedPage
        assertThat(publishedPage.title).isEqualTo(pageModel.title)
        assertThat(publishedPage.date).isEqualTo(pageModel.date)
        assertThat(publishedPage.indent).isEqualTo(indent)
        assertThat(publishedPage.actionsEnabled).isEqualTo(false)
    }
}
