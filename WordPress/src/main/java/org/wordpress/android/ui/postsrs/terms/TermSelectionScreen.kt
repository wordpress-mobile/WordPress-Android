package org.wordpress.android.ui.postsrs.terms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun TermSelectionScreen(
    title: String,
    uiState: TermSelectionUiState,
    onBackClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onTermToggled: (Long) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLoadMore: () -> Unit,
    onAddTermClicked: () -> Unit,
    onAddDialogDismissed: () -> Unit,
    onAddTermConfirmed: (String, Long?) -> Unit,
    onRetry: () -> Unit,
) {
    BackHandler(onBack = onBackClicked)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            Icons.AutoMirrored.Filled
                                .ArrowBack,
                            contentDescription =
                                stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.hasChanges) {
                        IconButton(
                            onClick = onSaveClicked
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription =
                                    stringResource(
                                        R.string.save
                                    )
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.error == null) {
                FloatingActionButton(
                    onClick = onAddTermClicked,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(
                            R.string.post_rs_settings_add_term
                        )
                    )
                }
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when {
                uiState.isLoading || uiState.isCreating ->
                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                uiState.error != null ->
                    ErrorContent(
                        error = uiState.error,
                        onRetry = onRetry,
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                else ->
                    TermListContent(
                        terms = uiState.terms,
                        searchQuery = uiState.searchQuery,
                        isSearching = uiState.isSearching,
                        canLoadMore = uiState.canLoadMore,
                        isLoadingMore =
                            uiState.isLoadingMore,
                        onSearchQueryChanged =
                            onSearchQueryChanged,
                        onTermToggled = onTermToggled,
                        onLoadMore = onLoadMore,
                    )
            }
        }
    }

    if (uiState.showAddDialog) {
        AddTermDialog(
            isHierarchical = uiState.isHierarchical,
            parentOptions = uiState.parentOptions,
            onDismiss = onAddDialogDismissed,
            onConfirm = onAddTermConfirmed,
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme
                .onSurfaceVariant,
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun TermListContent(
    terms: List<SelectableTerm>,
    searchQuery: String,
    isSearching: Boolean,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onTermToggled: (Long) -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = {
                Text(
                    stringResource(
                        R.string
                            .post_rs_settings_search_terms
                    )
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
        )
        when {
            isSearching ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            terms.isEmpty() &&
                searchQuery.isNotEmpty() ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            R.string
                                .post_rs_settings_search_no_results
                        ),
                        style = MaterialTheme.typography
                            .bodyLarge,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    )
                }
            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = terms,
                        key = { it.id }
                    ) { term ->
                        TermRow(
                            term = term,
                            onToggle = {
                                onTermToggled(term.id)
                            }
                        )
                    }
                    if (canLoadMore) {
                        item(key = "load_more") {
                            LoadMoreItem(
                                isLoading =
                                    isLoadingMore,
                                onLoadMore = onLoadMore,
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun LoadMoreItem(
    isLoading: Boolean,
    onLoadMore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isLoading,
                onClick = onLoadMore
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = stringResource(
                    R.string.post_rs_settings_load_more
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TermRow(
    term: SelectableTerm,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(
                start = (16 + term.level * 24).dp,
                end = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = term.isSelected,
            onCheckedChange = { onToggle() },
        )
        Text(
            text = term.name,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTermDialog(
    isHierarchical: Boolean,
    parentOptions: List<ParentOption>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedParentId by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var dropdownExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.post_rs_settings_add_term
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            stringResource(
                                R.string
                                    .post_rs_settings_term_name_hint
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isHierarchical) {
                    val selectedName =
                        parentOptions.firstOrNull {
                            it.id == selectedParentId
                        }?.name ?: stringResource(
                            R.string
                                .post_rs_settings_no_parent
                        )
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = {
                            dropdownExpanded = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    stringResource(
                                        R.string
                                            .post_rs_settings_parent_category
                                    )
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults
                                    .TrailingIcon(
                                        expanded =
                                            dropdownExpanded
                                    )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    ExposedDropdownMenuAnchorType
                                        .PrimaryNotEditable,
                                    enabled = true
                                )
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = {
                                dropdownExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            R.string
                                                .post_rs_settings_no_parent
                                        )
                                    )
                                },
                                onClick = {
                                    selectedParentId = 0L
                                    dropdownExpanded = false
                                }
                            )
                            parentOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(opt.name)
                                    },
                                    onClick = {
                                        selectedParentId =
                                            opt.id
                                        dropdownExpanded =
                                            false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parent = if (
                        isHierarchical &&
                        selectedParentId > 0
                    ) {
                        selectedParentId
                    } else {
                        null
                    }
                    onConfirm(name.trim(), parent)
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
