package org.wordpress.android.ui.navmenus

import org.wordpress.android.fluxc.model.navmenu.NavMenuItemModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuLocationModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuModel

/**
 * UI state for the menus list screen
 */
data class MenuListUiState(
    val isLoading: Boolean = false,
    val menus: List<MenuUiModel> = emptyList(),
    val locations: List<LocationUiModel> = emptyList(),
    val error: String? = null
)

/**
 * UI model for a single menu in the list
 */
data class MenuUiModel(
    val id: Long,
    val name: String,
    val description: String,
    val itemCount: Int,
    val locations: List<String>
)

/**
 * UI model for a menu location
 */
data class LocationUiModel(
    val name: String,
    val description: String,
    val menuId: Long
)

/**
 * UI state for menu detail/edit screen
 */
data class MenuDetailUiState(
    val menuId: Long = 0L,
    val name: String = "",
    val description: String = "",
    val autoAdd: Boolean = false,
    val selectedLocations: List<String> = emptyList(),
    val availableLocations: List<LocationUiModel> = emptyList(),
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isNew: Boolean = true
)

/**
 * UI state for menu items list screen
 */
data class MenuItemListUiState(
    val isLoading: Boolean = false,
    val menuId: Long = 0L,
    val menuName: String = "",
    val items: List<MenuItemUiModel> = emptyList(),
    val error: String? = null
)

/**
 * UI model for a single menu item
 */
data class MenuItemUiModel(
    val id: Long,
    val title: String,
    val url: String,
    val type: String,
    val typeLabel: String,
    val parentId: Long,
    val menuOrder: Int,
    val indentLevel: Int
)

/**
 * UI state for menu item detail/edit screen
 */
data class MenuItemDetailUiState(
    val itemId: Long = 0L,
    val menuId: Long = 0L,
    val title: String = "",
    val url: String = "",
    val type: String = NavMenuItemModel.TYPE_CUSTOM,
    val objectType: String = "",
    val objectId: Long = 0L,
    val parentId: Long = 0L,
    val menuOrder: Int = 0,
    val target: String = "",
    val cssClasses: String = "",
    val description: String = "",
    val attrTitle: String = "",
    val availableParents: List<ParentItemOption> = emptyList(),
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isNew: Boolean = true
)

/**
 * Option for parent item selection
 */
data class ParentItemOption(
    val id: Long,
    val title: String,
    val indentLevel: Int
)

/**
 * One-time UI events
 */
sealed class NavMenusUiEvent {
    data class ShowError(val message: String) : NavMenusUiEvent()
    data class ShowMessage(val message: String) : NavMenusUiEvent()
    data object NavigateBack : NavMenusUiEvent()
    data object MenuSaved : NavMenusUiEvent()
    data object MenuDeleted : NavMenusUiEvent()
    data object MenuItemSaved : NavMenusUiEvent()
    data object MenuItemDeleted : NavMenusUiEvent()
}

/**
 * Navigation routes for the menus screens
 */
enum class NavMenuScreen {
    MenuList,
    MenuDetail,
    MenuItemList,
    MenuItemDetail
}

/**
 * Helper function to convert NavMenuModel to MenuUiModel
 */
fun NavMenuModel.toUiModel(itemCount: Int): MenuUiModel {
    val locationsList = if (locations.isNotEmpty() && locations != "[]") {
        locations.trim('[', ']')
            .split(",")
            .map { it.trim().trim('"') }
            .filter { it.isNotEmpty() }
    } else {
        emptyList()
    }

    return MenuUiModel(
        id = remoteMenuId,
        name = name,
        description = description,
        itemCount = itemCount,
        locations = locationsList
    )
}

/**
 * Helper function to convert NavMenuLocationModel to LocationUiModel
 */
fun NavMenuLocationModel.toUiModel(): LocationUiModel {
    return LocationUiModel(
        name = name,
        description = description,
        menuId = menuId
    )
}

/**
 * Helper function to convert NavMenuItemModel to MenuItemUiModel
 */
fun NavMenuItemModel.toUiModel(indentLevel: Int): MenuItemUiModel {
    val typeLabel = when (type) {
        NavMenuItemModel.TYPE_CUSTOM -> "Custom Link"
        NavMenuItemModel.TYPE_POST_TYPE -> when (objectType) {
            NavMenuItemModel.OBJECT_TYPE_PAGE -> "Page"
            NavMenuItemModel.OBJECT_TYPE_POST -> "Post"
            else -> objectType.replaceFirstChar { it.uppercase() }
        }
        NavMenuItemModel.TYPE_TAXONOMY -> when (objectType) {
            NavMenuItemModel.OBJECT_TYPE_CATEGORY -> "Category"
            NavMenuItemModel.OBJECT_TYPE_TAG -> "Tag"
            else -> objectType.replaceFirstChar { it.uppercase() }
        }
        NavMenuItemModel.TYPE_POST_TYPE_ARCHIVE -> "Archive"
        else -> type.replaceFirstChar { it.uppercase() }
    }

    return MenuItemUiModel(
        id = remoteItemId,
        title = title,
        url = url,
        type = type,
        typeLabel = typeLabel,
        parentId = parentId,
        menuOrder = menuOrder,
        indentLevel = indentLevel
    )
}
