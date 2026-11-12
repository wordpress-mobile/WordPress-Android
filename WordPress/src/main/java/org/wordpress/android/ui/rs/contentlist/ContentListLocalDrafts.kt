package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.wordpress.android.R
import org.wordpress.android.ui.rs.RsLocalDraftUiModel

val RsLocalDraftUiModel.menuActions: List<RsLocalDraftMenuAction>
    get() = buildList {
        if (canRetry) add(RsLocalDraftMenuAction.RETRY)
        add(RsLocalDraftMenuAction.DELETE)
    }

fun RsLocalDraftUiModel.toContentListRowUiState() = ContentListRowUiState(
    // Negative so it can never be mistaken for a remote post id.
    id = -localId.toLong(),
    title = title,
    excerpt = excerpt,
    dateLabel = dateLabel,
    // A badge rather than a section header: the pages list and search results have no headers of
    // their own, so a header would seem to cover the server's rows below it too.
    badges = listOf(R.string.rs_local_draft_badge),
    statusLabels = statusLabels,
    isStatusError = isUploadFailed
)

/**
 * The drafts that exist only on this device, ahead of the server's rows.
 *
 * Keyed by strings, so they can't collide with the remote-id keys of the rows below and drop out of
 * the visible-row reporting that fetches metrics - a draft the server has never seen has none.
 */
fun LazyListScope.localDraftItems(
    drafts: List<RsLocalDraftUiModel>,
    density: ContentListDensity,
    onClick: (Int) -> Unit,
    onAction: (Int, RsLocalDraftMenuAction) -> Unit
) {
    items(
        items = drafts,
        key = { "$LOCAL_DRAFT_KEY_PREFIX${it.localId}" },
        contentType = { LOCAL_DRAFT_KEY_PREFIX }
    ) { draft ->
        val onClickDraft = { onClick(draft.localId) }
        ContentListRow(
            state = draft.toContentListRowUiState(),
            onClick = onClickDraft,
            density = density,
            actions = draft.menuActions.toContentListRowActions(
                onEdit = onClickDraft,
                showQuickActions = !density.isCondensed
            ) { action -> onAction(draft.localId, action) },
            modifier = Modifier.animateItem()
        )
    }
}

/**
 * Scrolls to the top when a local draft is added. It lands above the first row, and LazyColumn holds on to
 * whichever row was first in view, so a draft just saved in the editor would arrive out of sight. Drafts
 * are only added by the user saving one or widening a search, so the jump is always theirs. Removals don't
 * count, so a draft uploading while the user is scrolled down leaves them where they are.
 */
@Composable
fun KeepNewLocalDraftsInView(listState: LazyListState, drafts: List<RsLocalDraftUiModel>) {
    val ids = remember(drafts) { drafts.map { it.localId } }
    val previousIds = remember { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(ids) {
        val previous = previousIds.value
        previousIds.value = ids
        if (previous != null && ids.any { it !in previous }) listState.scrollToItem(0)
    }
}

private const val LOCAL_DRAFT_KEY_PREFIX = "local_draft_"
