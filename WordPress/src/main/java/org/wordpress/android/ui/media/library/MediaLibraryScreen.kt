package org.wordpress.android.ui.media.library

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.FeatureTip
import org.wordpress.android.ui.compose.components.FeatureTipPosition
import org.wordpress.android.ui.compose.components.FilterChipTabRow
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.rs.contentlist.ContentListConfirmationDialog
import org.wordpress.android.ui.rs.contentlist.ContentListDefaults
import org.wordpress.android.ui.rs.contentlist.ContentListMessageColumn
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.ContentListSearchClearButton
import org.wordpress.android.ui.rs.contentlist.ContentListSearchField
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem

private enum class TopBarMode { SELECTION, SEARCH, NORMAL }

@Composable
fun MediaLibraryScreen(
    state: MediaLibraryUiState,
    actions: MediaLibraryActions,
    onNavigateBack: () -> Unit,
    onAddMediaOptionClick: (AddMediaOption) -> Unit
) {
    val topBarMode = when {
        state.isSelectionActive -> TopBarMode.SELECTION
        state.isSearchActive -> TopBarMode.SEARCH
        else -> TopBarMode.NORMAL
    }

    // the keyboard comes up once per search open, not each time the search bar returns after a selection
    var searchFocusPending by rememberSaveable { mutableStateOf(false) }

    // the first back press ends the selection or search, the next one leaves the screen
    BackHandler(enabled = state.isSelectionActive) { actions.onClearSelection() }
    BackHandler(enabled = state.isSearchActive && !state.isSelectionActive) { actions.onSearchClose() }

    Scaffold(
        containerColor = ContentListDefaults.containerColor(),
        topBar = {
            AnimatedContent(targetState = topBarMode, label = "topBar") { mode ->
                when (mode) {
                    TopBarMode.SELECTION -> SelectionTopBar(
                        selectedCount = state.selectedIds.size,
                        onClearSelection = actions::onClearSelection,
                        onDeleteClick = actions::onDeleteSelectedClick
                    )
                    TopBarMode.SEARCH -> SearchTopBar(
                        query = state.searchQuery,
                        requestFocus = searchFocusPending,
                        onFocusRequested = { searchFocusPending = false },
                        onQueryChanged = actions::onSearchQueryChanged,
                        onClose = actions::onSearchClose
                    )
                    TopBarMode.NORMAL -> MainTopBar(
                        canUpload = state.canUpload,
                        addMediaOptions = state.addMediaOptions,
                        onNavigateBack = onNavigateBack,
                        onSearchClick = {
                            searchFocusPending = true
                            actions.onSearchOpen()
                        },
                        onAddMediaOptionClick = onAddMediaOptionClick
                    )
                }
            }
        },
        bottomBar = {
            val quota = state.quota
            if (quota != null && !state.isSearchActive) {
                QuotaBar(quota)
            }
        }
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (!state.isSearchActive) {
                // disabled rather than hidden during a selection, so the grid doesn't jump
                FilterChipTabRow(
                    labels = state.filters.map { stringResource(it.labelResId) },
                    selectedIndex = state.filters.indexOf(state.filter),
                    onSelect = { index -> actions.onFilterSelected(state.filters[index]) },
                    enabled = !state.isSelectionActive
                )
            }
            ContentListPullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = actions::onRefresh,
                enabled = !state.isSelectionActive,
                modifier = Modifier.weight(1f)
            ) {
                if (state.items.isEmpty()) {
                    MediaEmptyContent(state, onAddMediaOptionClick = onAddMediaOptionClick)
                } else {
                    // a fresh grid (and scroll position) for each filter or search, keyed on what the items were
                    // loaded for: keyed on the requested filter, the old list would still be showing and the grid
                    // would keep its top item anchored once the new list arrived
                    key(state.itemsFilter, state.itemsQuery) {
                        MediaGrid(state, actions)
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirmation) {
        val count = state.selectedIds.size
        // GlotPress can't translate quantity strings, so the two forms are separate strings
        val messageResId = if (count == 1) R.string.confirm_delete_single_media else R.string.confirm_delete_multi_media
        ContentListConfirmationDialog(
            titleResId = R.string.delete,
            message = stringResource(messageResId, count),
            onConfirm = actions::onConfirmDelete,
            onDismiss = actions::onDismissDelete,
            isDestructive = true
        )
    }
}

@Composable
private fun MediaGrid(state: MediaLibraryUiState, actions: MediaLibraryActions) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 4 else 3
    val thumbnailWidthPx = LocalWindowInfo.current.containerSize.width / columns
    val gridState = rememberLazyGridState()

    LoadMoreOnScrollToEnd(
        gridState = gridState,
        itemCount = state.items.size,
        // not keyed on isLoadingMore: each landed page would restart the trigger before the new items arrive and
        // ask for another page; the view model already ignores a load-more while one is running
        canLoadMore = state.canLoadMore,
        onLoadMore = actions::onLoadMore
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(GRID_PADDING),
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.items, key = { it.localId }) { item ->
            val index = state.selectedIds.indexOf(item.localId)
            MediaGridTile(
                item = item,
                thumbnailSource = state.thumbnailSource,
                thumbnailWidthPx = thumbnailWidthPx,
                selectionIndex = if (index >= 0) index + 1 else null,
                showSelectionBadge = state.isSelectionActive && item.canSelect,
                canLoadVideoFrame = actions::canLoadVideoFrame,
                onClick = { actions.onItemClick(item.localId) },
                onLongClick = { actions.onItemLongClick(item.localId) },
                onRetryClick = { actions.onItemRetryClick(item.localId) },
                onTrashClick = { actions.onItemTrashClick(item.localId) },
                modifier = Modifier.animateItem()
            )
        }
        if (state.isLoadingMore) {
            contentListLoadingMoreItem()
        }
    }
}

/** Scrollable so pull-to-refresh still works on an empty grid */
@Composable
private fun MediaEmptyContent(state: MediaLibraryUiState, onAddMediaOptionClick: (AddMediaOption) -> Unit) {
    val isSearching = state.searchQuery.isNotEmpty()
    val titleResId = when (state.emptyState) {
        MediaEmptyState.LOADING -> R.string.media_fetching
        MediaEmptyState.NO_CONTENT ->
            if (isSearching) R.string.media_empty_search_list else state.filter.emptyTitleResId
        MediaEmptyState.NETWORK_ERROR -> R.string.no_network_message
        MediaEmptyState.PERMISSION_ERROR -> R.string.media_error_no_permission
        MediaEmptyState.GENERIC_ERROR -> R.string.error_refresh_media
    }
    val showUploadPrompt = state.emptyState == MediaEmptyState.NO_CONTENT && !state.isSearchActive
    ContentListMessageColumn {
        Text(
            text = stringResource(titleResId),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (showUploadPrompt) {
            Spacer(modifier = Modifier.height(TEXT_GAP))
            Text(
                text = stringResource(R.string.media_empty_list_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (state.canUpload && !state.isSearchActive && state.emptyState != MediaEmptyState.LOADING) {
            Spacer(modifier = Modifier.height(BUTTON_GAP))
            AddMediaMenu(state.addMediaOptions, onAddMediaOptionClick) { onClick ->
                Button(onClick = onClick) {
                    Text(text = stringResource(R.string.media_empty_upload_media))
                }
            }
        }
    }
}

@Composable
private fun QuotaBar(quota: MediaQuotaUiState) {
    Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider()
        Text(
            text = quota.text,
            style = MaterialTheme.typography.bodySmall,
            color = if (quota.isNearLimit) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(QUOTA_PADDING)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    canUpload: Boolean,
    addMediaOptions: List<AddMediaOption>,
    onNavigateBack: () -> Unit,
    onSearchClick: () -> Unit,
    onAddMediaOptionClick: (AddMediaOption) -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(R.string.wp_media_title)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            // TODO: Temporary FeatureTip on the search icon
            FeatureTip(
                prefKey = AppPrefs.UndeletablePrefKey.FEATURE_TIP_MEDIA_REDESIGN_SHOWN,
                text = "Search your media library",
                anchorPosition = FeatureTipPosition.Below,
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                }
            }
            if (canUpload) {
                AddMediaMenu(addMediaOptions, onAddMediaOptionClick) { onClick ->
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_media))
                    }
                }
            }
        }
    )
}

/** The add-media options as a dropdown anchored to [anchor], which gets the click that opens it */
@Composable
private fun AddMediaMenu(
    options: List<AddMediaOption>,
    onOptionClick: (AddMediaOption) -> Unit,
    anchor: @Composable (onClick: () -> Unit) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        anchor { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelResId)) },
                    onClick = {
                        expanded = false
                        onOptionClick(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(selectedCount: Int, onClearSelection: () -> Unit, onDeleteClick: () -> Unit) {
    TopAppBar(
        // a distinct container color so selection mode visibly reads as a mode change
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        title = { Text(text = stringResource(R.string.cab_selected, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
            }
        },
        actions = {
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        title = {
            ContentListSearchField(
                query = query,
                onQueryChange = onQueryChanged,
                placeholderResId = R.string.search,
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            ContentListSearchClearButton(query) { onQueryChanged("") }
        }
    )
    // runs after the bar is composed, so the requester is attached
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }
}

private val GRID_PADDING = 2.dp
private val TEXT_GAP = 8.dp
private val BUTTON_GAP = 16.dp
private val QUOTA_PADDING = 8.dp
