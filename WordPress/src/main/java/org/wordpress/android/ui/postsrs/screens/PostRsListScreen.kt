package org.wordpress.android.ui.postsrs.screens

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.ConfirmationDialogState
import org.wordpress.android.ui.postsrs.PendingConfirmation
import org.wordpress.android.ui.postsrs.PostRsListTab
import org.wordpress.android.ui.postsrs.SnackbarMessage
import org.wordpress.android.ui.postsrs.PostRsListViewModel.Companion.MIN_SEARCH_QUERY_LENGTH
import org.wordpress.android.ui.postsrs.PostRsMenuAction
import org.wordpress.android.ui.postsrs.PostTabUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsListScreen(
    tabStates: Map<PostRsListTab, PostTabUiState>,
    isSearchActive: Boolean,
    searchQuery: String,
    authorFilter: AuthorFilterSelection,
    isAuthorFilterSupported: Boolean,
    avatarUrl: String?,
    confirmationDialog: ConfirmationDialogState,
    snackbarMessages: Flow<SnackbarMessage> = emptyFlow(),
    onSearchOpen: () -> Unit,
    onSearchQueryChanged: (String, PostRsListTab) -> Unit,
    onSearchClose: (PostRsListTab) -> Unit,
    onAuthorFilterChanged: (AuthorFilterSelection, PostRsListTab) -> Unit,
    onInitTab: (PostRsListTab) -> Unit,
    onRefreshTab: (PostRsListTab) -> Unit,
    onLoadMore: (PostRsListTab) -> Unit,
    onNavigateBack: () -> Unit,
    onPostClick: (Long, PostRsListTab) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
    onCreatePost: () -> Unit
) {
    val tabs = PostRsListTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val activeTab = tabs[pagerState.settledPage]
    val snackbarHostState = remember { SnackbarHostState() }

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
                        Text(text = stringResource(R.string.my_site_btn_blog_posts))
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
            FloatingActionButton(
                onClick = onCreatePost,
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.posts_empty_list_button)
                )
            }
        }
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (!isSearchActive) {
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
                            text = { Text(text = stringResource(tab.labelResId)) }
                        )
                    }
                }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.collect { page -> onInitTab(tabs[page]) }
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
                    isSearchIdle = isSearchActive && searchQuery.length < MIN_SEARCH_QUERY_LENGTH,
                    isSearching = isSearchActive && searchQuery.length >= MIN_SEARCH_QUERY_LENGTH,
                    onRefresh = { onRefreshTab(tab) },
                    onLoadMore = { onLoadMore(tab) },
                    onPostClick = { postId -> onPostClick(postId, tab) },
                    onPostMenuAction = onPostMenuAction,
                    onCreatePost = onCreatePost
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
