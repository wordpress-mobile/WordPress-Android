package org.wordpress.android.ui.navmenus

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
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

        lifecycleScope.launch {
            viewModel.uiEvent.filterNotNull().collect { event ->
                when (event) {
                    is NavMenusUiEvent.ShowError -> {
                        ToastUtils.showToast(this@NavMenusActivity, event.message, ToastUtils.Duration.LONG)
                    }
                    is NavMenusUiEvent.ShowMessage -> {
                        ToastUtils.showToast(this@NavMenusActivity, event.message)
                    }
                    is NavMenusUiEvent.MenuSaved -> {
                        ToastUtils.showToast(this@NavMenusActivity, R.string.menu_saved)
                    }
                    is NavMenusUiEvent.MenuDeleted -> {
                        ToastUtils.showToast(this@NavMenusActivity, R.string.menu_deleted)
                    }
                    is NavMenusUiEvent.MenuItemSaved -> {
                        ToastUtils.showToast(this@NavMenusActivity, R.string.menu_item_saved)
                    }
                    is NavMenusUiEvent.MenuItemDeleted -> {
                        ToastUtils.showToast(this@NavMenusActivity, R.string.menu_item_deleted)
                    }
                    NavMenusUiEvent.NavigateBack -> {
                        // Handled by ViewModel
                    }
                }
                viewModel.consumeUiEvent()
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

        AppThemeM3 {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when (currentRoute) {
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
                                        viewModel.menuItemListState.collectAsState().value.menuName
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
                            )
                        },
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
                        }
                    )
                },
                floatingActionButton = {
                    when (currentRoute) {
                        NavMenuScreen.MenuList.name -> {
                            FloatingActionButton(onClick = { viewModel.navigateToCreateMenu() }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_menu))
                            }
                        }
                        NavMenuScreen.MenuItemList.name -> {
                            FloatingActionButton(onClick = { viewModel.navigateToCreateMenuItem() }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_menu_item))
                            }
                        }
                        else -> {}
                    }
                }
            ) { contentPadding ->
                NavHost(
                    navController = navController,
                    startDestination = NavMenuScreen.MenuList.name
                ) {
                    composable(NavMenuScreen.MenuList.name) {
                        MenuListScreen(modifier = Modifier.padding(contentPadding))
                    }
                    composable(NavMenuScreen.MenuDetail.name) {
                        MenuDetailScreen(modifier = Modifier.padding(contentPadding))
                    }
                    composable(NavMenuScreen.MenuItemList.name) {
                        MenuItemListScreen(modifier = Modifier.padding(contentPadding))
                    }
                    composable(NavMenuScreen.MenuItemDetail.name) {
                        MenuItemDetailScreen(modifier = Modifier.padding(contentPadding))
                    }
                }
            }
        }
    }

    @Composable
    private fun MenuListScreen(modifier: Modifier) {
        val state by viewModel.menuListState.collectAsState()

        Box(modifier = modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.menus.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_menus),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(state.menus) { menu ->
                            MenuListItem(
                                menu = menu,
                                onEditClick = { viewModel.navigateToEditMenu(menu.id) },
                                onItemsClick = { viewModel.navigateToMenuItems(menu.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MenuListItem(
        menu: MenuUiModel,
        onEditClick: () -> Unit,
        onItemsClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditClick() }
                    .padding(16.dp)
            ) {
                Text(
                    text = menu.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (menu.locations.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.menu_locations_label, menu.locations.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onItemsClick,
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Text(stringResource(R.string.edit_items))
                    }
                }
            }
        }
    }

    @Composable
    private fun MenuDetailScreen(modifier: Modifier) {
        val state by viewModel.menuDetailState.collectAsState()
        val currentState = state ?: return

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = currentState.name,
                        onValueChange = { viewModel.updateMenuName(it) },
                        label = { Text(stringResource(R.string.menu_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currentState.description,
                        onValueChange = { viewModel.updateMenuDescription(it) },
                        label = { Text(stringResource(R.string.menu_description_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.menu_auto_add_pages))
                        Switch(
                            checked = currentState.autoAdd,
                            onCheckedChange = { viewModel.updateMenuAutoAdd(it) }
                        )
                    }
                }
            }

            if (currentState.availableLocations.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.menu_display_locations),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        currentState.availableLocations.forEach { location ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleMenuLocation(location.name) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = location.name in currentState.selectedLocations,
                                    onCheckedChange = { viewModel.toggleMenuLocation(location.name) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = location.name)
                                    Text(
                                        text = location.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.saveMenu() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !currentState.isSaving && !currentState.isDeleting
            ) {
                if (currentState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }

            if (!currentState.isNew) {
                Button(
                    onClick = { showDeleteMenuConfirmation(currentState.name) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    enabled = !currentState.isSaving && !currentState.isDeleting
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    if (currentState.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }

    @Composable
    private fun MenuItemListScreen(modifier: Modifier) {
        val state by viewModel.menuItemListState.collectAsState()

        Box(modifier = modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.items.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_menu_items),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items) { item ->
                            MenuItemListItem(
                                item = item,
                                onEditClick = { viewModel.navigateToEditMenuItem(item.id) },
                                onMoveUp = { viewModel.moveMenuItemUp(item.id) },
                                onMoveDown = { viewModel.moveMenuItemDown(item.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MenuItemListItem(
        item: MenuItemUiModel,
        onEditClick: () -> Unit,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditClick() }
                .padding(
                    start = (16 + item.indentLevel * 24).dp,
                    end = 8.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifEmpty { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMoveUp) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
            }
            IconButton(onClick = onMoveDown) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MenuItemDetailScreen(modifier: Modifier) {
        val state by viewModel.menuItemDetailState.collectAsState()
        val currentState = state ?: return

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = currentState.title,
                        onValueChange = { viewModel.updateMenuItemTitle(it) },
                        label = { Text(stringResource(R.string.menu_item_title_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (currentState.type == "custom") {
                        OutlinedTextField(
                            value = currentState.url,
                            onValueChange = { viewModel.updateMenuItemUrl(it) },
                            label = { Text(stringResource(R.string.menu_item_url_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (currentState.availableParents.isNotEmpty()) {
                        ParentDropdown(
                            selectedParentId = currentState.parentId,
                            availableParents = currentState.availableParents,
                            onParentSelected = { viewModel.updateMenuItemParent(it) }
                        )
                    }

                    OutlinedTextField(
                        value = currentState.target,
                        onValueChange = { viewModel.updateMenuItemTarget(it) },
                        label = { Text(stringResource(R.string.menu_item_target_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("_blank") }
                    )

                    OutlinedTextField(
                        value = currentState.cssClasses,
                        onValueChange = { viewModel.updateMenuItemCssClasses(it) },
                        label = { Text(stringResource(R.string.menu_item_css_classes_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currentState.description,
                        onValueChange = { viewModel.updateMenuItemDescription(it) },
                        label = { Text(stringResource(R.string.menu_item_description_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = currentState.attrTitle,
                        onValueChange = { viewModel.updateMenuItemAttrTitle(it) },
                        label = { Text(stringResource(R.string.menu_item_attr_title_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Button(
                onClick = { viewModel.saveMenuItem() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !currentState.isSaving && !currentState.isDeleting
            ) {
                if (currentState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }

            if (!currentState.isNew) {
                Button(
                    onClick = { showDeleteMenuItemConfirmation(currentState.title) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    enabled = !currentState.isSaving && !currentState.isDeleting
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    if (currentState.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ParentDropdown(
        selectedParentId: Long,
        availableParents: List<ParentItemOption>,
        onParentSelected: (Long) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        val selectedName = availableParents.find { it.id == selectedParentId }?.title
            ?: stringResource(R.string.menu_item_no_parent)

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.menu_item_parent_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_item_no_parent)) },
                        onClick = {
                            onParentSelected(0L)
                            expanded = false
                        }
                    )
                    availableParents.forEach { parent ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "  ".repeat(parent.indentLevel) + parent.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onParentSelected(parent.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showDeleteMenuConfirmation(menuName: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_menu_confirmation_title)
            .setMessage(getString(R.string.delete_menu_confirmation_message, menuName))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteMenu()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteMenuItemConfirmation(itemTitle: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_menu_item_confirmation_title)
            .setMessage(getString(R.string.delete_menu_item_confirmation_message, itemTitle))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteMenuItem()
            }
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
