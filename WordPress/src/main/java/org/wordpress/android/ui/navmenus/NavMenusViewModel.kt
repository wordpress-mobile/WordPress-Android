package org.wordpress.android.ui.navmenus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.navmenu.NavMenuItemModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuModel
import org.wordpress.android.fluxc.network.rest.wpapi.navmenu.NavMenuRsRestClient
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class NavMenusViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val navMenuRestClient: NavMenuRsRestClient,
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

    fun setNavController(controller: NavHostController) {
        navController = controller
    }

    fun loadMenus() {
        viewModelScope.launch {
            _menuListState.value = _menuListState.value.copy(isLoading = true, error = null)

            val site = selectedSiteRepository.getSelectedSite()
            if (site == null) {
                _menuListState.value = _menuListState.value.copy(
                    isLoading = false,
                    error = "No site selected"
                )
                return@launch
            }

            try {
                withContext(ioDispatcher) {
                    // Fetch menus
                    val menusResult = navMenuRestClient.fetchMenus(site)
                    // Fetch locations
                    val locationsResult = navMenuRestClient.fetchMenuLocations(site)

                    withContext(mainDispatcher) {
                        when (menusResult) {
                            is NavMenuRsRestClient.NavMenusResult.Success -> {
                                currentMenus = menusResult.menus

                                // For each menu, count items (we'll fetch items lazily)
                                val menuUiModels = menusResult.menus.map { menu ->
                                    menu.toUiModel(0) // Item count will be loaded when entering menu
                                }

                                val locations = when (locationsResult) {
                                    is NavMenuRsRestClient.NavMenuLocationsResult.Success -> {
                                        locationsResult.locations.map { it.toUiModel() }
                                    }
                                    is NavMenuRsRestClient.NavMenuLocationsResult.Error -> emptyList()
                                }

                                _menuListState.value = MenuListUiState(
                                    isLoading = false,
                                    menus = menuUiModels,
                                    locations = locations
                                )
                            }
                            is NavMenuRsRestClient.NavMenusResult.Error -> {
                                val errorMessage = menusResult.message.takeIf { it.isNotBlank() }
                                    ?: "Failed to load menus"
                                _menuListState.value = MenuListUiState(
                                    isLoading = false,
                                    error = errorMessage
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _menuListState.value = MenuListUiState(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
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

        val selectedLocations = if (menu.locations.isNotEmpty() && menu.locations != "[]") {
            menu.locations.trim('[', ']')
                .split(",")
                .map { it.trim().trim('"') }
                .filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        _menuDetailState.value = MenuDetailUiState(
            menuId = menu.remoteMenuId,
            name = menu.name,
            description = menu.description,
            autoAdd = menu.autoAdd,
            selectedLocations = selectedLocations,
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
                val result = navMenuRestClient.fetchMenuItems(site, menuId)

                withContext(mainDispatcher) {
                    when (result) {
                        is NavMenuRsRestClient.NavMenuItemsResult.Success -> {
                            currentMenuItems = result.items
                            val sortedItems = sortItemsHierarchically(result.items)
                            _menuItemListState.value = _menuItemListState.value.copy(
                                isLoading = false,
                                items = sortedItems
                            )
                        }
                        is NavMenuRsRestClient.NavMenuItemsResult.Error -> {
                            _menuItemListState.value = _menuItemListState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sortItemsHierarchically(items: List<NavMenuItemModel>): List<MenuItemUiModel> {
        val result = mutableListOf<MenuItemUiModel>()
        val itemsById = items.associateBy { it.remoteItemId }
        val visited = mutableSetOf<Long>()

        fun addItemWithChildren(item: NavMenuItemModel, indentLevel: Int) {
            if (item.remoteItemId in visited) return
            visited.add(item.remoteItemId)
            result.add(item.toUiModel(indentLevel))

            // Find and add children sorted by menu order
            items.filter { it.parentId == item.remoteItemId }
                .sortedBy { it.menuOrder }
                .forEach { child ->
                    addItemWithChildren(child, indentLevel + 1)
                }
        }

        // Start with root items (parent = 0)
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
            menuOrder = currentMenuItems.maxOfOrNull { it.menuOrder }?.plus(1) ?: 0,
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
        _menuDetailState.value = _menuDetailState.value?.copy(name = name)
    }

    fun updateMenuDescription(description: String) {
        _menuDetailState.value = _menuDetailState.value?.copy(description = description)
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
                _uiEvent.value = NavMenusUiEvent.ShowError("Menu name is required")
                return@launch
            }

            _menuDetailState.value = state.copy(isSaving = true)

            val menu = NavMenuModel().apply {
                localSiteId = site.id
                remoteMenuId = state.menuId
                name = state.name
                description = state.description
                locations = state.selectedLocations.joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]"
                ) { "\"$it\"" }
                autoAdd = state.autoAdd
            }

            withContext(ioDispatcher) {
                val result = if (state.isNew) {
                    navMenuRestClient.createMenu(site, menu)
                } else {
                    navMenuRestClient.updateMenu(site, menu)
                }

                withContext(mainDispatcher) {
                    _menuDetailState.value = state.copy(isSaving = false)
                    when (result) {
                        is NavMenuRsRestClient.NavMenuResult.Success -> {
                            _uiEvent.value = NavMenusUiEvent.MenuSaved
                            navigateBack()
                            loadMenus()
                        }
                        is NavMenuRsRestClient.NavMenuResult.Error -> {
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
                        is NavMenuRsRestClient.NavMenuDeleteResult.Success -> {
                            _uiEvent.value = NavMenusUiEvent.MenuDeleted
                            navigateBack()
                            loadMenus()
                        }
                        is NavMenuRsRestClient.NavMenuDeleteResult.Error -> {
                            _uiEvent.value = NavMenusUiEvent.ShowError(result.message)
                        }
                    }
                }
            }
        }
    }

    // Menu item detail update methods
    fun updateMenuItemTitle(title: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(title = title)
    }

    fun updateMenuItemUrl(url: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(url = url)
    }

    fun updateMenuItemParent(parentId: Long) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(parentId = parentId)
    }

    fun updateMenuItemTarget(target: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(target = target)
    }

    fun updateMenuItemCssClasses(classes: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(cssClasses = classes)
    }

    fun updateMenuItemDescription(description: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(description = description)
    }

    fun updateMenuItemAttrTitle(attrTitle: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(attrTitle = attrTitle)
    }

    fun moveMenuItemUp(itemId: Long) {
        reorderMenuItem(itemId, -1)
    }

    fun moveMenuItemDown(itemId: Long) {
        reorderMenuItem(itemId, 1)
    }

    private fun reorderMenuItem(itemId: Long, direction: Int) {
        val currentItems = _menuItemListState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.id == itemId }
        val newIndex = index + direction
        val canReorder = index >= 0 &&
            newIndex >= 0 &&
            newIndex < currentItems.size &&
            currentItems[index].indentLevel == currentItems[newIndex].indentLevel

        if (canReorder) {
            val currentItem = currentItems[index]
            val targetItem = currentItems[newIndex]

            // Swap the items
            currentItems[index] = targetItem
            currentItems[newIndex] = currentItem

            _menuItemListState.value = _menuItemListState.value.copy(items = currentItems)

            // Update menu orders on the server
            viewModelScope.launch {
                val site = selectedSiteRepository.getSelectedSite() ?: return@launch
                withContext(ioDispatcher) {
                    // Update both items with new order
                    val itemToMove = currentMenuItems.find { it.remoteItemId == itemId }
                    val swapWithItem = currentMenuItems.find { it.remoteItemId == targetItem.id }

                    if (itemToMove != null && swapWithItem != null) {
                        val newOrderForMovedItem = swapWithItem.menuOrder
                        val newOrderForSwapped = itemToMove.menuOrder

                        itemToMove.menuOrder = newOrderForMovedItem
                        swapWithItem.menuOrder = newOrderForSwapped

                        navMenuRestClient.updateMenuItem(site, itemToMove)
                        navMenuRestClient.updateMenuItem(site, swapWithItem)
                    }
                }
            }
        }
    }

    fun saveMenuItem() {
        viewModelScope.launch {
            val state = _menuItemDetailState.value ?: return@launch
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch

            if (state.title.isBlank()) {
                _uiEvent.value = NavMenusUiEvent.ShowError("Title is required")
                return@launch
            }

            if (state.type == NavMenuItemModel.TYPE_CUSTOM && state.url.isBlank()) {
                _uiEvent.value = NavMenusUiEvent.ShowError("URL is required for custom links")
                return@launch
            }

            _menuItemDetailState.value = state.copy(isSaving = true)

            val item = NavMenuItemModel().apply {
                localSiteId = site.id
                remoteItemId = state.itemId
                menuId = state.menuId
                title = state.title
                url = state.url
                type = state.type
                objectType = state.objectType
                objectId = state.objectId
                parentId = state.parentId
                menuOrder = state.menuOrder
                target = state.target
                classes = if (state.cssClasses.isNotEmpty()) {
                    "[\"${state.cssClasses.replace(",", "\",\"")}\"]"
                } else {
                    "[]"
                }
                description = state.description
                attrTitle = state.attrTitle
            }

            withContext(ioDispatcher) {
                val result = if (state.isNew) {
                    navMenuRestClient.createMenuItem(site, item)
                } else {
                    navMenuRestClient.updateMenuItem(site, item)
                }

                withContext(mainDispatcher) {
                    _menuItemDetailState.value = state.copy(isSaving = false)
                    when (result) {
                        is NavMenuRsRestClient.NavMenuItemResult.Success -> {
                            _uiEvent.value = NavMenusUiEvent.MenuItemSaved
                            navigateBack()
                            loadMenuItems(state.menuId)
                        }
                        is NavMenuRsRestClient.NavMenuItemResult.Error -> {
                            _uiEvent.value = NavMenusUiEvent.ShowError(result.message)
                        }
                    }
                }
            }
        }
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
                        is NavMenuRsRestClient.NavMenuItemDeleteResult.Success -> {
                            _uiEvent.value = NavMenusUiEvent.MenuItemDeleted
                            navigateBack()
                            loadMenuItems(state.menuId)
                        }
                        is NavMenuRsRestClient.NavMenuItemDeleteResult.Error -> {
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
}
