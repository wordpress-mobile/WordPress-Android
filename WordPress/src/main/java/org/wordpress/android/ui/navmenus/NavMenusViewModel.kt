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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuItemModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.navmenus.data.NavMenuRestClient
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ResourceProvider
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class NavMenusViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val navMenuRestClient: NavMenuRestClient,
    private val pageStore: PageStore,
    private val postStore: PostStore,
    private val taxonomyStore: TaxonomyStore,
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

            try {
                val newState = withContext(ioDispatcher) { fetchMenuData(site) }
                _menuListState.value = newState
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _menuListState.value = MenuListUiState(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Network error occurred"
                )
            }
        }
    }

    private suspend fun fetchMenuData(site: SiteModel): MenuListUiState {
        val menusResult = navMenuRestClient.fetchMenus(site)
        val locationsResult = navMenuRestClient.fetchMenuLocations(site)
        val allItemsResult = navMenuRestClient.fetchAllMenuItems(site)

        val itemCountByMenuId = buildItemCountMap(allItemsResult)

        return when (menusResult) {
            is NavMenuRestClient.NavMenusResult.Success -> {
                currentMenus = menusResult.menus
                buildSuccessState(menusResult.menus, locationsResult, itemCountByMenuId)
            }
            is NavMenuRestClient.NavMenusResult.Error -> {
                val errorMessage = menusResult.message.takeIf { it.isNotBlank() } ?: "Failed to load menus"
                MenuListUiState(isLoading = false, error = errorMessage)
            }
        }
    }

    private fun buildItemCountMap(
        result: NavMenuRestClient.NavMenuItemsResult
    ): Map<Long, Int> = when (result) {
        is NavMenuRestClient.NavMenuItemsResult.Success -> {
            result.items.groupingBy { it.menuId }.eachCount()
        }
        is NavMenuRestClient.NavMenuItemsResult.Error -> emptyMap()
    }

    private fun buildSuccessState(
        menus: List<NavMenuModel>,
        locationsResult: NavMenuRestClient.NavMenuLocationsResult,
        itemCountByMenuId: Map<Long, Int>
    ): MenuListUiState {
        val menuUiModels = menus.map { menu ->
            menu.toUiModel(itemCountByMenuId[menu.remoteMenuId] ?: 0)
        }

        val locations = when (locationsResult) {
            is NavMenuRestClient.NavMenuLocationsResult.Success -> {
                locationsResult.locations.map { it.toUiModel() }
            }
            is NavMenuRestClient.NavMenuLocationsResult.Error -> emptyList()
        }

        return MenuListUiState(
            isLoading = false,
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
                val result = navMenuRestClient.fetchMenuItems(site, menuId)

                withContext(mainDispatcher) {
                    when (result) {
                        is NavMenuRestClient.NavMenuItemsResult.Success -> {
                            currentMenuItems = result.items
                            val sortedItems = sortItemsHierarchically(result.items)
                            _menuItemListState.value = _menuItemListState.value.copy(
                                isLoading = false,
                                items = sortedItems
                            )
                        }
                        is NavMenuRestClient.NavMenuItemsResult.Error -> {
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
                _uiEvent.value = NavMenusUiEvent.ShowError(
                    resourceProvider.getString(R.string.menu_name_required)
                )
                return@launch
            }

            _menuDetailState.value = state.copy(isSaving = true)

            val menu = NavMenuModel().apply {
                localSiteId = site.id
                remoteMenuId = state.menuId
                name = state.name
                description = state.description
                locations = state.selectedLocations.toJsonStringArray()
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
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(title = title)
    }

    fun updateMenuItemUrl(url: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(url = url)
    }

    fun updateMenuItemParent(parentId: Long) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(parentId = parentId)
    }

    fun updateMenuItemDescription(description: String) {
        _menuItemDetailState.value = _menuItemDetailState.value?.copy(description = description)
    }

    fun updateMenuItemType(typeOption: MenuItemTypeOption) {
        val currentState = _menuItemDetailState.value ?: return
        _menuItemDetailState.value = currentState.copy(
            selectedTypeOption = typeOption,
            type = typeOption.type,
            objectType = typeOption.objectType,
            url = if (typeOption == MenuItemTypeOption.CUSTOM_LINK) currentState.url else "",
            objectId = 0L,
            selectedLinkableItem = null,
            linkableItemsState = LinkableItemsState()
        )

        if (typeOption != MenuItemTypeOption.CUSTOM_LINK) {
            loadLinkableItems(typeOption)
        }
    }

    fun updateSelectedLinkableItem(item: LinkableItemOption) {
        val currentState = _menuItemDetailState.value ?: return
        _menuItemDetailState.value = currentState.copy(
            selectedLinkableItem = item,
            objectId = item.id,
            title = if (currentState.title.isBlank()) item.title else currentState.title
        )
    }

    private fun loadLinkableItems(typeOption: MenuItemTypeOption) {
        viewModelScope.launch {
            val site = selectedSiteRepository.getSelectedSite() ?: return@launch
            _menuItemDetailState.value = _menuItemDetailState.value?.copy(
                linkableItemsState = LinkableItemsState(isLoading = true)
            )

            withContext(ioDispatcher) {
                val items = when (typeOption) {
                    MenuItemTypeOption.PAGE -> loadPages(site)
                    MenuItemTypeOption.POST -> loadPosts(site)
                    MenuItemTypeOption.CATEGORY -> loadCategories(site)
                    MenuItemTypeOption.TAG -> loadTags(site)
                    MenuItemTypeOption.CUSTOM_LINK -> emptyList()
                }

                withContext(mainDispatcher) {
                    _menuItemDetailState.value = _menuItemDetailState.value?.copy(
                        linkableItemsState = LinkableItemsState(
                            isLoading = false,
                            items = items
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadPages(site: SiteModel): List<LinkableItemOption> {
        pageStore.requestPagesFromServer(site, false)
        val pages = pageStore.getPagesFromDb(site)
        return buildHierarchicalList(
            pages.filter { it.status == org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED }
                .map { Triple(it.remoteId, it.title, it.parent?.remoteId ?: 0L) }
        )
    }

    private fun loadPosts(site: SiteModel): List<LinkableItemOption> {
        val posts = postStore.getPostsForSite(site)
        return posts
            .filter { PostStatus.fromPost(it) == PostStatus.PUBLISHED }
            .map { LinkableItemOption(id = it.remotePostId, title = it.title) }
    }

    private fun loadCategories(site: SiteModel): List<LinkableItemOption> {
        val categories = taxonomyStore.getCategoriesForSite(site)
        return buildHierarchicalList(
            categories.map { Triple(it.remoteTermId, it.name, it.parentRemoteId) }
        )
    }

    private fun loadTags(site: SiteModel): List<LinkableItemOption> {
        val tags = taxonomyStore.getTagsForSite(site)
        return tags.map { LinkableItemOption(id = it.remoteTermId, title = it.name) }
    }

    private fun buildHierarchicalList(
        items: List<Triple<Long, String, Long>>
    ): List<LinkableItemOption> {
        val result = mutableListOf<LinkableItemOption>()
        val itemsById = items.associateBy { it.first }
        val visited = mutableSetOf<Long>()

        fun addItemWithChildren(itemId: Long, indentLevel: Int) {
            if (itemId in visited) return
            val item = itemsById[itemId] ?: return
            visited.add(itemId)
            result.add(LinkableItemOption(id = item.first, title = item.second, indentLevel = indentLevel))

            items.filter { it.third == itemId }
                .sortedBy { it.second }
                .forEach { child -> addItemWithChildren(child.first, indentLevel + 1) }
        }

        // Start with root items (parent = 0)
        items.filter { it.third == 0L || itemsById[it.third] == null }
            .sortedBy { it.second }
            .forEach { addItemWithChildren(it.first, 0) }

        return result
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

            // Save original state for rollback
            val originalItems = _menuItemListState.value.items

            // Optimistically update the UI
            currentItems[index] = targetItem
            currentItems[newIndex] = currentItem
            _menuItemListState.value = _menuItemListState.value.copy(items = currentItems)

            // Update menu orders on the server
            viewModelScope.launch {
                val site = selectedSiteRepository.getSelectedSite() ?: return@launch
                val success = withContext(ioDispatcher) {
                    updateMenuItemOrder(site, itemId, targetItem.id)
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
    }

    private suspend fun updateMenuItemOrder(site: SiteModel, itemId: Long, targetItemId: Long): Boolean {
        val itemToMove = currentMenuItems.find { it.remoteItemId == itemId }
        val swapWithItem = currentMenuItems.find { it.remoteItemId == targetItemId }

        if (itemToMove == null || swapWithItem == null) return false

        val originalOrderForMoved = itemToMove.menuOrder
        val originalOrderForSwapped = swapWithItem.menuOrder

        itemToMove.menuOrder = originalOrderForSwapped
        swapWithItem.menuOrder = originalOrderForMoved

        val result1 = navMenuRestClient.updateMenuItem(site, itemToMove)
        if (result1 is NavMenuRestClient.NavMenuItemResult.Error) {
            // Restore original orders
            itemToMove.menuOrder = originalOrderForMoved
            swapWithItem.menuOrder = originalOrderForSwapped
            return false
        }

        val result2 = navMenuRestClient.updateMenuItem(site, swapWithItem)
        if (result2 is NavMenuRestClient.NavMenuItemResult.Error) {
            // Restore original orders
            itemToMove.menuOrder = originalOrderForMoved
            swapWithItem.menuOrder = originalOrderForSwapped
            return false
        }

        return true
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
            state.url.isNotBlank() && !isValidUrl(state.url) ->
                resourceProvider.getString(R.string.menu_item_invalid_url)
            else -> null
        }
    }

    private fun createMenuItemModel(site: SiteModel, state: MenuItemDetailUiState): NavMenuItemModel {
        return NavMenuItemModel().apply {
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

    private fun isValidUrl(url: String): Boolean {
        return android.util.Patterns.WEB_URL.matcher(url).matches()
    }
}
