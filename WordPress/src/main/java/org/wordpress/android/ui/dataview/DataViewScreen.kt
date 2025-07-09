package org.wordpress.android.ui.dataview

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.EmptyContentM3
import org.wordpress.android.ui.dataview.DummyDataViewItems.getDummyDataViewItems
import uniffi.wp_api.WpApiParamOrder
import java.util.Locale

/**
 * Provides a basic screen for displaying a list of [DataViewItem]s
 * which includes search and filter functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataViewScreen(
    uiState: State<DataViewUiState>,
    items: State<List<DataViewItem>>,
    supportedFilters: List<DataViewDropdownItem>,
    currentFilter: DataViewDropdownItem?,
    supportedSorts: List<DataViewDropdownItem>,
    currentSort: DataViewDropdownItem?,
    currentSortOrder: WpApiParamOrder,
    onSearchQueryChange: (String) -> Unit,
    onItemClick: (DataViewItem) -> Unit,
    onFilterClick: (DataViewDropdownItem) -> Unit,
    onSortClick: (DataViewDropdownItem) -> Unit,
    onSortOrderClick: (WpApiParamOrder) -> Unit,
    onRefresh: () -> Unit,
    onFetchMore: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    refreshState: State<Boolean>
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize(),
        isRefreshing = refreshState.value,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = refreshState.value,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SearchAndFilterBar(
                onSearchQueryChange = onSearchQueryChange,
                onFilterClick = onFilterClick,
                supportedFilters = supportedFilters,
                currentFilter = currentFilter,
                onSortClick = onSortClick,
                onSortOrderClick = onSortOrderClick,
                supportedSorts = supportedSorts,
                currentSort = currentSort,
                currentSortOrder = currentSortOrder
            )

            when (uiState.value) {
                DataViewUiState.LOADING -> LoadingDataView()
                DataViewUiState.EMPTY -> EmptyDataView()
                DataViewUiState.EMPTY_SEARCH -> EmptySearchDataView()
                DataViewUiState.ERROR -> ErrorDataView(errorMessage)
                DataViewUiState.OFFLINE -> OfflineDataView()
                DataViewUiState.LOADING_MORE,
                DataViewUiState.LOADED -> LoadedDataView(
                    items = items,
                    onItemClick = onItemClick,
                    onFetchMore = onFetchMore,
                    showProgress = uiState.value == DataViewUiState.LOADING_MORE
                )
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: (DataViewDropdownItem) -> Unit,
    currentFilter: DataViewDropdownItem? = null,
    supportedFilters: List<DataViewDropdownItem>,
    onSortClick: (DataViewDropdownItem) -> Unit,
    onSortOrderClick: (WpApiParamOrder) -> Unit,
    currentSort: DataViewDropdownItem? = null,
    currentSortOrder: WpApiParamOrder,
    supportedSorts: List<DataViewDropdownItem>,
) {
    var searchQuery by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearchQueryChange(it)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        onSearchQueryChange("")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear),
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                onSearchQueryChange("")
                            }
                        )
                    }
                }
            },
            singleLine = true
        )

        // Filter Button
        if (supportedFilters.isNotEmpty()) {
            FilterDropdownMenuButton(
                filters = supportedFilters,
                currentFilter = currentFilter,
                onFilterClick = { item ->
                    onFilterClick(item)
                }
            )
        }

        // Sort by button
        if (supportedSorts.isNotEmpty()) {
            SortDropdownMenuButton(
                sorts = supportedSorts,
                currentSort = currentSort,
                onSortClick = onSortClick,
                currentSortOrder = currentSortOrder,
                onSortOrderClick = onSortOrderClick
            )
        }
    }
}

/**
 * Dropdown menu button for displaying a list of filter [DataViewDropdownItem]s
 */
@Composable
private fun FilterDropdownMenuButton(
    filters: List<DataViewDropdownItem>,
    currentFilter: DataViewDropdownItem?,
    onFilterClick: (DataViewDropdownItem) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            menuExpanded = !menuExpanded
        },
        modifier = Modifier
            .size(48.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_filter_list_white_24dp),
            contentDescription = stringResource(id = R.string.filter),
            tint = MaterialTheme.colorScheme.primary
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
            }
        ) {
            DropdownItems(
                titleRes = R.string.filter,
                items = filters,
                currentItem = currentFilter,
                onItemClick = { item ->
                    onFilterClick(item)
                    menuExpanded = false
                }
            )
        }
    }
}

/**
 * Dropdown menu button for displaying a list of sort [DataViewDropdownItem]s along with sort order items
 */
@Composable
private fun SortDropdownMenuButton(
    sorts: List<DataViewDropdownItem>,
    currentSort: DataViewDropdownItem?,
    onSortClick: (DataViewDropdownItem) -> Unit,
    currentSortOrder: WpApiParamOrder,
    onSortOrderClick: (WpApiParamOrder) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            menuExpanded = !menuExpanded
        },
        modifier = Modifier
            .size(48.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_sort_24dp),
            contentDescription = stringResource(id = R.string.sort_by),
            tint = MaterialTheme.colorScheme.primary
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
            }
        ) {
            DropdownItems(
                titleRes = R.string.sort_by,
                items = sorts,
                currentItem = currentSort,
                onItemClick = { item ->
                    onSortClick(item)
                    menuExpanded = false
                }
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.ascending)) },
                trailingIcon = {
                    if (currentSortOrder == WpApiParamOrder.ASC) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                    }
                },
                onClick = {
                    onSortOrderClick(WpApiParamOrder.ASC)
                    menuExpanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.descending)) },
                trailingIcon = {
                    if (currentSortOrder == WpApiParamOrder.DESC) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                    }
                },
                onClick = {
                    onSortOrderClick(WpApiParamOrder.DESC)
                    menuExpanded = false
                }
            )
        }
    }
}

@Composable
private fun DropdownItems(
    @StringRes titleRes: Int,
    items: List<DataViewDropdownItem>,
    currentItem: DataViewDropdownItem?,
    onItemClick: (DataViewDropdownItem) -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(titleRes).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        enabled = false,
        onClick = { }
    )
    items.forEach { item ->
        DropdownMenuItem(
            text = { Text(stringResource(item.titleRes)) },
            trailingIcon = {
                if (item == currentItem) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                }
            },
            onClick = {
                onItemClick(item)
            }
        )
    }
}

@Composable
private fun LoadedDataView(
    items: State<List<DataViewItem>>,
    onItemClick: (DataViewItem) -> Unit,
    onFetchMore: () -> Unit,
    showProgress: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items.value) { item ->
                DataViewItemCard(
                    item = item,
                    onItemClick = {
                        onItemClick(item)
                    }
                )
                if (items.value.last() == item) {
                    onFetchMore()
                }
            }
        }
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun LoadingDataView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
        )
    }
}

@Composable
private fun EmptyDataView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContentM3(
            title = stringResource(R.string.subscribers_empty),
            image = R.drawable.img_jetpack_empty_state,
            imageContentDescription = stringResource(R.string.subscribers_empty),
        )
    }
}

@Composable
private fun EmptySearchDataView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContentM3(
            title = stringResource(R.string.subscribers_empty_search),
            image = R.drawable.img_illustration_empty_results_216dp,
            imageContentDescription = stringResource(R.string.subscribers_empty_search),
        )
    }
}

@Composable
private fun ErrorDataView(errorMessage: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContentM3(
            title = stringResource(R.string.subscribers_error_title),
            subtitle = errorMessage ?: stringResource(R.string.error_generic_network),
            image = R.drawable.img_illustration_cloud_off_152dp,
            imageContentDescription = stringResource(R.string.subscribers_error_title),
        )
    }
}

@Composable
private fun OfflineDataView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContentM3(
            title = stringResource(R.string.no_network_title),
            image = R.drawable.img_illustration_cloud_off_152dp,
            imageContentDescription = stringResource(R.string.no_network_title)
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadedPreview() {
    DataViewScreen(
        uiState = remember { mutableStateOf(DataViewUiState.LOADED) },
        items = remember { mutableStateOf(getDummyDataViewItems()) },
        supportedFilters = dummyDropdownItems,
        currentFilter = null,
        supportedSorts = dummyDropdownItems,
        currentSort = null,
        currentSortOrder = WpApiParamOrder.ASC,
        onRefresh = { },
        onFetchMore = { },
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onSortClick = { },
        onSortOrderClick = { },
        refreshState = remember { mutableStateOf(false) }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingPreview() {
    DataViewScreen(
        uiState = remember { mutableStateOf(DataViewUiState.LOADING) },
        items = remember { mutableStateOf(emptyList()) },
        supportedFilters = dummyDropdownItems,
        currentFilter = null,
        supportedSorts = dummyDropdownItems,
        currentSort = null,
        currentSortOrder = WpApiParamOrder.ASC,
        onRefresh = { },
        onFetchMore = { },
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onSortClick = { },
        onSortOrderClick = { },
        refreshState = remember { mutableStateOf(false) }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyPreview() {
    DataViewScreen(
        uiState = remember { mutableStateOf(DataViewUiState.EMPTY) },
        items = remember { mutableStateOf(emptyList()) },
        supportedFilters = dummyDropdownItems,
        currentFilter = null,
        supportedSorts = dummyDropdownItems,
        currentSort = null,
        currentSortOrder = WpApiParamOrder.ASC,
        onRefresh = { },
        onFetchMore = { },
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onSortClick = { },
        onSortOrderClick = { },
        refreshState = remember { mutableStateOf(false) }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptySearchPreview() {
    DataViewScreen(
        uiState = remember { mutableStateOf(DataViewUiState.EMPTY_SEARCH) },
        items = remember { mutableStateOf(emptyList()) },
        supportedFilters = dummyDropdownItems,
        currentFilter = null,
        supportedSorts = dummyDropdownItems,
        currentSort = null,
        currentSortOrder = WpApiParamOrder.ASC,
        onRefresh = { },
        onFetchMore = { },
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onSortClick = { },
        onSortOrderClick = { },
        refreshState = remember { mutableStateOf(false) }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfflinePreview() {
    DataViewScreen(
        uiState = remember { mutableStateOf(DataViewUiState.OFFLINE) },
        items = remember { mutableStateOf(emptyList()) },
        supportedFilters = dummyDropdownItems,
        currentFilter = null,
        supportedSorts = dummyDropdownItems,
        currentSort = null,
        currentSortOrder = WpApiParamOrder.ASC,
        onRefresh = { },
        onFetchMore = { },
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onSortClick = { },
        onSortOrderClick = { },
        refreshState = remember { mutableStateOf(false) }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorPreview() {
    DataViewScreen(
        uiState = remember { mutableStateOf(DataViewUiState.ERROR) },
        items = remember { mutableStateOf(emptyList()) },
        supportedFilters = dummyDropdownItems,
        currentFilter = null,
        supportedSorts = dummyDropdownItems,
        currentSort = null,
        currentSortOrder = WpApiParamOrder.ASC,
        onRefresh = { },
        onFetchMore = { },
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onSortClick = { },
        onSortOrderClick = { },
        refreshState = remember { mutableStateOf(false) }
    )
}

private val dummyDropdownItems = listOf(
    DataViewDropdownItem(
        id = 0L,
        titleRes = R.string.filter
    ),
)
