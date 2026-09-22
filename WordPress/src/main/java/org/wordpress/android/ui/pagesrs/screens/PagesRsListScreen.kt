package org.wordpress.android.ui.pagesrs.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.rsDebugTitle
import org.wordpress.android.ui.pagesrs.PageRsListConfirmation
import org.wordpress.android.ui.pagesrs.PageRsListItem
import org.wordpress.android.ui.pagesrs.PageRsListTab
import org.wordpress.android.ui.pagesrs.PageRsMenuAction
import org.wordpress.android.ui.pagesrs.PageRsParentPickerState
import org.wordpress.android.ui.pagesrs.PagesRsListViewModel.Companion.MIN_SEARCH_QUERY_LENGTH
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.rs.RsConfirmationDialogState
import org.wordpress.android.ui.rs.RsReveal
import org.wordpress.android.ui.rs.RsSnackbarMessage
import org.wordpress.android.ui.rs.RsTabUiState
import org.wordpress.android.ui.rs.contentlist.ContentListAuthorFilterButton
import org.wordpress.android.ui.rs.contentlist.ContentListConfirmationDialog
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentListDensityToggle
import org.wordpress.android.ui.rs.contentlist.ContentListTabRow
import org.wordpress.android.ui.rs.contentlist.ReportSettledTab
import org.wordpress.android.ui.rs.contentlist.ShowRsSnackbars

@Suppress("CyclomaticComplexMethod", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PagesRsListScreen(
    tabStates: Map<PageRsListTab, RsTabUiState<PageRsListItem>>,
    isSearchActive: Boolean,
    isOpeningPage: Boolean,
    searchQuery: String,
    authorFilter: AuthorFilterSelection,
    isAuthorFilterSupported: Boolean,
    avatarUrl: String?,
    confirmationDialog: RsConfirmationDialogState<PageRsListConfirmation>,
    parentPicker: PageRsParentPickerState?,
    snackbarMessages: Flow<RsSnackbarMessage> = emptyFlow(),
    revealRequests: Flow<RsReveal<PageRsListTab>> = emptyFlow(),
    onSearchOpen: () -> Unit,
    onSearchQueryChanged: (String, PageRsListTab) -> Unit,
    onSearchClose: (PageRsListTab) -> Unit,
    onAuthorFilterChanged: (AuthorFilterSelection, PageRsListTab) -> Unit,
    onInitTab: (PageRsListTab) -> Unit,
    onTabChanged: (PageRsListTab) -> Unit,
    onRefreshTab: (PageRsListTab) -> Unit,
    onLoadMore: (PageRsListTab) -> Unit,
    onNavigateBack: () -> Unit,
    onPageClick: (Long, PageRsListTab) -> Unit,
    onPageMenuAction: (Long, PageRsMenuAction) -> Unit,
    onParentSelected: (Long) -> Unit,
    onParentSearchChanged: (String) -> Unit,
    onLoadMoreParents: () -> Unit,
    onParentPickerDismissed: () -> Unit,
    onAddNewPage: () -> Unit,
    onRowsVisible: (PageRsListTab, List<Long>) -> Unit,
    onDensityToggled: (PageRsListTab) -> Unit,
    density: ContentListDensity = ContentListDensity.COMFORTABLE,
    isRedesignEnabled: Boolean = false
) {
    val tabs = PageRsListTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val activeTab = tabs[pagerState.settledPage]
    val snackbarHostState = remember { SnackbarHostState() }
    // A page the user just saved, to be scrolled to once the tab showing it has it. Held here
    // rather than acted on in the collector below so that the tab switch, which the user can win,
    // can be cancelled without taking the collector down with it.
    var pendingReveal by remember { mutableStateOf<RsReveal<PageRsListTab>?>(null) }

    LaunchedEffect(revealRequests) {
        revealRequests.collect { pendingReveal = it }
    }

    // The page's status decides the tab, which may not be the one on screen. Switching to it also
    // initializes a tab that was never opened, so its first fetch brings the page in.
    LaunchedEffect(pendingReveal) {
        val reveal = pendingReveal ?: return@LaunchedEffect
        if (isSearchActive) {
            pendingReveal = null
            return@LaunchedEffect
        }
        val page = tabs.indexOf(reveal.tab)
        if (pagerState.settledPage != page) pagerState.animateScrollToPage(page)
    }

    BackHandler(enabled = isSearchActive) { onSearchClose(activeTab) }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }

    ShowRsSnackbars(snackbarMessages, snackbarHostState)

    Scaffold(
        // Cards are drawn on `surface`, so the page behind them has to sit one step recessed or
        // they read as a flat sheet. Which role that is differs by mode: this app's dark scheme
        // makes `surface` darker than `surfaceContainerLow`, so reusing the light-mode role there
        // would put the page *above* the cards. The pre-redesign list keeps the theme background.
        containerColor = when {
            !isRedesignEnabled -> MaterialTheme.colorScheme.background
            isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceContainerLowest
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AddPageFab(
                visible = !isSearchActive,
                isExtended = isRedesignEnabled,
                onClick = onAddNewPage
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { query -> onSearchQueryChanged(query, activeTab) },
                            placeholder = {
                                Text(stringResource(R.string.pages_search_suggestion))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { focusManager.clearFocus() }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    } else {
                        Text(text = rsDebugTitle(R.string.my_site_btn_site_pages))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) onSearchClose(activeTab) else onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("", activeTab) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        }
                    } else {
                        // Ahead of the author filter, not between it and search: these actions are
                        // end-aligned, so inserting anywhere later would shift both pre-existing
                        // icons left of where they have always been.
                        if (isRedesignEnabled) {
                            ContentListDensityToggle(
                                density = density,
                                onToggle = { onDensityToggled(activeTab) }
                            )
                        }
                        if (isAuthorFilterSupported) {
                            ContentListAuthorFilterButton(
                                authorFilter = authorFilter,
                                avatarUrl = avatarUrl,
                                onSelectionChanged = { selection ->
                                    onAuthorFilterChanged(selection, activeTab)
                                }
                            )
                        }
                        IconButton(onClick = onSearchOpen) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.pages_search_suggestion)
                            )
                        }
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (!isSearchActive) {
                ContentListTabRow(
                    labels = tabs.map { stringResource(it.labelResId) },
                    selectedIndex = pagerState.settledPage,
                    isRedesignEnabled = isRedesignEnabled,
                    onSelect = { index ->
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    }
                )
            }

            ReportSettledTab(
                pagerState = pagerState,
                onTabSettled = { page -> onInitTab(tabs[page]) },
                onTabChanged = { page -> onTabChanged(tabs[page]) },
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !isSearchActive
            ) { page ->
                val tab = tabs[page]
                val tabState = tabStates[tab] ?: RsTabUiState(isLoading = true)

                PageRsTabListScreen(
                    state = tabState,
                    emptyMessageResId = tab.emptyMessageResId,
                    revealPageId = pendingReveal?.takeIf { it.tab == tab }?.remoteId,
                    onRevealHandled = { pendingReveal = null },
                    isSearchIdle = isSearchActive && searchQuery.length < MIN_SEARCH_QUERY_LENGTH,
                    isSearching = isSearchActive && searchQuery.length >= MIN_SEARCH_QUERY_LENGTH,
                    onRefresh = { onRefreshTab(tab) },
                    onLoadMore = { onLoadMore(tab) },
                    onPageClick = { pageId -> onPageClick(pageId, tab) },
                    onPageMenuAction = onPageMenuAction,
                    onRowsVisible = { ids -> onRowsVisible(tab, ids) },
                    density = density,
                    isRedesignEnabled = isRedesignEnabled
                )
            }
        }
    }

    PageConfirmationDialogHost(confirmationDialog)

    if (parentPicker != null) {
        PageRsParentPickerSheet(
            state = parentPicker,
            onParentSelected = onParentSelected,
            onParentSearchChanged = onParentSearchChanged,
            onLoadMoreParents = onLoadMoreParents,
            onDismiss = onParentPickerDismissed
        )
    }

    if (isOpeningPage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun PageConfirmationDialogHost(confirmationDialog: RsConfirmationDialogState<PageRsListConfirmation>) {
    when (val pending = confirmationDialog.pending) {
        is PageRsListConfirmation.Trash -> ContentListConfirmationDialog(
            titleResId = R.string.trash,
            message = stringResource(R.string.page_rs_confirm_trash_message),
            onConfirm = confirmationDialog.onConfirm,
            onDismiss = confirmationDialog.onDismiss
        )
        is PageRsListConfirmation.Delete -> ContentListConfirmationDialog(
            titleResId = R.string.delete_page,
            message = stringResource(R.string.page_delete_dialog_message, pending.pageTitle),
            confirmTextResId = R.string.delete,
            isDestructive = true,
            onConfirm = confirmationDialog.onConfirm,
            onDismiss = confirmationDialog.onDismiss
        )
        is PageRsListConfirmation.MoveToDraft -> ContentListConfirmationDialog(
            titleResId = R.string.page_rs_move_trashed_page_to_draft_dialog_title,
            message = stringResource(R.string.page_rs_move_trashed_page_to_draft_dialog_message),
            confirmTextResId = R.string.pages_move_to_draft,
            onConfirm = confirmationDialog.onConfirm,
            onDismiss = confirmationDialog.onDismiss
        )
        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPageFab(visible: Boolean, isExtended: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        // Serves as the extended FAB's own label as well as the plain one's tooltip.
        val label = stringResource(R.string.create_page_fab_tooltip)
        if (isExtended) {
            // The extended form spells the action out, so the tooltip it would otherwise need
            // would only repeat the label already on screen.
            ExtendedFloatingActionButton(
                onClick = onClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(label) }
            )
        } else {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(label) } },
                state = rememberTooltipState()
            ) {
                FloatingActionButton(onClick = onClick) {
                    Icon(Icons.Default.Add, contentDescription = label)
                }
            }
        }
    }
}

