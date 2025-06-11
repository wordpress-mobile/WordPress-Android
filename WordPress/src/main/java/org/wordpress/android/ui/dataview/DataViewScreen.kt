package org.wordpress.android.ui.dataview

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.EmptyContentM3
import org.wordpress.android.ui.compose.components.ProgressDialog
import org.wordpress.android.ui.compose.components.ProgressDialogState
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.DummyDataViewItems.getDummyDataViewItems

/**
 * Provides a basic screen for displaying a list of [DataViewItem]s
 * which includes search and filter functionality. More complex use
 * cases may require extending DataViewItem and passing a custom
 * renderer via [onRenderItem].
 */
@Composable
fun DataViewScreen(
    title: String,
    uiState: State<DataViewUiState>,
    items: State<List<DataViewItem>>,
    filters: List<DataViewItemFilter>,
    onSearchQueryChange: (String) -> Unit,
    onItemClick: (DataViewItem) -> Unit,
    onFilterClick: (DataViewItemFilter) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRenderItem: @Composable ((DataViewItem) -> Unit)? = null,
) {
    Screen(
        title = title,
        onBackClick = onBackClick,
        content = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SearchAndFilterBar(
                    onSearchQueryChange = onSearchQueryChange,
                    onFilterClick = onFilterClick,
                    filters = filters
                )

                when (uiState.value) {
                    DataViewUiState.LOADING -> LoadingDataView()
                    DataViewUiState.EMPTY -> EmptyDataView()
                    DataViewUiState.OFFLINE -> OfflineDataView()
                    DataViewUiState.LOADED -> LoadedDataView(items, onItemClick, onRenderItem)
                }
            }
        }

    )
}

@Composable
private fun SearchAndFilterBar(
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: (DataViewItemFilter) -> Unit,
    filters: List<DataViewItemFilter>
) {
    var searchQuery by remember { mutableStateOf("") }
    var filtersExpanded by remember { mutableStateOf(false) }

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
        IconButton(
            onClick = {
                filtersExpanded = !filtersExpanded
            },
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_filter_list_white_24dp),
                contentDescription = stringResource(R.string.filter),
                tint = MaterialTheme.colorScheme.primary
            )
            DropdownMenu(
                expanded = filtersExpanded,
                onDismissRequest = {
                    filtersExpanded = false
                }
            ) {
                filters.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.title) },
                        onClick = {
                            onFilterClick(filter)
                            filtersExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedDataView(
    items: State<List<DataViewItem>>,
    onItemClick: (DataViewItem) -> Unit,
    onRenderItem: @Composable ((DataViewItem) -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items.value) { item ->
            // if onRenderItem is provided, use it, otherwise use the default DataViewItemCard
            onRenderItem?.invoke(item) ?: run {
                DataViewItemCard(
                    item = item,
                    onItemClick = {
                        onItemClick(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingDataView() {
    ProgressDialog(ProgressDialogState())
}

@Composable
private fun EmptyDataView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContentM3(
            title = stringResource(R.string.reader_empty_search_title),
            image = R.drawable.img_illustration_empty_results_216dp,
            imageContentDescription = stringResource(R.string.empty_list_default),
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
            imageContentDescription = stringResource(R.string.no_network_message)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    title: String,
    content: @Composable () -> Unit,
    onBackClick: () -> Unit
) {
    AppThemeM3 {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadedPreview() {
    DataViewScreen(
        title = "Title",
        uiState = remember { mutableStateOf(DataViewUiState.LOADED) },
        items = remember { mutableStateOf(getDummyDataViewItems()) },
        filters = emptyList(),
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onBackClick = { },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingPreview() {
    DataViewScreen(
        title = "Title",
        uiState = remember { mutableStateOf(DataViewUiState.LOADING) },
        items = remember { mutableStateOf(emptyList()) },
        filters = emptyList(),
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onBackClick = { },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyPreview() {
    DataViewScreen(
        title = "Title",
        uiState = remember { mutableStateOf(DataViewUiState.EMPTY) },
        items = remember { mutableStateOf(emptyList()) },
        filters = emptyList(),
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onBackClick = { },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfflinePreview() {
    DataViewScreen(
        title = "Title",
        uiState = remember { mutableStateOf(DataViewUiState.OFFLINE) },
        items = remember { mutableStateOf(emptyList()) },
        filters = emptyList(),
        onSearchQueryChange = { },
        onItemClick = {},
        onFilterClick = { },
        onBackClick = { },
    )
}
