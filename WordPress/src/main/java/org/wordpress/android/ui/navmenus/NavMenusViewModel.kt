package org.wordpress.android.ui.navmenus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.navmenus.models.NavMenuItemModel
import org.wordpress.android.ui.navmenus.models.NavMenuModel
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.navmenus.data.NavMenuRestClient
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.R
import org.wordpress.android.util.AppLog
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
@Suppress("LargeClass")
class NavMenusViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val navMenuRestClient: NavMenuRestClient,
    private val resourceProvider: ResourceProvider,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var navController: NavHostController? = null

    // Menu list state
    private val _menuListState = MutableStateFlow(MenuListUiState())
    val menuListState: StateFlow<MenuListUiState> = _menuListState.asStateFlow()

    // Menu detail state
    private val _menuDetailState = MutableStateFlow<MenuDetailUiState?>(null)
    val menuDetailState: StateFlow<MenuDetailUiState?> = _menuDetailState.asStateFlow()

    // Menu items list state
    private val _menuItemListState = MutableStateFlow(MenuItemListUiState())
    val menuItemListState: StateFlow<MenuItemListUiState> = _menuItemListState.asStateFlow()

    // Menu item detail state
    private val _menuItemDetailState = MutableStateFlow<MenuItemDetailUiState?>(null)
    val menuItemDetailState: StateFlow<MenuItemDetailUiState?> = _menuItemDetailState.asStateFlow()

    // UI events
    private val _uiEvent = MutableStateFlow<NavMenusUiEvent?>(null)
    val uiEvent: StateFlow<NavMenusUiEvent?> = _uiEvent.asStateFlow()

    // Cache
    private var currentMenus = listOf<NavMenuModel>()
    private var currentMenuItems = listOf<NavMenuItemModel>()

    // Mutexes for pagination to prevent race conditions
    private val menusPaginationMutex = Mutex()
    private val menuItemsPaginationMutex = Mutex()
    private val linkableItemsPaginationMutex = Mutex()

    // Job for loading linkable items (cancelled when type changes)
    private var linkableItemsLoadingJob: Job? = null

    fun setNavController(controller: NavHostController) {
        navController = controller
    }

    fun loadMenus() {
        loadMenusInternal(isRefresh = false)
    }

    fun refreshMenus() {
        loadMenusInternal(isRefresh = true)
    }

    private fun loadMenusInternal(isRefresh: Boolean) {
        viewModelScope.launch {
            _menuListState.value = _menuListState.value.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                error = null
            )

            val site = selectedSiteRepository.getSelectedSite()
            if (site == null) {
                _menuListState.value = _menuListState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = resourceProvider.getString(R.string.menu_error_no_site_selected)
                )
                return@launch
            }

            @Suppress("TooGenericExceptionCaught")
            try {
                val newState = withContext(ioDispatcher) { fetchMenuData(site, offset = 0) }
                _menuListState.value = newState
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _menuListState.value = MenuListUiState(
                    isLoading = false,
                    isRefreshing = false,
                    canLoadMore = true,
                    error = e.message ?: resourceProvider.getString(R.string.error_generic)
                )
            }
        }
    }

    fun loadMoreMenus() {
        viewModelScope.launch {
            menusPaginationMutex.withLock {
                val currentState = _menuListState.value
                if (currentState.isLoading || currentState.isLoadingMore || !currentState.canLoadMore) return@launch

                _menuListState.value = currentState.copy(isLoadingMore = true)

                val site = selectedSiteRepository.getSelectedSite() ?: run {
                    _menuListState.value = currentState.copy(isLoadingMore = false)
                    return@launch
                }
                val offset = currentState.menus.size

                @Suppress("TooGenericExceptionCaught")
                try {
                    val result = withContext(ioDispatcher) {
                        navMenuRestClient.fetchMenus(site, offset)
                    }

                    when (result) {
                        is NavMenuRestClient.NavMenuListResult.Success -> {
                            currentMenus = currentMenus + result.menus
                            val allMenus = currentMenus.map { it.toUiModel() }
                            _menuListState.value = _menuListState.value.copy(
                                isLoadingMore = false,
                                canLoadMore = result.canLoadMore,
                                menus = allMenus
                            )
                        }
                        is NavMenuRestClient.NavMenuListResult.Error -> {
                            _menuListState.value = _menuListState.value.copy(
                                isLoadingMore = false,
                                canLoadMore = true
                            )
                            _uiEvent.value = NavMenusUiEvent.ShowError(result.message)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.e(AppLog.T.API, "Failed to load more menus", e)
                    _menuListState.value = _menuListState.value.copy(
                        isLoadingMore = false,
                        canLoadMore = true
                    )
                }
            }
        }
    }

    private suspend fun fetchMenuData(site: SiteModel, offset: Int): MenuListUiState {
        val menusResult = navMenuRestClient.fetchMenus(site, offset)
        val locationsResult = navMenuRestClient.fetchMenuLocations(site)

        return when (menusResult) {
            is NavMenuRestClient.NavMenuListResult.Success -> {
                currentMenus = menusResult.menus
                buildSuccessState(menusResult.menus, locationsResult, menusResult.canLoadMore)
            }
            is NavMenuRestClient.NavMenuListResult.Error -> {
                val errorMessage = menusResult.message.takeIf { it.isNotBlank() } ?: "Failed to load menus"
                MenuListUiState(isLoading = false, canLoadMore = true, error = errorMessage)
            }
        }
    }

    private fun buildSuccessState(
        menus: List<NavMenuModel>,
        locationsResult: NavMenuRestClient.NavMenuLocationsResult,
        canLoadMore: Boolean
    ): MenuListUiState {
        val menuUiModels = menus.map { menu -> menu.toUiModel() }

        val locations = when (locationsResult) {
            is NavMenuRestClient.NavMenuLocationsResult.Success -> {
                locationsResult.locations.map { it.toUiModel() }
            }
            is NavMenuRestClient.NavMenuLocationsResult.Error -> emptyList()
        }

        return MenuListUiState(
            isLoading = false,
            canLoadMore = canLoadMore,
            menus = menuUiModels,
            locations = locations
        )
    }

    fun navigateToCreateMenu() {
        _menuDetailState.value = MenuDetailUiState(
            menuId = 0L,
            name = "",
            description = "",
            autoAdd = false,
            selectedLocations = emptyList(),
            availableLocations = _menuListState.value.locations,
            isNew = true
        )
        navController?.navigate(NavMenuScreen.MenuDetail.name)
    }

    fun navigateToEditMenu(menuId: Long) {
        val menu = currentMenus.find { it.remoteMenuId == menuId } ?: return

        _menuDetailState.value = MenuDetailUiState(
            menuId = menu.remoteMenuId,
            name = menu.name,
            description = menu.description,
            autoAdd = menu.autoAdd,
            selectedLocations = menu.locations.parseJsonStringArray(),
            availableLocations = _menuListState.value.locations,
            isNew = false
        )
        navController?.navigate(NavMenuScreen.MenuDetail.name)
    }

    fun navigateToMenuItems(menuId: Long) {
        val menu = currentMenus.find { it.remoteMenuId == menuId } ?: return
        _menuItemListState.value = MenuItemListUiState(
            isLoading = true,
            menuId = menuId,
            menuName = menu.name
        )
        navController?.navigate(NavMenuScreen.MenuItemList.name)
        loadMenuItems(menuId)
    }

    private fun loadMenuItems(menuId: Long) {
        viewModelScope.launch {
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch

            withContext(ioDispatcher) {
                val result = navMenuRestClient.fetchMenuItems(site, menuId, offset = 0)

                withContext(mainDispatcher) {
                    when (result) {
                        is NavMenuRestClient.NavMenuItemListResult.Success -> {
                            currentMenuItems = result.items
                            val sortedItems = sortItemsHierarchically(result.items)
                            _menuItemListState.value = _menuItemListState.value.copy(
                                isLoading = false,
                                canLoadMore = result.canLoadMore,
                                items = sortedItems
                            )
                        }
                        is NavMenuRestClient.NavMenuItemListResult.Error -> {
                            _menuItemListState.value = _menuItemListState.value.copy(
                                isLoading = false,
                                canLoadMore = true,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMoreMenuItems() {
        viewModelScope.launch {
            menuItemsPaginationMutex.withLock {
                val currentState = _menuItemListState.value
                if (currentState.isLoading || currentState.isLoadingMore || !currentState.canLoadMore) return@launch

                _menuItemListState.value = currentState.copy(isLoadingMore = true)

                val site = selectedSiteRepository.getSelectedSite() ?: run {
                    _menuItemListState.value = currentState.copy(isLoadingMore = false)
                    return@launch
                }
                val offset = currentState.items.size

                @Suppress("TooGenericExceptionCaught")
                try {
                    val result = withContext(ioDispatcher) {
                        navMenuRestClient.fetchMenuItems(site, currentState.menuId, offset)
                    }

                    when (result) {
                        is NavMenuRestClient.NavMenuItemListResult.Success -> {
                            currentMenuItems = currentMenuItems + result.items
                            val sortedItems = sortItemsHierarchically(currentMenuItems)
                            _menuItemListState.value = _menuItemListState.value.copy(
                                isLoadingMore = false,
                                canLoadMore = result.canLoadMore,
                                items = sortedItems
                            )
                        }
                        is NavMenuRestClient.NavMenuItemListResult.Error -> {
                            _menuItemListState.value = _menuItemListState.value.copy(
                                isLoadingMore = false,
                                canLoadMore = true
                            )
                            _uiEvent.value = NavMenusUiEvent.ShowError(result.message)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.e(AppLog.T.API, "Failed to load more menu items", e)
                    _menuItemListState.value = _menuItemListState.value.copy(
                        isLoadingMore = false,
                        canLoadMore = true
                    )
                }
            }
        }
    }

    private fun sortItemsHierarchically(items: List<NavMenuItemModel>): List<MenuItemUiModel> {
        val result = mutableListOf<MenuItemUiModel>()
        val itemsById = items.associateBy { it.remoteItemId }
        val childrenByParentId = items.groupBy { it.parentId }
        val visited = mutableSetOf<Long>()

        fun addItemWithChildren(item: NavMenuItemModel, indentLevel: Int) {
            if (item.remoteItemId in visited) return
            visited.add(item.remoteItemId)
            result.add(item.toUiModel(indentLevel))

            // Get pre-computed children sorted by menu order
            childrenByParentId[item.remoteItemId]
                ?.sortedBy { it.menuOrder }
                ?.forEach { child ->
                    addItemWithChildren(child, indentLevel + 1)
                }
        }

        // Start with root items (parent = 0 or parent not in items)
        items.filter { it.parentId == 0L || itemsById[it.parentId] == null }
            .sortedBy { it.menuOrder }
            .forEach { rootItem ->
                addItemWithChildren(rootItem, 0)
            }

        return result
    }

    fun navigateToCreateMenuItem() {
        val menuId = _menuItemListState.value.menuId
        val availableParents = buildAvailableParents(0L)

        _menuItemDetailState.value = MenuItemDetailUiState(
            itemId = 0L,
            menuId = menuId,
            title = "",
            url = "",
            type = NavMenuItemModel.TYPE_CUSTOM,
            availableParents = availableParents,
            selectedTypeOption = MenuItemTypeOption.CUSTOM_LINK,
            linkableItemsState = LinkableItemsState(),
            selectedLinkableItem = null,
            menuOrder = currentMenuItems.maxOfOrNull { it.menuOrder }?.plus(1) ?: 1,
            isNew = true
        )
        navController?.navigate(NavMenuScreen.MenuItemDetail.name)
    }

    fun navigateToEditMenuItem(itemId: Long) {
        val item = currentMenuItems.find { it.remoteItemId == itemId } ?: return
        val availableParents = buildAvailableParents(itemId)

        _menuItemDetailState.value = MenuItemDetailUiState(
            itemId = item.remoteItemId,
            menuId = item.menuId,
            title = item.title,
            url = item.url,
            type = item.type,
            objectType = item.objectType,
            objectId = item.objectId,
            parentId = item.parentId,
            menuOrder = item.menuOrder,
            target = item.target,
            cssClasses = item.classes.trim('[', ']').replace("\"", ""),
            description = item.description,
            attrTitle = item.attrTitle,
            availableParents = availableParents,
            isNew = false
        )
        navController?.navigate(NavMenuScreen.MenuItemDetail.name)
    }

    private fun buildAvailableParents(excludeItemId: Long): List<ParentItemOption> {
        val result = mutableListOf<ParentItemOption>()
        val descendants = getDescendants(excludeItemId)

        fun addItem(item: NavMenuItemModel, indentLevel: Int) {
            if (item.remoteItemId != excludeItemId && item.remoteItemId !in descendants) {
                result.add(ParentItemOption(item.remoteItemId, item.title, indentLevel))
                currentMenuItems.filter { it.parentId == item.remoteItemId }
                    .sortedBy { it.menuOrder }
                    .forEach { child -> addItem(child, indentLevel + 1) }
            }
        }

        currentMenuItems.filter { it.parentId == 0L }
            .sortedBy { it.menuOrder }
            .forEach { addItem(it, 0) }

        return result
    }

    private fun getDescendants(itemId: Long): Set<Long> {
        val descendants = mutableSetOf<Long>()
        fun addDescendants(parentId: Long) {
            currentMenuItems.filter { it.parentId == parentId }.forEach { child ->
                descendants.add(child.remoteItemId)
                addDescendants(child.remoteItemId)
            }
        }
        addDescendants(itemId)
        return descendants
    }

    fun navigateBack() {
        navController?.navigateUp()
    }

    // Menu detail update methods
    fun updateMenuName(name: String) {
        val sanitized = sanitizeInput(name, MAX_MENU_NAME_LENGTH)
        _menuDetailState.value = _menuDetailState.value?.copy(name = sanitized)
    }

    fun updateMenuDescription(description: String) {
        val sanitized = sanitizeInput(description, MAX_MENU_DESCRIPTION_LENGTH)
        _menuDetailState.value = _menuDetailState.value?.copy(description = sanitized)
    }

    fun updateMenuAutoAdd(autoAdd: Boolean) {
        _menuDetailState.value = _menuDetailState.value?.copy(autoAdd = autoAdd)
    }

    fun toggleMenuLocation(locationName: String) {
        val currentState = _menuDetailState.value ?: return
        val currentLocations = currentState.selectedLocations.toMutableList()
        if (locationName in currentLocations) {
            currentLocations.remove(locationName)
        } else {
            currentLocations.add(locationName)
        }
        _menuDetailState.value = currentState.copy(selectedLocations = currentLocations)
    }

    fun saveMenu() {
        viewModelScope.launch {
            val state = _menuDetailState.value ?: return@launch
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch

            if (state.name.isBlank()) {
                _uiEvent.value = NavMenusUiEvent.ShowError(
                    resourceProvider.getString(R.string.menu_name_required)
                )
                return@launch
            }

            _menuDetailState.value = state.copy(isSaving = true)

            val menu = NavMenuModel(
                localSiteId = site.id,
                remoteMenuId = state.menuId,
                name = state.name,
                description = state.description,
                locations = state.selectedLocations.toJsonStringArray(),
                autoAdd = state.autoAdd
            )

            withContext(ioDispatcher) {
                val result = if (state.isNew) {
                    navMenuRestClient.createMenu(site, menu)
                } else {
                    navMenuRestClient.updateMenu(site, menu)
                }

                withContext(mainDispatcher) {
                    _menuDetailState.value = state.copy(isSaving = false)
                    when (result) {
                        is NavMenuRestClient.NavMenuResult.Success -> {
                            _uiEvent.value = NavMenusUiEvent.MenuSaved
                            navigateBack()
                            loadMenus()
                        }
                        is NavMenuRestClient.NavMenuResult.Error -> {
                            _uiEvent.value = NavMenusUiEvent.ShowError(result.message)
                        }
                    }
                }
            }
        }
    }

    fun deleteMenu() {
        viewModelScope.launch {
            val state = _menuDetailState.value ?: return@launch
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch

            if (state.menuId <= 0) return@launch

            _menuDetailState.value = state.copy(isDeleting = true)

            withContext(ioDispatcher) {
                val result = navMenuRestClient.deleteMenu(site, state.menuId)

                withContext(mainDispatcher) {
                    _menuDetailState.value = state.copy(isDeleting = false)
                    when (result) {
                        is NavMenuRestClient.NavMenuDeleteResult.Success -> {
                            _uiEvent.value = NavMenusUiEvent.MenuDeleted
                            navigateBack()
                            loadMenus()
                        }
                        is NavMenuRestClient.NavMenuDeleteResult.Error -> {
                            _uiEvent.value = NavMenusUiEvent.ShowError(result.message)
                        }
                    }
                }
            }
        }
    }

    // Menu item detail update methods
    fun updateMenuItemTitle(title: String) {
        val sanitized = sanitizeInput(title, MAX_MENU_ITEM_TITLE_LENGTH)
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(title = sanitized)
    }

    fun updateMenuItemUrl(url: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(url = url.trim())
    }

    fun updateMenuItemParent(parentId: Long) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(parentId = parentId)
    }

    fun updateMenuItemDescription(description: String) {
        val sanitized = sanitizeInput(description, MAX_MENU_ITEM_DESCRIPTION_LENGTH)
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(description = sanitized)
    }

    fun updateMenuItemType(typeOption: MenuItemTypeOption) {
        // Cancel any ongoing linkable items loading
        linkableItemsLoadingJob?.cancel()
        linkableItemsLoadingJob = null

        val currentState = _menuItemDetailState.value ?: return
        _menuItemDetailState.value = currentState.copy(
            selectedTypeOption = typeOption,
            type = typeOption.type,
            objectType = typeOption.objectType,
            url = if (typeOption == MenuItemTypeOption.CUSTOM_LINK) currentState.url else "",
            objectId = 0L,
            selectedLinkableItem = null,
            linkableItemsState = LinkableItemsState(isLoading = typeOption != MenuItemTypeOption.CUSTOM_LINK)
        )

        // Load linkable items for non-custom types
        if (typeOption != MenuItemTypeOption.CUSTOM_LINK) {
            loadLinkableItems(typeOption)
        }
    }

    private fun loadLinkableItems(typeOption: MenuItemTypeOption) {
        linkableItemsLoadingJob = viewModelScope.launch {
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch

            val result = withContext(ioDispatcher) {
                fetchLinkableItems(site, typeOption, offset = 0)
            }

            val currentState = _menuItemDetailState.value ?: return@launch
            // Only update if the type hasn't changed while loading
            if (currentState.selectedTypeOption == typeOption) {
                _menuItemDetailState.value = when (result) {
                    is NavMenuRestClient.LinkableItemsResult.Success -> {
                        currentState.copy(
                            linkableItemsState = LinkableItemsState(
                                isLoading = false,
                                canLoadMore = result.canLoadMore,
                                items = result.items
                            )
                        )
                    }
                    is NavMenuRestClient.LinkableItemsResult.Error -> {
                        currentState.copy(
                            linkableItemsState = LinkableItemsState(
                                isLoading = false,
                                canLoadMore = true,
                                error = result.message
                            )
                        )
                    }
                }
            }
        }
    }

    @Suppress("LongMethod")
    fun loadMoreLinkableItems() {
        viewModelScope.launch {
            linkableItemsPaginationMutex.withLock {
                val currentState = _menuItemDetailState.value ?: return@launch
                val linkableState = currentState.linkableItemsState
                if (linkableState.isLoading || linkableState.isLoadingMore || !linkableState.canLoadMore) {
                    return@launch
                }

                _menuItemDetailState.value = currentState.copy(
                    linkableItemsState = linkableState.copy(isLoadingMore = true)
                )

                val site = selectedSiteRepository.getSelectedSite() ?: run {
                    _menuItemDetailState.value = currentState.copy(
                        linkableItemsState = linkableState.copy(isLoadingMore = false)
                    )
                    return@launch
                }
                val offset = linkableState.items.size

                @Suppress("TooGenericExceptionCaught")
                try {
                    val result = withContext(ioDispatcher) {
                        fetchLinkableItems(site, currentState.selectedTypeOption, offset)
                    }

                    val updatedState = _menuItemDetailState.value ?: return@launch
                    // Only update if the type hasn't changed while loading
                    if (updatedState.selectedTypeOption == currentState.selectedTypeOption) {
                        _menuItemDetailState.value = when (result) {
                            is NavMenuRestClient.LinkableItemsResult.Success -> {
                                updatedState.copy(
                                    linkableItemsState = updatedState.linkableItemsState.copy(
                                        isLoadingMore = false,
                                        canLoadMore = result.canLoadMore,
                                        items = updatedState.linkableItemsState.items + result.items
                                    )
                                )
                            }
                            is NavMenuRestClient.LinkableItemsResult.Error -> {
                                updatedState.copy(
                                    linkableItemsState = updatedState.linkableItemsState.copy(
                                        isLoadingMore = false,
                                        canLoadMore = true
                                    )
                                )
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.e(AppLog.T.API, "Failed to load more linkable items", e)
                    val updatedState = _menuItemDetailState.value ?: return@launch
                    _menuItemDetailState.value = updatedState.copy(
                        linkableItemsState = updatedState.linkableItemsState.copy(
                            isLoadingMore = false,
                            canLoadMore = true
                        )
                    )
                }
            }
        }
    }

    private suspend fun fetchLinkableItems(
        site: SiteModel,
        typeOption: MenuItemTypeOption,
        offset: Int
    ): NavMenuRestClient.LinkableItemsResult {
        return when (typeOption) {
            MenuItemTypeOption.POST -> navMenuRestClient.fetchPosts(site, offset)
            MenuItemTypeOption.PAGE -> navMenuRestClient.fetchPages(site, offset)
            MenuItemTypeOption.CATEGORY -> navMenuRestClient.fetchCategories(site, offset)
            MenuItemTypeOption.TAG -> navMenuRestClient.fetchTags(site, offset)
            MenuItemTypeOption.CUSTOM_LINK -> {
                NavMenuRestClient.LinkableItemsResult.Success(emptyList(), canLoadMore = false)
            }
        }
    }

    fun updateSelectedLinkableItem(item: LinkableItemOption) {
        val currentState = _menuItemDetailState.value ?: return
        _menuItemDetailState.value = currentState.copy(
            selectedLinkableItem = item,
            objectId = item.id,
            title = item.title
        )
    }

    fun moveMenuItemUp(itemId: Long) {
        reorderMenuItem(itemId, ReorderDirection.UP)
    }

    fun moveMenuItemDown(itemId: Long) {
        reorderMenuItem(itemId, ReorderDirection.DOWN)
    }

    private fun reorderMenuItem(itemId: Long, direction: ReorderDirection) {
        val currentItems = _menuItemListState.value.items
        val index = currentItems.indexOfFirst { it.id == itemId }
        if (index < 0) return

        val item = currentItems[index]
        val indentLevel = item.indentLevel

        // Find the sibling to swap with
        val siblingIndex = findSiblingIndex(currentItems, index, indentLevel, direction)
        if (siblingIndex < 0) return

        val sibling = currentItems[siblingIndex]

        // Get the end indices for both subtrees (item + descendants, sibling + descendants)
        val itemSubtreeEnd = findSubtreeEnd(currentItems, index)
        val siblingSubtreeEnd = findSubtreeEnd(currentItems, siblingIndex)

        // Extract subtrees
        val itemSubtree = currentItems.subList(index, itemSubtreeEnd + 1)
        val siblingSubtree = currentItems.subList(siblingIndex, siblingSubtreeEnd + 1)

        // Create new list with swapped subtrees
        val newItems = when (direction) {
            ReorderDirection.UP -> {
                // Moving up: sibling is before item
                currentItems.subList(0, siblingIndex) +
                    itemSubtree +
                    siblingSubtree +
                    currentItems.subList(itemSubtreeEnd + 1, currentItems.size)
            }
            ReorderDirection.DOWN -> {
                // Moving down: sibling is after item
                currentItems.subList(0, index) +
                    siblingSubtree +
                    itemSubtree +
                    currentItems.subList(siblingSubtreeEnd + 1, currentItems.size)
            }
        }

        // Save original state for rollback
        val originalItems = currentItems

        // Optimistically update the UI
        _menuItemListState.value = _menuItemListState.value.copy(items = newItems)

        // Update menu orders on the server
        viewModelScope.launch {
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch
            val success = withContext(ioDispatcher) {
                updateMenuItemOrder(site, itemId, sibling.id)
            }

            if (!success) {
                // Rollback UI state on failure
                _menuItemListState.value = _menuItemListState.value.copy(items = originalItems)
                _uiEvent.value = NavMenusUiEvent.ShowError(
                    resourceProvider.getString(R.string.menu_item_reorder_failed)
                )
            }
        }
    }

    private fun findSiblingIndex(
        items: List<MenuItemUiModel>,
        fromIndex: Int,
        indentLevel: Int,
        direction: ReorderDirection
    ): Int {
        val range = when (direction) {
            ReorderDirection.UP -> (fromIndex - 1 downTo 0)
            ReorderDirection.DOWN -> ((fromIndex + 1) until items.size)
        }
        return range.asSequence()
            .takeWhile { items[it].indentLevel >= indentLevel }
            .firstOrNull { items[it].indentLevel == indentLevel }
            ?: -1
    }

    private fun findSubtreeEnd(items: List<MenuItemUiModel>, startIndex: Int): Int {
        val startIndent = items[startIndex].indentLevel
        var endIndex = startIndex
        for (i in (startIndex + 1) until items.size) {
            if (items[i].indentLevel <= startIndent) break
            endIndex = i
        }
        return endIndex
    }

    private enum class ReorderDirection { UP, DOWN }

    private suspend fun updateMenuItemOrder(site: SiteModel, itemId: Long, targetItemId: Long): Boolean {
        val itemToMove = currentMenuItems.find { it.remoteItemId == itemId }
        val swapWithItem = currentMenuItems.find { it.remoteItemId == targetItemId }

        if (itemToMove == null || swapWithItem == null) return false

        val updatedItemToMove = itemToMove.copy(menuOrder = swapWithItem.menuOrder)
        val updatedSwapWithItem = swapWithItem.copy(menuOrder = itemToMove.menuOrder)

        val result1 = navMenuRestClient.updateMenuItem(site, updatedItemToMove)
        val result2 = if (result1 is NavMenuRestClient.NavMenuItemResult.Success) {
            navMenuRestClient.updateMenuItem(site, updatedSwapWithItem)
        } else {
            result1
        }

        return result2 is NavMenuRestClient.NavMenuItemResult.Success
    }

    fun saveMenuItem() {
        viewModelScope.launch {
            val state = _menuItemDetailState.value ?: return@launch
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch

            validateMenuItemState(state)?.let { errorMessage ->
                _uiEvent.value = NavMenusUiEvent.ShowError(errorMessage)
                return@launch
            }

            _menuItemDetailState.value = state.copy(isSaving = true)

            val item = createMenuItemModel(site, state)

            withContext(ioDispatcher) {
                val result = if (state.isNew) {
                    navMenuRestClient.createMenuItem(site, item)
                } else {
                    navMenuRestClient.updateMenuItem(site, item)
                }

                withContext(mainDispatcher) {
                    _menuItemDetailState.value = state.copy(isSaving = false)
                    when (result) {
                        is NavMenuRestClient.NavMenuItemResult.Success -> {
                            _uiEvent.value = NavMenusUiEvent.MenuItemSaved
                            navigateBack()
                            loadMenuItems(state.menuId)
                        }
                        is NavMenuRestClient.NavMenuItemResult.Error -> {
                            _uiEvent.value = NavMenusUiEvent.ShowError(result.message)
                        }
                    }
                }
            }
        }
    }

    private fun validateMenuItemState(state: MenuItemDetailUiState): String? {
        return when {
            state.title.isBlank() ->
                resourceProvider.getString(R.string.menu_item_title_required)
            state.selectedTypeOption == MenuItemTypeOption.CUSTOM_LINK && state.url.isBlank() ->
                resourceProvider.getString(R.string.menu_item_url_required)
            state.selectedTypeOption != MenuItemTypeOption.CUSTOM_LINK && state.objectId <= 0 ->
                resourceProvider.getString(R.string.menu_item_select_required)
            state.url.isNotBlank() && !isValidLinkUrl(state.url) ->
                resourceProvider.getString(R.string.menu_item_invalid_url)
            else -> null
        }
    }

    private fun createMenuItemModel(site: SiteModel, state: MenuItemDetailUiState): NavMenuItemModel {
        return NavMenuItemModel(
            localSiteId = site.id,
            remoteItemId = state.itemId,
            menuId = state.menuId,
            title = state.title,
            url = state.url,
            type = state.type,
            objectType = state.objectType,
            objectId = state.objectId,
            parentId = state.parentId,
            menuOrder = state.menuOrder,
            target = state.target,
            classes = if (state.cssClasses.isNotEmpty()) {
                "[\"${state.cssClasses.replace(",", "\",\"")}\"]"
            } else {
                "[]"
            },
            description = state.description,
            attrTitle = state.attrTitle
        )
    }

    fun deleteMenuItem() {
        viewModelScope.launch {
            val state = _menuItemDetailState.value ?: return@launch
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch

            if (state.itemId <= 0) return@launch

            _menuItemDetailState.value = state.copy(isDeleting = true)

            withContext(ioDispatcher) {
                val result = navMenuRestClient.deleteMenuItem(site, state.itemId)

                withContext(mainDispatcher) {
                    _menuItemDetailState.value = state.copy(isDeleting = false)
                    when (result) {
                        is NavMenuRestClient.NavMenuItemDeleteResult.Success -> {
                            _uiEvent.value = NavMenusUiEvent.MenuItemDeleted
                            navigateBack()
                            loadMenuItems(state.menuId)
                        }
                        is NavMenuRestClient.NavMenuItemDeleteResult.Error -> {
                            _uiEvent.value = NavMenusUiEvent.ShowError(result.message)
                        }
                    }
                }
            }
        }
    }

    fun consumeUiEvent() {
        _uiEvent.value = null
    }

    /**
     * Validates a URL against WordPress's allowed protocols.
     * See: https://developer.wordpress.org/reference/functions/wp_allowed_protocols/
     */
    private fun isValidLinkUrl(url: String): Boolean {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) return false

        return when {
            // Anchor links are valid (e.g., #section or #contact)
            trimmedUrl.startsWith("#") -> trimmedUrl.length > 1
            // Protocol-relative URLs are valid
            trimmedUrl.startsWith("//") -> trimmedUrl.length > 2
            else -> ALLOWED_PROTOCOLS.any { trimmedUrl.lowercase().startsWith("$it:") }
        }
    }

    private fun sanitizeInput(input: String, maxLength: Int): String {
        return input.trim().take(maxLength)
    }

    companion object {
        private const val MAX_MENU_NAME_LENGTH = 200
        private const val MAX_MENU_DESCRIPTION_LENGTH = 500
        private const val MAX_MENU_ITEM_TITLE_LENGTH = 200
        private const val MAX_MENU_ITEM_DESCRIPTION_LENGTH = 500

        /**
         * WordPress allowed protocols from wp_allowed_protocols().
         * See: https://developer.wordpress.org/reference/functions/wp_allowed_protocols/
         */
        private val ALLOWED_PROTOCOLS = listOf(
            "http", "https", "ftp", "ftps", "mailto", "news", "irc", "irc6", "ircs",
            "gopher", "nntp", "feed", "telnet", "mms", "rtsp", "sms", "svn", "tel",
            "fax", "xmpp", "webcal", "urn"
        )
    }
}
