package org.wordpress.android.ui.dataview

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

/**
 * Provides a basic screen for displaying a list of [DataViewItem]s
 * which includes search and filter functionality. More complex use
 * cases may require extending DataViewItem and passing a custom renderer
 * via [onRenderItem]
 */
@Composable
fun DataViewScreen(
    @StringRes titleRes: Int,
    items: List<DataViewItem>,
    filters: List<DataViewFilterItem>,
    onSearchQueryChange: (String) -> Unit,
    onItemClick: (DataViewItem) -> Unit,
    onFilterClick: (DataViewFilterItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRenderItem: @Composable ((DataViewItem) -> Unit)? = null,
) {
    val content = @Composable {
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

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items) { item ->
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
    }

    Screen(
        titleRes = titleRes,
        content = content,
        onBackClick = onBackClick
    )
}

@Composable
private fun SearchAndFilterBar(
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: (DataViewFilterItem) -> Unit,
    filters: List<DataViewFilterItem>
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
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    @StringRes titleRes: Int,
    content: @Composable () -> Unit,
    onBackClick: () -> Unit
) {
    AppThemeM3 {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = titleRes)) },
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

@Preview(showBackground = true)
@Composable
fun DataViewScreenPreview() {
    val items = listOf(
        DummyDataViewItem,
        DummyDataViewItem,
        DummyDataViewItem,
        DummyDataViewItem,
        DummyDataViewItem,
        DummyDataViewItem,
    )
    DataViewScreen(
        titleRes = R.string.app_name,
        items = items,
        onRenderItem = { item ->
            DataViewItemCard(
                item = item,
                onItemClick = {}
            )
        },
        onSearchQueryChange = {},
        onItemClick = {},
        onFilterClick = {},
        onBackClick = {},
        filters = emptyList()
    )
}

// TODO: remove this when actual data is available
val DummyDataViewItem = DataViewItem(
    id = 1L,
    title = "Title",
    subtitle = "Subtitle",
    dateLine = "2023-01-01",
)
