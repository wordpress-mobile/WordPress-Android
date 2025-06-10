package org.wordpress.android.ui.dataview

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun DataViewScreen(
    items: List<String>,
    @StringRes titleRes: Int,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by remember { mutableStateOf("") }

    val content = @Composable {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onFilterClick = onFilterClick
            )

            // List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    Screen(
        titleRes = titleRes,
        content = content,
        onCloseClick = {}
    )
}

@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
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
                onSearchQueryChange(it)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            placeholder = { Text("Search...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        onSearchQueryChange("")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true
        )

        // Filter Button
        IconButton(
            onClick = onFilterClick,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    @StringRes titleRes: Int,
    content: @Composable () -> Unit,
    onCloseClick: () -> Unit
) {
    AppThemeM3 {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = titleRes)) },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
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
fun SearchableListScreenPreview() {
    DataViewScreen(
        titleRes = R.string.app_name,
        items = listOf("Item 1", "Item 2", "Item 3"),
        onSearchQueryChange = {},
        onFilterClick = {}
    )
}
