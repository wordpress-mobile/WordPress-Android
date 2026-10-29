package org.wordpress.android.ui.reader.views.compose.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.htmlEncode
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.LoadingState
import org.wordpress.android.ui.reader.views.compose.ReaderSiteRow
import org.wordpress.android.ui.reader.viewmodels.ReaderSearchContent
import org.wordpress.android.ui.reader.viewmodels.ReaderSearchTab
import org.wordpress.android.ui.reader.viewmodels.ReaderSearchUiState
import org.wordpress.android.ui.rs.contentlist.ContentListSearchClearButton
import org.wordpress.android.ui.rs.contentlist.ContentListSearchField
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem
import org.wordpress.android.util.UrlUtils

/**
 * The Reader search screen. [postList] is the post list fragment, which stays composed (and hidden by
 * the caller when [isPostListVisible] is false) so its results and scroll survive tab switches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSearchScreen(
    uiState: ReaderSearchUiState,
    actions: ReaderSearchActions,
    postList: @Composable (isPostListVisible: Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    // runs once per composition, so the keyboard comes back after rotation but not on return from a post or site
    LaunchedEffect(Unit) {
        if (uiState.submittedQuery == null) {
            focusRequester.requestFocus()
        }
    }

    // hoisted so the Sites tab keeps its scroll while hidden, and keyed so a new search starts at the top
    val sitesListState = rememberSaveable(uiState.submittedQuery, saver = LazyListState.Saver) { LazyListState() }
    // re-running the same query reloads the first page, so it needs the scroll reset its key can't give it
    LaunchedEffect(uiState.isLoadingSites) {
        if (uiState.isLoadingSites) sitesListState.requestScrollToItem(0)
    }
    val scope = rememberCoroutineScope()
    val content = uiState.content
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        ContentListSearchField(
                            query = uiState.query,
                            onQueryChange = actions.onQueryChanged,
                            placeholderResId = R.string.reader_hint_post_search,
                            modifier = Modifier.focusRequester(focusRequester),
                            onSearch = actions.onSearch
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = actions.onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        ContentListSearchClearButton(uiState.query) { actions.onQueryChanged("") }
                    }
                )
                if (content == ReaderSearchContent.RESULTS) {
                    SearchTabs(uiState.selectedTab, actions.onTabSelected) { tab ->
                        // tapping the current tab jumps back to the top, as it did before
                        when (tab) {
                            ReaderSearchTab.POSTS -> actions.onPostsTabReselected()
                            ReaderSearchTab.SITES -> scope.launch { sitesListState.animateScrollToItem(0) }
                        }
                    }
                }
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding()
        ) {
            val isPostListVisible = content == ReaderSearchContent.RESULTS &&
                    uiState.selectedTab == ReaderSearchTab.POSTS
            postList(isPostListVisible)

            if (!isPostListVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (content) {
                        ReaderSearchContent.EXPLAINER ->
                            SearchMessage(title = stringResource(R.string.reader_label_post_search_explainer))
                        ReaderSearchContent.SUGGESTIONS -> Suggestions(uiState.suggestions, actions)
                        ReaderSearchContent.RESULTS -> SitesTab(uiState, sitesListState, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTabs(
    selectedTab: ReaderSearchTab,
    onTabSelected: (ReaderSearchTab) -> Unit,
    onTabReselected: (ReaderSearchTab) -> Unit
) {
    // the top bar slot isn't inset, and only the TopAppBar insets itself
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        ReaderSearchTab.entries.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { if (tab == selectedTab) onTabReselected(tab) else onTabSelected(tab) },
                text = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}

@Composable
private fun Suggestions(suggestions: List<String>, actions: ReaderSearchActions) {
    var isClearDialogVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // prefixed so a saved query can't collide with the footer's key
        items(suggestions, key = { "suggestion:$it" }) { suggestion ->
            SuggestionRow(
                suggestion = suggestion,
                onClick = {
                    // hide the keyboard, as submitting from the keyboard does
                    focusManager.clearFocus()
                    actions.onSuggestionClicked(suggestion)
                },
                onDelete = { actions.onSuggestionDeleteClicked(suggestion) }
            )
        }
        item(key = "clear_history") {
            Text(
                text = stringResource(R.string.label_clear_search_history),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isClearDialogVisible = true }
                    .padding(horizontal = 56.dp, vertical = 16.dp)
            )
        }
    }

    if (isClearDialogVisible) {
        AlertDialog(
            onDismissRequest = { isClearDialogVisible = false },
            text = { Text(stringResource(R.string.dlg_confirm_clear_search_history)) },
            confirmButton = {
                TextButton(onClick = {
                    isClearDialogVisible = false
                    actions.onClearHistoryConfirmed()
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { isClearDialogVisible = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

@Composable
private fun SuggestionRow(suggestion: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp)
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.reader_list_item_suggestion_remove_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SitesTab(uiState: ReaderSearchUiState, listState: LazyListState, actions: ReaderSearchActions) {
    when {
        uiState.isLoadingSites -> LoadingState()

        uiState.sitesRequestFailed ->
            SearchMessage(title = stringResource(R.string.reader_empty_search_request_failed))

        uiState.sites.isEmpty() -> {
            val query = uiState.submittedQuery.orEmpty().htmlEncode()
            val description = stringResource(R.string.reader_empty_search_description, "<em>$query</em>")
            SearchMessage(
                title = stringResource(R.string.reader_empty_search_title),
                description = remember(description) { AnnotatedString.fromHtml(description) }
            )
        }

        else -> SitesList(uiState, listState, actions)
    }
}

@Composable
private fun SitesList(uiState: ReaderSearchUiState, listState: LazyListState, actions: ReaderSearchActions) {
    LoadMoreOnScrollToEnd(
        listState = listState,
        itemCount = uiState.sites.size,
        canLoadMore = uiState.canRequestMoreSites,
        onLoadMore = actions.onLoadMoreSites
    )
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(uiState.sites, key = { it.key }) { site ->
            ReaderSiteRow(
                title = site.title?.takeIf { it.isNotEmpty() } ?: stringResource(R.string.untitled_in_parentheses),
                subtitle = remember(site.url) { UrlUtils.getHost(site.url) },
                iconUrl = site.iconUrl,
                isFollowing = site.isFollowing,
                isFollowEnabled = site.isFollowEnabled,
                onClick = { actions.onSiteClicked(site) },
                onFollowClick = { actions.onSiteFollowClicked(site) }
            )
            HorizontalDivider()
        }
        if (uiState.isLoadingMoreSites) {
            contentListLoadingMoreItem()
        }
    }
}

@Composable
private fun SearchMessage(title: String, description: AnnotatedString? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // scrollable so the message still fits once the keyboard shrinks the screen
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
