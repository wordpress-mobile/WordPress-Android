package org.wordpress.android.ui.navmenus

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.navmenus.data.NavMenuRestClient
import org.wordpress.android.ui.navmenus.models.NavMenuItemModel
import org.wordpress.android.ui.navmenus.models.NavMenuLocationModel
import org.wordpress.android.ui.navmenus.models.NavMenuModel
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
class NavMenusViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var navMenuRestClient: NavMenuRestClient

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

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
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            mainDispatcher = testDispatcher(),
            ioDispatcher = testDispatcher()
        )
    }

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

    // region Helpers

    private fun createTestMenu(
        id: Long,
        name: String = "Menu $id",
        description: String = "",
        locations: String = "[]"
    ) = NavMenuModel(
        localSiteId = testSite.id,
        remoteMenuId = id,
        name = name,
        description = description,
        locations = locations
    )

    private fun createTestMenuItem(
        id: Long,
        menuId: Long = 1L,
        parentId: Long = 0L,
        order: Int = 0,
        title: String = "Item $id",
        type: String = NavMenuItemModel.TYPE_CUSTOM,
        url: String = "https://example.com/$id"
    ) = NavMenuItemModel(
        localSiteId = testSite.id,
        remoteItemId = id,
        menuId = menuId,
        title = title,
        url = url,
        type = type,
        parentId = parentId,
        menuOrder = order,
        classes = "[]"
    )

    private suspend fun setupMenusInCache(
        menus: List<NavMenuModel>,
        canLoadMore: Boolean = false,
        locations: List<NavMenuLocationModel> = emptyList()
    ) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(navMenuRestClient.fetchMenus(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuListResult.Success(menus, canLoadMore)
        )
        whenever(navMenuRestClient.fetchMenuLocations(any())).thenReturn(
            NavMenuRestClient.NavMenuLocationsResult.Success(locations)
        )
        viewModel.loadMenus()
    }

    private suspend fun setupMenuItemsInCache(
        menuId: Long,
        items: List<NavMenuItemModel>,
        canLoadMore: Boolean = false
    ) {
        setupMenusInCache(listOf(createTestMenu(menuId)))
        whenever(navMenuRestClient.fetchMenuItems(any(), any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemListResult.Success(items, canLoadMore)
        )
        viewModel.navigateToMenuItems(menuId)
    }

    // endregion

    // region Menu Loading

    @Test
    fun `when loadMenus called without site, then error state is set`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.loadMenus()

        val state = viewModel.menuListState.first()
        assertThat(state.error).isNotNull()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `when loadMenus succeeds, then menus and locations are set`() = test {
        val menus = listOf(createTestMenu(1L, "Main Menu"))
        val locations = listOf(
            NavMenuLocationModel(
                name = "primary",
                description = "Primary Menu",
                menuId = 1L
            )
        )
        setupMenusInCache(menus, locations = locations)

        val state = viewModel.menuListState.first()
        assertThat(state.menus).hasSize(1)
        assertThat(state.menus[0].name).isEqualTo("Main Menu")
        assertThat(state.locations).hasSize(1)
        assertThat(state.locations[0].name).isEqualTo("primary")
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `when loadMenus returns error, then error state is set`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(navMenuRestClient.fetchMenus(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuListResult.Error("Network error")
        )
        whenever(navMenuRestClient.fetchMenuLocations(any())).thenReturn(
            NavMenuRestClient.NavMenuLocationsResult.Success(emptyList())
        )

        viewModel.loadMenus()

        val state = viewModel.menuListState.first()
        assertThat(state.error).isEqualTo("Network error")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `when refreshMenus called, then menus are reloaded`() = test {
        setupMenusInCache(listOf(createTestMenu(1L, "Old Menu")))

        whenever(navMenuRestClient.fetchMenus(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuListResult.Success(
                listOf(createTestMenu(1L, "Updated Menu")), false
            )
        )

        viewModel.refreshMenus()

        val state = viewModel.menuListState.first()
        assertThat(state.menus).hasSize(1)
        assertThat(state.menus[0].name).isEqualTo("Updated Menu")
    }

    @Test
    fun `when loadMoreMenus succeeds, then menus are appended`() = test {
        setupMenusInCache(
            listOf(createTestMenu(1L, "Menu 1")),
            canLoadMore = true
        )

        whenever(navMenuRestClient.fetchMenus(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuListResult.Success(
                listOf(createTestMenu(2L, "Menu 2")), false
            )
        )

        viewModel.loadMoreMenus()

        val state = viewModel.menuListState.first()
        assertThat(state.menus).hasSize(2)
        assertThat(state.menus[0].name).isEqualTo("Menu 1")
        assertThat(state.menus[1].name).isEqualTo("Menu 2")
        assertThat(state.canLoadMore).isFalse()
    }

    @Test
    fun `when loadMoreMenus called with no more pages, then request is skipped`() = test {
        setupMenusInCache(listOf(createTestMenu(1L)), canLoadMore = false)

        val stateBefore = viewModel.menuListState.first()
        viewModel.loadMoreMenus()
        val stateAfter = viewModel.menuListState.first()

        assertThat(stateAfter.menus).isEqualTo(stateBefore.menus)
        assertThat(stateAfter.isLoadingMore).isFalse()
    }

    // endregion

    // region Menu Item Loading

    @Test
    fun `when navigateToMenuItems called, then items are loaded`() = test {
        val items = listOf(
            createTestMenuItem(10L, menuId = 1L, order = 1, title = "Home"),
            createTestMenuItem(20L, menuId = 1L, order = 2, title = "About")
        )
        setupMenuItemsInCache(1L, items)

        val state = viewModel.menuItemListState.first()
        assertThat(state.items).hasSize(2)
        assertThat(state.items[0].title).isEqualTo("Home")
        assertThat(state.items[1].title).isEqualTo("About")
        assertThat(state.isLoading).isFalse()
        assertThat(state.menuName).isEqualTo("Menu 1")
    }

    @Test
    fun `when loadMenuItems returns error, then error state is set`() = test {
        setupMenusInCache(listOf(createTestMenu(1L)))
        whenever(navMenuRestClient.fetchMenuItems(any(), any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemListResult.Error("Failed to load items")
        )

        viewModel.navigateToMenuItems(1L)

        val state = viewModel.menuItemListState.first()
        assertThat(state.error).isEqualTo("Failed to load items")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `when loadMoreMenuItems succeeds, then items are appended`() = test {
        val initialItems = listOf(
            createTestMenuItem(10L, menuId = 1L, order = 1),
            createTestMenuItem(20L, menuId = 1L, order = 2)
        )
        setupMenuItemsInCache(1L, initialItems, canLoadMore = true)

        whenever(navMenuRestClient.fetchMenuItems(any(), any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemListResult.Success(
                listOf(createTestMenuItem(30L, menuId = 1L, order = 3)),
                canLoadMore = false
            )
        )

        viewModel.loadMoreMenuItems()

        val state = viewModel.menuItemListState.first()
        assertThat(state.items).hasSize(3)
        assertThat(state.canLoadMore).isFalse()
    }

    // endregion

    // region Menu Detail

    @Test
    fun `when navigateToCreateMenu called, then detail state is initialized for new menu`() = test {
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
        viewModel.saveMenu()

        viewModel.consumeUiEvent()

        val event = viewModel.uiEvent.first()
        assertThat(event).isNull()
    }

    // endregion

    // region Menu Item Detail

    @Test
    fun `when navigateToCreateMenuItem called, then state is initialized with custom link type`() = test {
        viewModel.navigateToCreateMenuItem()

        val state = viewModel.menuItemDetailState.first()
        assertThat(state).isNotNull
        assertThat(state?.selectedTypeOption).isEqualTo(MenuItemTypeOption.CUSTOM_LINK)
        assertThat(state?.type).isEqualTo(NavMenuItemModel.TYPE_CUSTOM)
        assertThat(state?.isNew).isTrue()
        verify(analyticsTrackerWrapper).track(Stat.MENUS_OPENED_ITEM_EDITOR)
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
        viewModel.updateSelectedLinkableItem(
            LinkableItemOption(id = 123L, title = "Test Page")
        )

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
        assertThat(state?.title).isEqualTo("Test Item")
    }

    @Test
    fun `when updateMenuItemType called with non-custom type, then linkable items are fetched`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        viewModel.navigateToCreateMenuItem()

        viewModel.updateMenuItemType(MenuItemTypeOption.POST)

        verify(navMenuRestClient).fetchPosts(any(), any())
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

    // endregion

    // region Navigation Cache Miss / Hit

    @Test
    fun `when navigateToEditMenu with invalid id, then error event is emitted`() = test {
        setupMenusInCache(listOf(createTestMenu(1L)))

        viewModel.navigateToEditMenu(999L)

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(NavMenusUiEvent.ShowError::class.java)
    }

    @Test
    fun `when navigateToMenuItems with invalid id, then error event is emitted`() = test {
        setupMenusInCache(listOf(createTestMenu(1L)))

        viewModel.navigateToMenuItems(999L)

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(NavMenusUiEvent.ShowError::class.java)
    }

    @Test
    fun `when navigateToEditMenuItem with invalid id, then error event is emitted`() = test {
        setupMenuItemsInCache(
            1L,
            listOf(createTestMenuItem(10L, menuId = 1L))
        )

        viewModel.navigateToEditMenuItem(999L)

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(NavMenusUiEvent.ShowError::class.java)
        verify(analyticsTrackerWrapper, never())
            .track(Stat.MENUS_OPENED_ITEM_EDITOR)
    }

    @Test
    fun `when navigateToEditMenu with valid id, then detail state is populated`() = test {
        setupMenusInCache(
            listOf(createTestMenu(1L, "Main Menu", description = "Site nav"))
        )

        viewModel.navigateToEditMenu(1L)

        val state = viewModel.menuDetailState.first()
        assertThat(state).isNotNull()
        assertThat(state?.menuId).isEqualTo(1L)
        assertThat(state?.name).isEqualTo("Main Menu")
        assertThat(state?.description).isEqualTo("Site nav")
        assertThat(state?.isNew).isFalse()
    }

    // endregion

    // region Reorder

    @Test
    fun `when reorder succeeds, then cache is updated for subsequent operations`() = test {
        val items = listOf(
            createTestMenuItem(10L, menuId = 1L, order = 1),
            createTestMenuItem(20L, menuId = 1L, order = 2)
        )
        setupMenuItemsInCache(1L, items)

        whenever(navMenuRestClient.updateMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemResult.Success(createTestMenuItem(10L))
        )

        viewModel.moveMenuItemUp(20L)

        // Verify cache was updated by navigating to edit
        viewModel.navigateToEditMenuItem(20L)
        val detail = viewModel.menuItemDetailState.first()
        assertThat(detail?.menuOrder).isEqualTo(1) // Was 2, swapped to 1
        verify(navMenuRestClient, times(2)).updateMenuItem(any(), any())
        verify(analyticsTrackerWrapper).track(Stat.MENUS_ORDERED_ITEMS)
    }

    @Test
    fun `when reorder fails, then UI state is rolled back`() = test {
        val items = listOf(
            createTestMenuItem(10L, menuId = 1L, order = 1, title = "First"),
            createTestMenuItem(20L, menuId = 1L, order = 2, title = "Second")
        )
        setupMenuItemsInCache(1L, items)

        whenever(navMenuRestClient.updateMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemResult.Error("Server error")
        )

        viewModel.moveMenuItemUp(20L)

        val state = viewModel.menuItemListState.first()
        assertThat(state.items[0].title).isEqualTo("First")
        assertThat(state.items[1].title).isEqualTo("Second")

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(NavMenusUiEvent.ShowError::class.java)
    }

    @Test
    fun `when moveMenuItemDown called, then items are swapped in UI`() = test {
        val items = listOf(
            createTestMenuItem(10L, menuId = 1L, order = 1, title = "First"),
            createTestMenuItem(20L, menuId = 1L, order = 2, title = "Second")
        )
        setupMenuItemsInCache(1L, items)

        whenever(navMenuRestClient.updateMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemResult.Success(createTestMenuItem(10L))
        )

        viewModel.moveMenuItemDown(10L)

        val state = viewModel.menuItemListState.first()
        assertThat(state.items[0].title).isEqualTo("Second")
        assertThat(state.items[1].title).isEqualTo("First")
    }

    // endregion

    // region Save / Delete

    @Test
    fun `when saveMenu succeeds, then MenuSaved event is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(navMenuRestClient.createMenu(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuResult.Success(createTestMenu(1L, "New Menu"))
        )
        whenever(navMenuRestClient.fetchMenus(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuListResult.Success(emptyList(), false)
        )
        whenever(navMenuRestClient.fetchMenuLocations(any())).thenReturn(
            NavMenuRestClient.NavMenuLocationsResult.Success(emptyList())
        )

        viewModel.navigateToCreateMenu()
        viewModel.updateMenuName("New Menu")
        viewModel.saveMenu()

        val event = viewModel.uiEvent.first()
        assertThat(event).isEqualTo(NavMenusUiEvent.MenuSaved)
        verify(navMenuRestClient).createMenu(any(), any())
        verify(analyticsTrackerWrapper).track(Stat.MENUS_CREATED_MENU)
    }

    @Test
    fun `when deleteMenu succeeds, then MenuDeleted event is emitted`() = test {
        setupMenusInCache(listOf(createTestMenu(1L, "Menu to Delete")))
        viewModel.navigateToEditMenu(1L)

        whenever(navMenuRestClient.deleteMenu(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuDeleteResult.Success
        )

        viewModel.deleteMenu()

        val event = viewModel.uiEvent.first()
        assertThat(event).isEqualTo(NavMenusUiEvent.MenuDeleted)
        verify(navMenuRestClient).deleteMenu(any(), any())
        verify(analyticsTrackerWrapper).track(Stat.MENUS_DELETED_MENU)
    }

    @Test
    fun `when saveMenuItem succeeds, then MenuItemSaved event is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(navMenuRestClient.createMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemResult.Success(createTestMenuItem(1L))
        )
        whenever(navMenuRestClient.fetchMenuItems(any(), any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemListResult.Success(emptyList(), false)
        )

        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemTitle("Test Link")
        viewModel.updateMenuItemUrl("https://example.com")
        viewModel.saveMenuItem()

        val event = viewModel.uiEvent.first()
        assertThat(event).isEqualTo(NavMenusUiEvent.MenuItemSaved)
        verify(navMenuRestClient).createMenuItem(any(), any())
        verify(analyticsTrackerWrapper).track(Stat.MENUS_CREATED_ITEM)
    }

    @Test
    fun `when deleteMenuItem succeeds, then MenuItemDeleted event is emitted`() = test {
        val items = listOf(createTestMenuItem(10L, menuId = 1L, order = 1))
        setupMenuItemsInCache(1L, items)
        viewModel.navigateToEditMenuItem(10L)

        whenever(navMenuRestClient.deleteMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemDeleteResult.Success
        )

        viewModel.deleteMenuItem()

        val event = viewModel.uiEvent.first()
        assertThat(event).isEqualTo(NavMenusUiEvent.MenuItemDeleted)
        verify(navMenuRestClient).deleteMenuItem(any(), any())
        verify(analyticsTrackerWrapper).track(Stat.MENUS_DELETED_ITEM)
    }

    @Test
    fun `when saveMenu called without site, then no API call is made`() = test {
        viewModel.navigateToCreateMenu()
        viewModel.updateMenuName("Test Menu")

        viewModel.saveMenu()

        verify(navMenuRestClient, never()).createMenu(any(), any())
        verify(navMenuRestClient, never()).updateMenu(any(), any())
    }

    @Test
    fun `when deleteMenu called without site, then no API call is made`() = test {
        setupMenusInCache(listOf(createTestMenu(1L, "Menu")))
        viewModel.navigateToEditMenu(1L)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.deleteMenu()

        verify(navMenuRestClient, never()).deleteMenu(any(), any())
    }

    // endregion

    // region Linkable Items

    @Test
    fun `when type set to PAGE, then pages are fetched and loaded`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        val pages = listOf(
            LinkableItemOption(id = 1L, title = "Home Page"),
            LinkableItemOption(id = 2L, title = "About Page")
        )
        whenever(navMenuRestClient.fetchPages(any(), any())).thenReturn(
            NavMenuRestClient.LinkableItemsResult.Success(pages, canLoadMore = false)
        )

        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemType(MenuItemTypeOption.PAGE)

        val state = viewModel.menuItemDetailState.first()
        val linkableItems = state?.linkableItemsState?.items
        assertThat(linkableItems).hasSize(2)
        assertThat(linkableItems?.get(0)?.title).isEqualTo("Home Page")
        assertThat(state?.linkableItemsState?.isLoading).isFalse()
        verify(navMenuRestClient).fetchPages(any(), any())
    }

    @Test
    fun `when loadMoreLinkableItems succeeds, then items are appended`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        val initialPages = listOf(LinkableItemOption(id = 1L, title = "Page 1"))
        whenever(navMenuRestClient.fetchPages(any(), any())).thenReturn(
            NavMenuRestClient.LinkableItemsResult.Success(
                initialPages,
                canLoadMore = true
            )
        )

        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemType(MenuItemTypeOption.PAGE)

        val morePages = listOf(LinkableItemOption(id = 2L, title = "Page 2"))
        whenever(navMenuRestClient.fetchPages(any(), any())).thenReturn(
            NavMenuRestClient.LinkableItemsResult.Success(
                morePages,
                canLoadMore = false
            )
        )

        viewModel.loadMoreLinkableItems()

        val state = viewModel.menuItemDetailState.first()
        assertThat(state?.linkableItemsState?.items).hasSize(2)
        assertThat(state?.linkableItemsState?.canLoadMore).isFalse()
    }

    // endregion

    // region Hierarchical Sorting

    @Test
    fun `when items have parent-child relationships, then children follow parents`() = test {
        val items = listOf(
            createTestMenuItem(
                10L, menuId = 1L, parentId = 0L, order = 1, title = "Parent"
            ),
            createTestMenuItem(
                20L, menuId = 1L, parentId = 10L, order = 1, title = "Child"
            ),
            createTestMenuItem(
                30L, menuId = 1L, parentId = 0L, order = 2, title = "Sibling"
            )
        )
        setupMenuItemsInCache(1L, items)

        val state = viewModel.menuItemListState.first()
        assertThat(state.items[0].title).isEqualTo("Parent")
        assertThat(state.items[1].title).isEqualTo("Child")
        assertThat(state.items[2].title).isEqualTo("Sibling")
    }

    @Test
    fun `when child items loaded, then they have correct indent levels`() = test {
        val items = listOf(
            createTestMenuItem(10L, menuId = 1L, parentId = 0L, order = 1),
            createTestMenuItem(20L, menuId = 1L, parentId = 10L, order = 1),
            createTestMenuItem(30L, menuId = 1L, parentId = 20L, order = 1)
        )
        setupMenuItemsInCache(1L, items)

        val state = viewModel.menuItemListState.first()
        assertThat(state.items[0].indentLevel).isEqualTo(0)
        assertThat(state.items[1].indentLevel).isEqualTo(1)
        assertThat(state.items[2].indentLevel).isEqualTo(2)
    }

    @Test
    fun `when siblings loaded, then they are sorted by menu order`() = test {
        val items = listOf(
            createTestMenuItem(
                10L, menuId = 1L, parentId = 0L, order = 3, title = "Third"
            ),
            createTestMenuItem(
                20L, menuId = 1L, parentId = 0L, order = 1, title = "First"
            ),
            createTestMenuItem(
                30L, menuId = 1L, parentId = 0L, order = 2, title = "Second"
            )
        )
        setupMenuItemsInCache(1L, items)

        val state = viewModel.menuItemListState.first()
        assertThat(state.items[0].title).isEqualTo("First")
        assertThat(state.items[1].title).isEqualTo("Second")
        assertThat(state.items[2].title).isEqualTo("Third")
    }

    // endregion

    // region URL Normalization

    @Test
    fun `when saveMenuItem with bare domain, then https is prepended`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(navMenuRestClient.createMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemResult.Success(createTestMenuItem(1L))
        )
        whenever(navMenuRestClient.fetchMenuItems(any(), any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemListResult.Success(emptyList(), false)
        )

        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemTitle("Test Link")
        viewModel.updateMenuItemUrl("example.com")
        viewModel.saveMenuItem()

        verify(navMenuRestClient).createMenuItem(any(), org.mockito.kotlin.check {
            assertThat(it.url).isEqualTo("https://example.com")
        })
    }

    @Test
    fun `when saveMenuItem with anchor URL, then URL is preserved`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(navMenuRestClient.createMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemResult.Success(createTestMenuItem(1L))
        )
        whenever(navMenuRestClient.fetchMenuItems(any(), any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemListResult.Success(emptyList(), false)
        )

        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemTitle("Section Link")
        viewModel.updateMenuItemUrl("#section")
        viewModel.saveMenuItem()

        verify(navMenuRestClient).createMenuItem(any(), org.mockito.kotlin.check {
            assertThat(it.url).isEqualTo("#section")
        })
    }

    @Test
    fun `when saveMenuItem with relative path, then URL is preserved`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(navMenuRestClient.createMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemResult.Success(createTestMenuItem(1L))
        )
        whenever(navMenuRestClient.fetchMenuItems(any(), any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemListResult.Success(emptyList(), false)
        )

        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemTitle("About Page")
        viewModel.updateMenuItemUrl("/about")
        viewModel.saveMenuItem()

        verify(navMenuRestClient).createMenuItem(any(), org.mockito.kotlin.check {
            assertThat(it.url).isEqualTo("/about")
        })
    }

    @Test
    fun `when saveMenuItem with mailto URL, then URL is preserved`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(navMenuRestClient.createMenuItem(any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemResult.Success(createTestMenuItem(1L))
        )
        whenever(navMenuRestClient.fetchMenuItems(any(), any(), any())).thenReturn(
            NavMenuRestClient.NavMenuItemListResult.Success(emptyList(), false)
        )

        viewModel.navigateToCreateMenuItem()
        viewModel.updateMenuItemTitle("Email Us")
        viewModel.updateMenuItemUrl("mailto:hello@example.com")
        viewModel.saveMenuItem()

        verify(navMenuRestClient).createMenuItem(any(), org.mockito.kotlin.check {
            assertThat(it.url).isEqualTo("mailto:hello@example.com")
        })
    }

    // endregion

    // region Input Sanitization

    @Test
    fun `when menu name exceeds max length, then it is truncated`() = test {
        viewModel.navigateToCreateMenu()
        val longName = "A".repeat(250)

        viewModel.updateMenuName(longName)

        val state = viewModel.menuDetailState.first()
        assertThat(state?.name?.length).isEqualTo(200)
    }

    // endregion
}
