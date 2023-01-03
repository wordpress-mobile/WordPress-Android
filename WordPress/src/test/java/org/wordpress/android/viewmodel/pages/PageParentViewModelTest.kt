package org.wordpress.android.viewmodel.pages

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PageParentViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var pageStore: PageStore

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var site: SiteModel
    private lateinit var viewModel: PageParentViewModel

    @Before
    fun setUp() {
        viewModel = PageParentViewModel(
            pageStore,
            resourceProvider,
            testDispatcher(),
            testDispatcher()
        )

        runBlocking {
            whenever(pageStore.getPagesFromDb(site)).thenReturn(fakePublishedPageList())
            whenever(resourceProvider.getString(any())).thenReturn("Pages")
            mockPageStoreGetPages()
        }
    }

    @Test
    fun `initial and current parent are top level for newly created page that was never saved (no id)`() {
        viewModel.start(site, 0L)

        assertThat(viewModel.initialParent).matches { it.id == 0L }
        assertThat(viewModel.currentParent).matches { it.id == 0L }
    }

    @Test
    fun `initial and current parent are top level for top level page`() {
        viewModel.start(site, 3L)

        assertThat(viewModel.initialParent).matches { it.id == 0L }
        assertThat(viewModel.currentParent).matches { it.id == 0L }
    }

    @Test
    fun `initial and current parent are page parent for nested page`() {
        viewModel.start(site, 4L)

        assertThat(viewModel.initialParent).matches { it.id == 3L }
        assertThat(viewModel.currentParent).matches { it.id == 3L }
    }

    @Test
    fun `page list shows all pages except local-only pages for newly created page that was never saved (no id)`() {
        val validIds = fakePublishedPageList()
            .map { it.remoteId }
            .filterNot { it < 0 }

        viewModel.start(site, 0L)

        val shownPageIds = viewModel.pages.value
            ?.mapNotNull { (it as? ParentPage)?.id }
            ?: emptyList()

        assertThat(shownPageIds).first().isEqualTo(0L)
        assertThat(shownPageIds).containsAll(validIds)
    }

    @Test
    fun `page list shows all pages except for itself, children and local-only pages`() {
        val pageId = 3L
        val childrenIds = listOf(4L, 5L)
        val validIds = fakePublishedPageList()
            .map { it.remoteId }
            .filterNot { it < 0 || it == pageId || it in childrenIds }

        viewModel.start(site, pageId)

        val shownPageIds = viewModel.pages.value
            ?.mapNotNull { (it as? ParentPage)?.id }
            ?: emptyList()

        assertThat(shownPageIds).first().isEqualTo(0L)
        assertThat(shownPageIds).containsAll(validIds)
    }

    @Test
    fun `current parent is updated when selecting new parent`() = test {
        val newParentId = 2L

        viewModel.start(site, 1L)
        val newParent = viewModel.pages.value?.first { (it as? ParentPage)?.id == newParentId } as ParentPage

        assertThat(viewModel.currentParent).matches { it.id == 0L }

        viewModel.onParentSelected(newParent)

        assertThat(viewModel.currentParent).matches { it.id == newParentId }
    }

    @Test
    fun `save button is visible when selecting new parent`() = test {
        val newParentId = 2L

        viewModel.start(site, 1L)
        val newParent = viewModel.pages.value?.first { (it as? ParentPage)?.id == newParentId } as ParentPage

        assertThat(viewModel.isSaveButtonVisible.value).isFalse

        viewModel.onParentSelected(newParent)

        assertThat(viewModel.isSaveButtonVisible.value).isTrue
    }

    @Test
    fun `save button is not visible when selecting the initial parent`() = test {
        val newParentId = 2L

        viewModel.start(site, 1L)
        val initialParent = viewModel.initialParent
        val newParent = viewModel.pages.value?.first { (it as? ParentPage)?.id == newParentId } as ParentPage

        viewModel.onParentSelected(newParent)
        assertThat(viewModel.isSaveButtonVisible.value).isTrue

        viewModel.onParentSelected(initialParent)
        assertThat(viewModel.isSaveButtonVisible.value).isFalse
    }

    private suspend fun mockPageStoreGetPages() {
        val pages = fakePublishedPageList()

        whenever(pageStore.getPageByRemoteId(any(), any())).thenAnswer {
            val id = it.arguments[0] as Long
            pages.firstOrNull { page -> page.remoteId == id }
        }
    }

    private fun fakePublishedPageList(): List<PageModel> {
        val page1 = fakePublishedPageModel(1L, "Page 1")
        val page2 = fakePublishedPageModel(2L, "Page 2")
        val page3 = fakePublishedPageModel(3L, "Page 3")
        val page3A = fakePublishedPageModel(4L, "Page 3A", parent = page3)
        val page3B = fakePublishedPageModel(5L, "Page 3B", parent = page3A)
        val localPage = fakePublishedPageModel(-1L, "Local Page")

        return listOf(page1, page2, page3, page3A, page3B, localPage)
    }

    private fun fakePublishedPageModel(id: Long, title: String, parent: PageModel? = null): PageModel {
        return PageModel(
            post = PostModel().apply { },
            site = site,
            pageId = id.toInt(),
            title = title,
            status = PageStatus.PUBLISHED,
            date = Calendar.getInstance().time,
            hasLocalChanges = false,
            remoteId = id,
            parent = parent,
            featuredImageId = 0L
        )
    }
}
