package org.wordpress.android.ui.navmenus

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.navmenus.screens.MenuDetailScreen
import org.wordpress.android.ui.navmenus.screens.MenuItemDetailScreen
import org.wordpress.android.ui.navmenus.screens.MenuItemListScreen
import org.wordpress.android.ui.navmenus.screens.MenuListScreen
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class NavMenusActivity : BaseAppCompatActivity() {
    private val viewModel by viewModels<NavMenusViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        composeView = ComposeView(this)
        setContentView(
            composeView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    NavMenusContent()
                }
            }
        )

        viewModel.loadMenus()
        observeUiEvents()
    }

    private fun observeUiEvents() {
        lifecycleScope.launch {
            viewModel.uiEvent.filterNotNull().collect { event ->
                handleUiEvent(event)
                viewModel.consumeUiEvent()
            }
        }
    }

    private fun handleUiEvent(event: NavMenusUiEvent) {
        when (event) {
            is NavMenusUiEvent.ShowError -> {
                ToastUtils.showToast(this, event.message, ToastUtils.Duration.LONG)
            }
            is NavMenusUiEvent.MenuSaved -> {
                ToastUtils.showToast(this, R.string.menu_saved)
            }
            is NavMenusUiEvent.MenuDeleted -> {
                ToastUtils.showToast(this, R.string.menu_deleted)
            }
            is NavMenusUiEvent.MenuItemSaved -> {
                ToastUtils.showToast(this, R.string.menu_item_saved)
            }
            is NavMenusUiEvent.MenuItemDeleted -> {
                ToastUtils.showToast(this, R.string.menu_item_deleted)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NavMenusContent() {
        navController = rememberNavController()

        LaunchedEffect(navController) {
            viewModel.setNavController(navController)
        }

        val currentBackStackEntry by navController.currentBackStackEntryFlow
            .collectAsState(initial = navController.currentBackStackEntry)
        val currentRoute = currentBackStackEntry?.destination?.route

        var isFabVisible by remember { mutableStateOf(true) }

        LaunchedEffect(currentRoute) {
            isFabVisible = true
        }

        AppThemeM3 {
            Scaffold(
                topBar = { NavMenusTopBar(currentRoute) },
                floatingActionButton = { NavMenusFab(currentRoute, isFabVisible) }
            ) { contentPadding ->
                NavHost(
                    navController = navController,
                    startDestination = NavMenuScreen.MenuList.name
                ) {
                    composable(NavMenuScreen.MenuList.name) {
                        val state by viewModel.menuListState.collectAsState()
                        MenuListScreen(
                            state = state,
                            onEditMenuClick = { viewModel.navigateToEditMenu(it) },
                            onMenuItemsClick = { viewModel.navigateToMenuItems(it) },
                            onRefresh = { viewModel.refreshMenus() },
                            onLoadMore = { viewModel.loadMoreMenus() },
                            onFabVisibilityChange = { isFabVisible = it },
                            modifier = Modifier.padding(contentPadding)
                        )
                    }
                    composable(NavMenuScreen.MenuDetail.name) {
                        val state by viewModel.menuDetailState.collectAsState()
                        MenuDetailScreen(
                            state = state,
                            onNameChange = { viewModel.updateMenuName(it) },
                            onDescriptionChange = { viewModel.updateMenuDescription(it) },
                            onAutoAddChange = { viewModel.updateMenuAutoAdd(it) },
                            onLocationToggle = { viewModel.toggleMenuLocation(it) },
                            onSaveClick = { viewModel.saveMenu() },
                            modifier = Modifier.padding(contentPadding)
                        )
                    }
                    composable(NavMenuScreen.MenuItemList.name) {
                        val state by viewModel.menuItemListState.collectAsState()
                        MenuItemListScreen(
                            state = state,
                            onEditItemClick = { viewModel.navigateToEditMenuItem(it) },
                            onMoveItemUp = { viewModel.moveMenuItemUp(it) },
                            onMoveItemDown = { viewModel.moveMenuItemDown(it) },
                            onLoadMore = { viewModel.loadMoreMenuItems() },
                            onFabVisibilityChange = { isFabVisible = it },
                            modifier = Modifier.padding(contentPadding)
                        )
                    }
                    composable(NavMenuScreen.MenuItemDetail.name) {
                        val state by viewModel.menuItemDetailState.collectAsState()
                        MenuItemDetailScreen(
                            state = state,
                            onTitleChange = { viewModel.updateMenuItemTitle(it) },
                            onUrlChange = { viewModel.updateMenuItemUrl(it) },
                            onParentChange = { viewModel.updateMenuItemParent(it) },
                            onDescriptionChange = { viewModel.updateMenuItemDescription(it) },
                            onTypeChange = { viewModel.updateMenuItemType(it) },
                            onLinkableItemChange = { viewModel.updateSelectedLinkableItem(it) },
                            onLoadMoreLinkableItems = { viewModel.loadMoreLinkableItems() },
                            onSaveClick = { viewModel.saveMenuItem() },
                            modifier = Modifier.padding(contentPadding)
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NavMenusTopBar(currentRoute: String?) {
        val menuDetailState by viewModel.menuDetailState.collectAsState()
        val menuItemDetailState by viewModel.menuItemDetailState.collectAsState()

        TopAppBar(
            title = { Text(getScreenTitle(currentRoute)) },
            navigationIcon = {
                IconButton(onClick = {
                    if (navController.previousBackStackEntry != null) {
                        viewModel.navigateBack()
                    } else {
                        finish()
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                if (currentRoute == NavMenuScreen.MenuDetail.name &&
                    menuDetailState?.isNew == false
                ) {
                    IconButton(
                        onClick = {
                            showDeleteConfirmation(
                                titleRes = R.string.delete_menu_confirmation_title,
                                messageRes = R.string.delete_menu_confirmation_message,
                                itemName = menuDetailState?.name ?: "",
                                onConfirm = { viewModel.deleteMenu() }
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
                if (currentRoute == NavMenuScreen.MenuItemDetail.name &&
                    menuItemDetailState?.isNew == false
                ) {
                    IconButton(
                        onClick = {
                            showDeleteConfirmation(
                                titleRes = R.string.delete_menu_item_confirmation_title,
                                messageRes = R.string.delete_menu_item_confirmation_message,
                                itemName = menuItemDetailState?.title ?: "",
                                onConfirm = { viewModel.deleteMenuItem() }
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun getScreenTitle(currentRoute: String?): String {
        return when (currentRoute) {
            NavMenuScreen.MenuList.name -> stringResource(R.string.menus)
            NavMenuScreen.MenuDetail.name -> {
                val state = viewModel.menuDetailState.collectAsState().value
                if (state?.isNew == true) {
                    stringResource(R.string.create_menu)
                } else {
                    stringResource(R.string.edit_menu)
                }
            }
            NavMenuScreen.MenuItemList.name -> {
                val menuName = viewModel.menuItemListState.collectAsState().value.menuName
                stringResource(R.string.menu_items_title, menuName)
            }
            NavMenuScreen.MenuItemDetail.name -> {
                val state = viewModel.menuItemDetailState.collectAsState().value
                if (state?.isNew == true) {
                    stringResource(R.string.add_menu_item)
                } else {
                    stringResource(R.string.edit_menu_item)
                }
            }
            else -> stringResource(R.string.menus)
        }
    }

    @Composable
    private fun NavMenusFab(currentRoute: String?, isVisible: Boolean) {
        AnimatedVisibility(
            visible = isVisible && (
                currentRoute == NavMenuScreen.MenuList.name ||
                    currentRoute == NavMenuScreen.MenuItemList.name
                ),
            enter = slideInVertically { it * 2 },
            exit = slideOutVertically { it * 2 }
        ) {
            when (currentRoute) {
                NavMenuScreen.MenuList.name -> {
                    FloatingActionButton(
                        onClick = { viewModel.navigateToCreateMenu() }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(
                                R.string.create_menu
                            )
                        )
                    }
                }
                NavMenuScreen.MenuItemList.name -> {
                    FloatingActionButton(
                        onClick = { viewModel.navigateToCreateMenuItem() }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(
                                R.string.add_menu_item
                            )
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private fun showDeleteConfirmation(
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        itemName: String,
        onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(titleRes)
            .setMessage(getString(messageRes, itemName))
            .setPositiveButton(R.string.delete) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, NavMenusActivity::class.java)
        }
    }
}
