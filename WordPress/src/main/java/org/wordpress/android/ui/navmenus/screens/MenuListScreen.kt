package org.wordpress.android.ui.navmenus.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.navmenus.MenuListUiState
import org.wordpress.android.ui.navmenus.MenuUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuListScreen(
    state: MenuListUiState,
    onEditMenuClick: (Long) -> Unit,
    onMenuItemsClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onFabVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            state.menus.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.no_menus),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            else -> {
                val listState = rememberLazyListState()

                ObserveLoadMore(
                    listState = listState,
                    itemCount = state.menus.size,
                    canLoadMore = state.canLoadMore,
                    isLoadingMore = state.isLoadingMore,
                    onLoadMore = onLoadMore
                )
                ObserveScrollDirectionForFab(listState, onFabVisibilityChange)

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(state.menus) { menu ->
                        MenuListItem(
                            menu = menu,
                            onEditClick = { onEditMenuClick(menu.id) },
                            onItemsClick = { onMenuItemsClick(menu.id) }
                        )
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = menu.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (menu.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = menu.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (menu.locations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.menu_locations_label, menu.locations.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onItemsClick,
                colors = ButtonDefaults.textButtonColors()
            ) {
                Text(stringResource(R.string.edit_items))
            }
        }
    }
}

// region Previews

private val sampleMenus = listOf(
    MenuUiModel(id = 1L, name = "Main Menu", description = "Primary navigation menu", locations = listOf("primary")),
    MenuUiModel(id = 2L, name = "Footer Menu", description = "Links in the footer", locations = listOf("footer")),
    MenuUiModel(id = 3L, name = "Social Menu", description = "", locations = emptyList())
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MenuListScreenPreview() {
    AppThemeM3 {
        MenuListScreen(
            state = MenuListUiState(menus = sampleMenus),
            onEditMenuClick = {},
            onMenuItemsClick = {},
            onRefresh = {},
            onLoadMore = {},
            onFabVisibilityChange = {}
        )
    }
}

@Preview(name = "Loading Light", showBackground = true)
@Preview(name = "Loading Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MenuListScreenLoadingPreview() {
    AppThemeM3 {
        MenuListScreen(
            state = MenuListUiState(isLoading = true),
            onEditMenuClick = {},
            onMenuItemsClick = {},
            onRefresh = {},
            onLoadMore = {},
            onFabVisibilityChange = {}
        )
    }
}

@Preview(name = "Empty Light", showBackground = true)
@Preview(name = "Empty Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MenuListScreenEmptyPreview() {
    AppThemeM3 {
        MenuListScreen(
            state = MenuListUiState(menus = emptyList()),
            onEditMenuClick = {},
            onMenuItemsClick = {},
            onRefresh = {},
            onLoadMore = {},
            onFabVisibilityChange = {}
        )
    }
}

@Preview(name = "Error Light", showBackground = true)
@Preview(name = "Error Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MenuListScreenErrorPreview() {
    AppThemeM3 {
        MenuListScreen(
            state = MenuListUiState(error = "Failed to load menus. Please try again."),
            onEditMenuClick = {},
            onMenuItemsClick = {},
            onRefresh = {},
            onLoadMore = {},
            onFabVisibilityChange = {}
        )
    }
}

// endregion
