package org.wordpress.android.ui.postsrs.screens

import androidx.annotation.StringRes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DensityLarge
import androidx.compose.material.icons.filled.DensitySmall
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.rsDebugTitle
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.ConfirmationDialogState
import org.wordpress.android.ui.postsrs.PendingConfirmation
import org.wordpress.android.ui.postsrs.PostRsListTab
import org.wordpress.android.ui.postsrs.PostRsReveal
import org.wordpress.android.ui.postsrs.SnackbarMessage
import org.wordpress.android.ui.postsrs.PostRsListViewModel.Companion.MIN_SEARCH_QUERY_LENGTH
import org.wordpress.android.ui.postsrs.PostRsMenuAction
import org.wordpress.android.ui.postsrs.PostTabUiState
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentListFilterChips

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsListScreen(
    tabStates: Map<PostRsListTab, PostTabUiState>,
    isSearchActive: Boolean,
    isOpeningPost: Boolean,
    searchQuery: String,
    authorFilter: AuthorFilterSelection,
    isAuthorFilterSupported: Boolean,
    avatarUrl: String?,
    confirmationDialog: ConfirmationDialogState,
    snackbarMessages: Flow<SnackbarMessage> = emptyFlow(),
    revealRequests: Flow<PostRsReveal> = emptyFlow(),
    onSearchOpen: () -> Unit,
    onSearchQueryChanged: (String, PostRsListTab) -> Unit,
    onSearchClose: (PostRsListTab) -> Unit,
    onAuthorFilterChanged: (AuthorFilterSelection, PostRsListTab) -> Unit,
    onInitTab: (PostRsListTab) -> Unit,
    onTabChanged: (PostRsListTab) -> Unit,
    onRefreshTab: (PostRsListTab) -> Unit,
    onLoadMore: (PostRsListTab) -> Unit,
    onNavigateBack: () -> Unit,
    onPostClick: (Long, PostRsListTab) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
    onCreatePost: () -> Unit,
    onRowsVisible: (PostRsListTab, List<Long>) -> Unit,
    onDensityToggled: (PostRsListTab) -> Unit,
    density: ContentListDensity = ContentListDensity.COMFORTABLE,
    isRedesignEnabled: Boolean = false
) {
    val tabs = PostRsListTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val activeTab = tabs[pagerState.settledPage]
    val snackbarHostState = remember { SnackbarHostState() }
    // A post the user just saved, to be scrolled to once the tab showing it has it. Held here
    // rather than acted on in the collector below so that the tab switch, which the user can win,
    // can be cancelled without taking the collector down with it.
    var pendingReveal by remember { mutableStateOf<PostRsReveal?>(null) }

    LaunchedEffect(revealRequests) {
        revealRequests.collect { pendingReveal = it }
    }

    // The post's status decides the tab, which may not be the one on screen. Switching to it also
    // initializes a tab that was never opened, so its first fetch brings the post in.
    LaunchedEffect(pendingReveal) {
        val reveal = pendingReveal ?: return@LaunchedEffect
        if (isSearchActive) {
            pendingReveal = null
            return@LaunchedEffect
        }
        val page = tabs.indexOf(reveal.tab)
        if (pagerState.settledPage != page) pagerState.animateScrollToPage(page)
    }

    LaunchedEffect(snackbarMessages) {
        snackbarMessages.collect { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg.message,
                actionLabel = msg.actionLabel
            )
            if (result == SnackbarResult.ActionPerformed) {
                msg.onAction?.invoke()
            }
        }
    }

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
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { query -> onSearchQueryChanged(query, activeTab) },
                            placeholder = {
                                Text(stringResource(R.string.post_list_search_prompt))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Search
                            ),
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
                        // Plain sans, like every other top bar in the app. The serif belongs on
                        // the row titles, which are content; a serif on the chrome reads as a
                        // rendering fault rather than a choice.
                        Text(text = rsDebugTitle(R.string.my_site_btn_blog_posts))
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
                        if (isAuthorFilterSupported) {
                            AuthorFilterButton(
                                authorFilter = authorFilter,
                                avatarUrl = avatarUrl,
                                onSelectionChanged = { selection ->
                                    onAuthorFilterChanged(selection, activeTab)
                                }
                            )
                        }
                        if (isRedesignEnabled) {
                            DensityToggleButton(
                                density = density,
                                onToggle = { onDensityToggled(activeTab) }
                            )
                        }
                        IconButton(onClick = onSearchOpen) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.post_list_search_prompt)
                            )
                        }
                    }
                }
            )

            if (isSearchActive) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        },
        floatingActionButton = {
            if (isRedesignEnabled) {
                ExtendedFloatingActionButton(
                    onClick = onCreatePost,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.content_list_fab_write)) }
                )
            } else {
                FloatingActionButton(
                    onClick = onCreatePost,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.posts_empty_list_button)
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (!isSearchActive) {
                if (isRedesignEnabled) {
                    // The pager stays: chips replace the tab row's appearance, not swiping between
                    // tabs, which users of this screen already rely on.
                    ContentListFilterChips(
                        labels = tabs.map { stringResource(it.labelResId) },
                        selectedIndex = pagerState.settledPage,
                        onSelect = { index ->
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                } else {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.settledPage,
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.settledPage == index,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text(text = stringResource(tab.labelResId)) }
                            )
                        }
                    }
                }
            }

            LaunchedEffect(pagerState) {
                var isFirstEmission = true
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    onInitTab(tabs[page])
                    if (isFirstEmission) {
                        isFirstEmission = false
                    } else {
                        onTabChanged(tabs[page])
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !isSearchActive
            ) { page ->
                val tab = tabs[page]
                val tabState = tabStates[tab] ?: PostTabUiState(isLoading = true)

                PostRsTabListScreen(
                    state = tabState,
                    emptyMessageResId = tab.emptyMessageResId,
                    revealPostId = pendingReveal?.takeIf { it.tab == tab }?.remotePostId,
                    onRevealHandled = { pendingReveal = null },
                    isSearchIdle = isSearchActive && searchQuery.length < MIN_SEARCH_QUERY_LENGTH,
                    isSearching = isSearchActive && searchQuery.length >= MIN_SEARCH_QUERY_LENGTH,
                    onRefresh = { onRefreshTab(tab) },
                    onLoadMore = { onLoadMore(tab) },
                    onPostClick = { postId -> onPostClick(postId, tab) },
                    onPostMenuAction = onPostMenuAction,
                    onCreatePost = onCreatePost,
                    onRowsVisible = { ids -> onRowsVisible(tab, ids) },
                    density = density,
                    isRedesignEnabled = isRedesignEnabled,
                    showDateGroups = tab != PostRsListTab.SCHEDULED
                )
            }
        }
    }

    when (confirmationDialog.pending) {
        is PendingConfirmation.Trash -> ConfirmationDialog(
            titleResId = R.string.trash,
            messageResId = R.string.post_rs_confirm_trash_message,
            onConfirm = confirmationDialog.onConfirm,
            onDismiss = confirmationDialog.onDismiss
        )
        is PendingConfirmation.Delete -> ConfirmationDialog(
            titleResId = R.string.delete,
            messageResId = R.string.post_rs_confirm_delete_message,
            isDestructive = true,
            onConfirm = confirmationDialog.onConfirm,
            onDismiss = confirmationDialog.onDismiss
        )
        is PendingConfirmation.MoveToDraft -> ConfirmationDialog(
            titleResId =
                R.string.post_list_move_trashed_post_to_draft_dialog_title,
            messageResId =
                R.string.post_list_move_trashed_post_to_draft_dialog_message,
            confirmTextResId =
                R.string.post_list_move_trashed_post_to_draft_dialog_positive,
            onConfirm = confirmationDialog.onConfirm,
            onDismiss = confirmationDialog.onDismiss
        )
        null -> {}
    }

    if (isOpeningPost) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember {
                        MutableInteractionSource()
                    }
                ) { },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Flips between the two list densities. Deliberately a top-bar action rather than an overflow item:
 * a view control the user is expected to find has to be visible.
 */
@Composable
private fun DensityToggleButton(
    density: ContentListDensity,
    onToggle: () -> Unit
) {
    val labelResId = if (density.isCondensed) {
        R.string.content_list_density_show_comfortable
    } else {
        R.string.content_list_density_show_condensed
    }
    IconButton(onClick = onToggle) {
        // Both glyphs come from the same family - bars at different spacing - so the two states
        // read as one control rather than two unrelated pictures. The icon shows the density the
        // tap will switch *to*, which is what the content description says.
        Icon(
            imageVector = if (density.isCondensed) {
                Icons.Default.DensityLarge
            } else {
                Icons.Default.DensitySmall
            },
            contentDescription = stringResource(labelResId)
        )
    }
}

@Composable
private fun AuthorFilterButton(
    authorFilter: AuthorFilterSelection,
    avatarUrl: String?,
    onSelectionChanged: (AuthorFilterSelection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val contentDesc = stringResource(R.string.post_list_toggle_author_filter)

    Box {
        IconButton(onClick = { expanded = true }) {
            AuthorFilterIcon(
                selection = authorFilter,
                avatarUrl = avatarUrl,
                contentDescription = contentDesc
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AuthorFilterSelection.entries.forEach { selection ->
                val label = when (selection) {
                    AuthorFilterSelection.ME -> stringResource(R.string.me)
                    AuthorFilterSelection.EVERYONE ->
                        stringResource(R.string.everyone)
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (selection == authorFilter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Unspecified
                            }
                        )
                    },
                    leadingIcon = {
                        AuthorFilterIcon(
                            selection = selection,
                            avatarUrl = avatarUrl,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectionChanged(selection)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthorFilterIcon(
    selection: AuthorFilterSelection,
    avatarUrl: String?,
    contentDescription: String?
) {
    val personIcon = if (selection == AuthorFilterSelection.ME) {
        Icons.Filled.Person
    } else {
        Icons.Outlined.Person
    }
    if (selection == AuthorFilterSelection.ME && !avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            fallback = rememberVectorPainter(personIcon),
            error = rememberVectorPainter(personIcon),
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )
    } else {
        Icon(
            personIcon,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun ConfirmationDialog(
    @StringRes titleResId: Int,
    @StringRes messageResId: Int,
    @StringRes confirmTextResId: Int = titleResId,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResId)) },
        text = { Text(stringResource(messageResId)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(confirmTextResId),
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.Unspecified
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
