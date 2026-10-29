package org.wordpress.android.ui.reader.views.compose.subs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.EmptyContentM3
import org.wordpress.android.ui.compose.components.LoadingState
import org.wordpress.android.ui.reader.viewmodels.ReaderSubsSite
import org.wordpress.android.ui.reader.viewmodels.ReaderSubsTab
import org.wordpress.android.ui.reader.viewmodels.ReaderSubsTag
import org.wordpress.android.ui.reader.viewmodels.ReaderSubsUiState
import org.wordpress.android.ui.reader.views.compose.ReaderFollowToggleButton
import org.wordpress.android.ui.reader.views.compose.ReaderSiteRow
import org.wordpress.android.ui.rs.contentlist.ContentListSearchClearButton
import org.wordpress.android.ui.rs.contentlist.ContentListSearchField

// keeps the last row's button clear of the FAB
private val listBottomPadding = 88.dp

/**
 * Manage subscriptions: the followed tags and sites in swipeable tabs, with a field for following a
 * tag or site by name or URL. The Sites tab can be searched.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderSubsScreen(
    uiState: ReaderSubsUiState,
    actions: ReaderSubsActions,
    snackbarHostState: SnackbarHostState
) {
    val tabs = ReaderSubsTab.entries
    val pagerState = rememberPagerState(initialPage = uiState.selectedTab.ordinal) { tabs.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { actions.onTabSelected(tabs[it]) }
    }

    val isSearchVisible = uiState.siteSearchQuery != null && tabs[pagerState.currentPage] == ReaderSubsTab.SITES
    BackHandler(enabled = isSearchVisible, onBack = actions.onSiteSearchClosed)
    // set by the search icon, so only opening the search raises the keyboard - not swiping back to the
    // Blogs tab or rotating with an empty search open
    var shouldFocusSearch by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchVisible) {
                            SiteSearchField(
                                query = uiState.siteSearchQuery.orEmpty(),
                                shouldFocus = shouldFocusSearch,
                                onFocused = { shouldFocusSearch = false },
                                actions = actions
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.reader_activity_title_manage_tags_blogs),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = if (isSearchVisible) actions.onSiteSearchClosed else actions.onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (isSearchVisible) {
                            ContentListSearchClearButton(uiState.siteSearchQuery.orEmpty()) {
                                actions.onSiteSearchQueryChanged("")
                            }
                        } else if (tabs[pagerState.currentPage] == ReaderSubsTab.SITES) {
                            IconButton(onClick = {
                                shouldFocusSearch = true
                                actions.onSiteSearchOpened()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                            }
                        }
                    }
                )
                // the top bar slot isn't inset, and only the TopAppBar insets itself
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = tab.ordinal == pagerState.currentPage,
                            onClick = { scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                            text = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        },
        bottomBar = { EntryBar(uiState, actions) },
        floatingActionButton = {
            // hidden while typing, since it would cover the list - or the top bar in landscape
            if (!WindowInsets.isImeVisible) {
                FloatingActionButton(onClick = actions.onShowInterests) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (tabs[page]) {
                    ReaderSubsTab.TAGS -> TagsPage(uiState.tags, actions)
                    ReaderSubsTab.SITES -> SitesPage(uiState.sites, uiState.siteSearchQuery, actions)
                }
            }
            if (uiState.isAdding) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun SiteSearchField(
    query: String,
    shouldFocus: Boolean,
    onFocused: () -> Unit,
    actions: ReaderSubsActions
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(shouldFocus) {
        if (shouldFocus) {
            focusRequester.requestFocus()
            onFocused()
        }
    }
    ContentListSearchField(
        query = query,
        onQueryChange = actions.onSiteSearchQueryChanged,
        placeholderResId = R.string.reader_hint_search_subscribed_blogs,
        modifier = Modifier.focusRequester(focusRequester)
    )
}

@Composable
private fun EntryBar(uiState: ReaderSubsUiState, actions: ReaderSubsActions) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
    ) {
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = uiState.entry,
                onValueChange = actions.onEntryChanged,
                placeholder = { Text(stringResource(R.string.reader_hint_add_tag_or_url_follow)) },
                enabled = !uiState.isAdding,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { actions.onAddClicked() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = actions.onAddClicked, enabled = !uiState.isAdding) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    }
}

@Composable
private fun TagsPage(tags: List<ReaderSubsTag>?, actions: ReaderSubsActions) {
    when {
        tags == null -> LoadingState()
        tags.isEmpty() -> EmptyContentM3(
            title = stringResource(R.string.reader_no_followed_tags_text_title),
            subtitle = stringResource(R.string.reader_empty_subscribed_tags_subtitle),
            modifier = Modifier.fillMaxSize()
        )
        else -> LazyColumn(
            contentPadding = PaddingValues(bottom = listBottomPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tags, key = { it.slug }) { tag ->
                TagRow(tag, onFollowClick = { actions.onTagFollowClicked(tag) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TagRow(tag: ReaderSubsTag, onFollowClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = tag.tag.label,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        )
        ReaderFollowToggleButton(tag.isFollowing, tag.isFollowEnabled, onFollowClick)
    }
}

@Composable
private fun SitesPage(sites: List<ReaderSubsSite>?, searchQuery: String?, actions: ReaderSubsActions) {
    // keyed by the query so each search starts at the top, rather than where the previous list was scrolled to.
    // An open but empty search shows the same list as no search, so the two share a key.
    val listState = rememberSaveable(searchQuery?.takeIf { it.isNotEmpty() }, saver = LazyListState.Saver) {
        LazyListState()
    }
    when {
        sites == null -> LoadingState()
        sites.isEmpty() && !searchQuery.isNullOrEmpty() -> EmptyContentM3(
            title = stringResource(R.string.reader_no_followed_blogs_search_title),
            modifier = Modifier.fillMaxSize()
        )
        sites.isEmpty() -> EmptyContentM3(
            title = stringResource(R.string.reader_no_followed_blogs_title),
            subtitle = stringResource(R.string.reader_no_followed_blogs_description_subs),
            modifier = Modifier.fillMaxSize()
        )
        else -> LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = listBottomPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sites, key = { it.key }) { site ->
                ReaderSiteRow(
                    title = site.title,
                    subtitle = site.subtitle,
                    iconUrl = site.iconUrl,
                    isFollowing = site.isFollowing,
                    isFollowEnabled = site.isFollowEnabled,
                    onClick = { actions.onSiteClicked(site) },
                    onFollowClick = { actions.onSiteFollowClicked(site) }
                )
                HorizontalDivider()
            }
        }
    }
}
