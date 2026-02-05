package org.wordpress.android.ui.navmenus

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.navmenus.models.NavMenuItemModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.navmenus.data.NavMenuRestClient

@ExperimentalCoroutinesApi
class NavMenusViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var navMenuRestClient: NavMenuRestClient

    @Mock
    lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: NavMenusViewModel

    private val testSite = SiteModel().apply {
        id = 123
        siteId = 456L
    }

    @Before
    fun setup() {
        whenever(resourceProvider.getString(any())).thenAnswer { invocation ->
            "String resource ${invocation.arguments[0]}"
        }
        setupDefaultLinkableItemsMocks()
        viewModel = NavMenusViewModel(
            selectedSiteRepository = selectedSiteRepository,
            navMenuRestClient = navMenuRestClient,
            resourceProvider = resourceProvider,
            mainDispatcher = testDispatcher(),
            ioDispatcher = testDispatcher()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupDefaultLinkableItemsMocks() = test {
        val emptySuccess = NavMenuRestClient.LinkableItemsResult.Success(
            items = emptyList(),
            canLoadMore = false
        )
        lenient().`when`(navMenuRestClient.fetchPosts(any(), any())).thenReturn(emptySuccess)
        lenient().`when`(navMenuRestClient.fetchPages(any(), any())).thenReturn(emptySuccess)
        lenient().`when`(navMenuRestClient.fetchCategories(any(), any())).thenReturn(emptySuccess)
        lenient().`when`(navMenuRestClient.fetchTags(any(), any())).thenReturn(emptySuccess)
    }

    @Test
    fun `when loadMenus called without site, then error state is set`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.loadMenus()

        val state = viewModel.menuListState.first()
        assertThat(state.error).isNotNull()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `when navigateToCreateMenu called, then menu detail state is initialized for new menu`() = test {
        viewModel.navigateToCreateMenu()

        val state = viewModel.menuDetailState.first()
        assertThat(state).isNotNull
        assertThat(state?.menuId).isEqualTo(0L)
        assertThat(state?.isNew).isTrue()
        assertThat(state?.name).isEmpty()
    }

    @Test
    fun `when updateMenuName called, then menu name is updated`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.updateMenuName("My Menu")

        val state = viewModel.menuDetailState.first()
        assertThat(state?.name).isEqualTo("My Menu")
    }

    @Test
    fun `when updateMenuDescription called, then description is updated`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.updateMenuDescription("Menu description")

        val state = viewModel.menuDetailState.first()
        assertThat(state?.description).isEqualTo("Menu description")
    }

    @Test
    fun `when updateMenuAutoAdd called, then autoAdd is updated`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.updateMenuAutoAdd(true)

        val state = viewModel.menuDetailState.first()
        assertThat(state?.autoAdd).isTrue()
    }

    @Test
    fun `when toggleMenuLocation called, then location is added`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.toggleMenuLocation("primary")

        val state = viewModel.menuDetailState.first()
        assertThat(state?.selectedLocations).contains("primary")
    }

    @Test
    fun `when toggleMenuLocation called twice for same location, then location is removed`() = test {
        viewModel.navigateToCreateMenu()

        viewModel.toggleMenuLocation("primary")
        viewModel.toggleMenuLocation("primary")

        val state = viewModel.menuDetailState.first()
        assertThat(state?.selectedLocations).doesNotContain("primary")
    }

    @Test
    fun `when saveMenu called with empty name, then error event is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        viewModel.navigateToCreateMenu()

        viewModel.saveMenu()

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(NavMenusUiEvent.ShowError::class.java)
        assertThat((event as NavMenusUiEvent.ShowError).message).isNotBlank()
    }

    @Test
    fun `when consumeUiEvent called, then event is cleared`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        viewModel.navigateToCreateMenu()
        viewModel.saveMenu() // triggers error event

        viewModel.consumeUiEvent()

        val event = viewModel.uiEvent.first()
        assertThat(event).isNull()
    }

    @Test
    fun `when navigateToCreateMenuItem called, then state is initialized with custom link type`() = test {
        viewModel.navigateToCreateMenuItem()

        val state = viewModel.menuItemDetailState.first()
        assertThat(state).isNotNull
        assertThat(state?.selectedTypeOption).isEqualTo(MenuItemTypeOption.CUSTOM_LINK)
        assertThat(state?.type).isEqualTo(NavMenuItemModel.TYPE_CUSTOM)
        assertThat(state?.isNew).isTrue()
    }

    @Test
    fun `when updateMenuItemType called with PAGE, then type and objectType are updated`() = test {
        viewModel.navigateToCreateMenuItem()

        viewModel.updateMenuItemType(MenuItemTypeOption.PAGE)

        val state = viewModel.menuItemDetailState.first()
        assertThat(state?.selectedTypeOption).isEqualTo(MenuItemTypeOption.PAGE)
        assertThat(state?.type).isEqualTo(NavMenuItemModel.TYPE_POST_TYPE)
        assertThat(state?.objectType).isEqualTo(NavMenuItemModel.OBJECT_TYPE_PAGE)
        assertThat(state?.url).isEmpty()
    }

    @Test
    fun `when updateMenuItemType called, then previous selection is cleared`() = test {
        viewModel.navigateToCreateMenuItem()
        viewModel.updateSelectedLinkableItem(LinkableItemOption(id = 123L, title = "Test Page"))

        viewModel.updateMenuItemType(MenuItemTypeOption.POST)

        val state = viewModel.menuItemDetailState.first()
        assertThat(state?.selectedLinkableItem).isNull()
        assertThat(state?.objectId).isEqualTo(0L)
    }

    @Test
    fun `when updateSelectedLinkableItem called, then objectId is set`() = test {
        viewModel.navigateToCreateMenuItem()
        val testItem = LinkableItemOption(id = 456L, title = "Test Item")

        viewModel.updateSelectedLinkableItem(testItem)

        val state = viewModel.menuItemDetailState.first()
        assertThat(state?.selectedLinkableItem).isEqualTo(testItem)
        assertThat(state?.objectId).isEqualTo(456L)
    }

    @Test
    fun `when updateSelectedLinkableItem called, then title is set from item`() = test {
        viewModel.navigateToCreateMenuItem()
        val testItem = LinkableItemOption(id = 789L, title = "Auto Title")

        viewModel.updateSelectedLinkableItem(testItem)

        val state = viewModel.menuItemDetailState.first()
        assertThat(state?.title).isEqualTo("Auto Title")
    }

    @Test
    fun `when updateSelectedLinkableItem called with existing title, then title is replaced`() = test {
        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemTitle("Existing Title")
        val testItem = LinkableItemOption(id = 789L, title = "New Title")

        viewModel.updateSelectedLinkableItem(testItem)

        val state = viewModel.menuItemDetailState.first()
        assertThat(state?.title).isEqualTo("New Title")
    }

    @Test
    fun `when updateMenuItemType called with non-custom type, then loading state is set`() = test {
        viewModel.navigateToCreateMenuItem()

        viewModel.updateMenuItemType(MenuItemTypeOption.POST)

        val state = viewModel.menuItemDetailState.first()
        assertThat(state?.linkableItemsState?.isLoading).isTrue()
    }

    @Test
    fun `when saveMenuItem called with non-custom type and no objectId, then error is shown`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemType(MenuItemTypeOption.PAGE)
        viewModel.updateMenuItemTitle("Test Item")

        viewModel.saveMenuItem()

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(NavMenusUiEvent.ShowError::class.java)
        assertThat((event as NavMenusUiEvent.ShowError).message).isNotBlank()
    }

    @Test
    fun `when saveMenuItem called with custom type and empty URL, then error is shown`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemTitle("Test Item")

        viewModel.saveMenuItem()

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(NavMenusUiEvent.ShowError::class.java)
        assertThat((event as NavMenusUiEvent.ShowError).message).isNotBlank()
    }
}
