package org.wordpress.android.viewmodel.pages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.string
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.test
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.ui.pages.PageItem.Type.DIVIDER
import org.wordpress.android.ui.pages.PageItem.Type.PARENT
import org.wordpress.android.ui.pages.PageItem.Type.TOP_LEVEL_PARENT
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class PageParentViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var pageStore: PageStore
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var viewModel: PageParentViewModel
    private lateinit var listOfParents: List<PageItem>

    @UseExperimental(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        viewModel = PageParentViewModel(
                pageStore = pageStore,
                resourceProvider = resourceProvider,
                defaultScope = TEST_SCOPE)
        listOfParents = setUpPagesList()
    }

    @Test
    fun `when query is empty, all pages are returned`() = test {
        val emptyQuery = ""
        val filtered = viewModel.filterListByQuery(listOfParents, emptyQuery)
        assertThat(filtered.count()).isEqualTo(5)
    }

    @Test
    fun `when no match for query, no pages message is returned`() = test {
        val query = "no match"

        whenever(resourceProvider.getString(string.set_parent_no_match, query))
                .thenReturn("No page with title matching '$query'")

        val filtered = viewModel.filterListByQuery(listOfParents, query)
        assertThat(filtered.count()).isEqualTo(2)

        assertThat((filtered.find { it.type == DIVIDER } as Divider).title).isEqualTo(resourceProvider.getString(
                string.set_parent_no_match, query))
    }

    @Test
    fun `list is filtered`() = test {
        val query = "ou"
        val filtered = viewModel.filterListByQuery(listOfParents, query)
        assertThat(filtered.count()).isEqualTo(3)
    }

    private fun setUpPagesList(): List<PageItem> {
        return listOf(
                ParentPage(0, "Top level", true, TOP_LEVEL_PARENT),
                Divider("Pages"),
                ParentPage(1, "Contact", false, PARENT),
                ParentPage(2, "Home", false, PARENT),
                ParentPage(3, "About", false, PARENT)
        )
    }
}
