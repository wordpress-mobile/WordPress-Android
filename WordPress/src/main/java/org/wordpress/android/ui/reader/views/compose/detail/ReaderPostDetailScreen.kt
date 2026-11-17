package org.wordpress.android.ui.reader.views.compose.detail

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.LoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.compose.ReaderShellLayout
import org.wordpress.android.ui.reader.views.compose.ReaderTopBarScrollConnection
import org.wordpress.android.ui.reader.views.compose.postcard.ReaderPostMoreMenuItem

private const val ACTION_MAX_WIDTH_DP = 80

/**
 * Which top bar actions are showing. Only More shows while the post loads, and none do once it fails.
 */
data class ReaderPostDetailTopBarState(
    val showReadingPreferences: Boolean = false,
    val showBrowse: Boolean = false,
    val showShare: Boolean = false,
    val showMore: Boolean = true,
)

/**
 * The parts of the screen driven by the fragment, rather than by the view model's UI state: the WebView body and
 * what waits for it, fullscreen video and snackbars
 */
@Stable
class ReaderPostDetailChromeState(canScroll: () -> Boolean) {
    /** Whether the post's body and the bottom bar are showing, which they aren't while it loads or fails */
    var isPostShown by mutableStateOf(false)

    /** Whether the WebView has laid out the post's body, which the parts below it wait for */
    var hasWebViewContent by mutableStateOf(false)

    /** Whether fullscreen video is showing, which hides the top and bottom bars */
    var isCustomViewShowing by mutableStateOf(false)

    /**
     * The state of the post this screen showed before it was replaced by another in place (a related post, or one
     * from its post history), which mustn't render below the new post while it loads
     */
    var replacedState by mutableStateOf<UiState?>(null)

    val snackbarHostState = SnackbarHostState()

    val scrollConnection = ReaderTopBarScrollConnection(canScroll)

    /**
     * The system bars' top inset in pixels, from the View insets rather than Compose's: those follow the pager's
     * status bar as it animates away, which would slide the top bar up as the screen opens
     */
    var topInset by mutableIntStateOf(0)
}

@Suppress("LongParameterList")
class ReaderPostDetailScreenActions(
    val onNavigationClick: () -> Unit,
    val onReadingPreferencesClick: () -> Unit,
    val onBrowseClick: () -> Unit,
    val onShareClick: () -> Unit,
    val onMoreClick: () -> Unit,
    val onMoreDismiss: () -> Unit,
    val onMoreItemClick: (ReaderPostCardActionType) -> Unit,
    val onSignInClick: () -> Unit,
    val onOpenInBrowserClick: () -> Unit,
)

/**
 * The post detail's chrome around [body], the View that scrolls the post: a WebView can't be measured inside a
 * Compose scroll container, so the body stays a NestedScrollView whose scrolling hides the bars.
 */
@Composable
fun ReaderPostDetailScreen(
    chrome: ReaderPostDetailChromeState,
    uiState: UiState?,
    topBarState: ReaderPostDetailTopBarState,
    isRelatedPost: Boolean,
    actions: ReaderPostDetailScreenActions,
    body: View,
    customViewContainer: View,
) {
    val detailsState = uiState as? ReaderPostDetailsUiState
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ReaderShellLayout(
            scrollConnection = chrome.scrollConnection,
            topBar = {
                if (!chrome.isCustomViewShowing) {
                    TopBar(topBarState, detailsState?.moreMenuItems, isRelatedPost, chrome.topInset, actions)
                }
            },
            content = { AndroidView(factory = { body }, modifier = Modifier.fillMaxSize()) },
        )
        when (uiState) {
            is LoadingUiState -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is ErrorUiState -> ErrorContent(uiState, actions, Modifier.align(Alignment.Center))
            else -> Unit
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            SnackbarHost(chrome.snackbarHostState)
            if (detailsState != null && chrome.isPostShown && !chrome.isCustomViewShowing) {
                BottomBar(
                    state = detailsState,
                    // hides as the top bar does, sliding down by as much of its height as the top bar has slid up
                    modifier = Modifier.graphicsLayer {
                        translationY = size.height * chrome.scrollConnection.hiddenFraction
                    }
                )
            }
        }
        // only composed while video is showing, since it would otherwise take every touch meant for the post
        if (chrome.isCustomViewShowing) {
            AndroidView(factory = { customViewContainer }, modifier = Modifier.fillMaxSize())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    state: ReaderPostDetailTopBarState,
    moreMenuItems: List<ReaderPostCardAction>?,
    isRelatedPost: Boolean,
    topInset: Int,
    actions: ReaderPostDetailScreenActions,
) {
    TopAppBar(
        title = {
            if (isRelatedPost) {
                Text(
                    text = stringResource(R.string.reader_title_related_post_detail),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            // a related post's X closes it, rather than going back through its post history
            if (isRelatedPost) {
                TopBarAction(R.drawable.ic_cross_white_24dp, R.string.close, actions.onNavigationClick)
            } else {
                TopBarAction(R.drawable.ic_arrow_left_white_24dp, R.string.navigate_up_desc, actions.onNavigationClick)
            }
        },
        actions = {
            if (state.showReadingPreferences) {
                TopBarAction(
                    R.drawable.ic_reader_preferences,
                    R.string.reader_menu_reading_preferences,
                    actions.onReadingPreferencesClick
                )
            }
            if (state.showBrowse) {
                TopBarAction(R.drawable.ic_globe_white_24dp, R.string.view_in_browser, actions.onBrowseClick)
            }
            if (state.showShare) {
                TopBarAction(R.drawable.ic_share_white_24dp, R.string.share_action, actions.onShareClick)
            }
            if (state.showMore) {
                Box {
                    TopBarAction(R.drawable.ic_ellipsis_vertical_white_24dp, R.string.more, actions.onMoreClick)
                    DropdownMenu(expanded = moreMenuItems != null, onDismissRequest = actions.onMoreDismiss) {
                        moreMenuItems?.forEach { item ->
                            ReaderPostMoreMenuItem(item) { actions.onMoreItemClick(it.type) }
                        }
                    }
                }
            }
        },
        // the other colors default to the surface's, but action icons default to onSurfaceVariant, which the reading
        // theme makes translucent
        colors = TopAppBarDefaults.topAppBarColors(actionIconContentColor = MaterialTheme.colorScheme.onSurface),
        // only the status bar, which the pager hides, rather than M3's default of the bars plus the display cutout:
        // that would leave a gap above the bar on a device with a camera cutout, where the old toolbar had none
        windowInsets = WindowInsets(top = topInset),
    )
}

@Composable
private fun TopBarAction(@DrawableRes iconRes: Int, @StringRes labelRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painter = painterResource(iconRes), contentDescription = stringResource(labelRes))
    }
}

@Composable
private fun BottomBar(state: ReaderPostDetailsUiState, modifier: Modifier = Modifier) {
    val actions = state.actions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.Medium.value),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BottomBarAction(
                postId = state.postId,
                blogId = state.blogId,
                action = actions.bookmarkAction,
                labelRes = R.string.save,
                iconRes = if (actions.bookmarkAction.isSelected && actions.bookmarkAction.isEnabled) {
                    R.drawable.ic_bookmark_fill_new_24dp
                } else {
                    R.drawable.ic_bookmark_outline_new_24dp
                },
            )
            BottomBarAction(
                postId = state.postId,
                blogId = state.blogId,
                action = actions.reblogAction,
                labelRes = R.string.reader_view_reblog,
                iconRes = R.drawable.ic_reblog_new_24dp,
            )
            BottomBarAction(
                postId = state.postId,
                blogId = state.blogId,
                action = actions.commentsAction,
                labelRes = R.string.comment,
                iconRes = R.drawable.ic_comment_outline_new_24dp,
            )
            BottomBarAction(
                postId = state.postId,
                blogId = state.blogId,
                action = actions.likeAction,
                labelRes = R.string.like,
                iconRes = if (actions.likeAction.isSelected) {
                    R.drawable.ic_like_fill_new_24dp
                } else {
                    R.drawable.ic_like_outline_new_24dp
                },
            )
        }
    }
}

@Composable
private fun BottomBarAction(
    postId: Long,
    blogId: Long,
    action: PrimaryAction,
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val color = when {
        !action.isEnabled -> onSurface.copy(alpha = DISABLED_ALPHA)
        action.isSelected -> MaterialTheme.colorScheme.primary
        else -> onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA)
    }
    val description = action.contentDescription?.let { uiStringText(it) }
    Column(
        modifier = Modifier
            .widthIn(max = ACTION_MAX_WIDTH_DP.dp)
            .heightIn(min = 48.dp)
            .clickable(enabled = action.isEnabled) {
                action.onClicked?.invoke(postId, blogId, action.type)
            }
            .semantics {
                description?.let { contentDescription = it }
                selected = action.isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = color)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorContent(state: ErrorUiState, actions: ReaderPostDetailScreenActions, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Margin.ExtraLarge.value),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.message?.let { message ->
            Icon(
                painter = painterResource(R.drawable.ic_notice_48dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = uiStringText(message),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Margin.Small.value),
            )
        }
        if (state.signInButtonVisibility) {
            ErrorButton(R.string.sign_in_wpcom, actions.onSignInClick)
        }
        if (state.openInBrowserButtonVisibility) {
            ErrorButton(R.string.reader_open_in_browser, actions.onOpenInBrowserClick)
        }
    }
}

@Composable
private fun ErrorButton(@StringRes labelRes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Margin.ExtraLarge.value),
    ) {
        Text(stringResource(labelRes))
    }
}
