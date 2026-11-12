package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    statusLabels = statusLabels,
    isStatusError = isUploadFailed
)

/**
 * The drafts that exist only on this device, under their own header ahead of the server's rows.
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
    if (drafts.isEmpty()) return
    item(key = LOCAL_DRAFTS_HEADER_KEY, contentType = LOCAL_DRAFTS_HEADER_KEY) {
        ContentListSectionHeader(
            text = stringResource(R.string.rs_local_drafts_header),
            modifier = Modifier.animateItem()
        )
    }
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

private const val LOCAL_DRAFTS_HEADER_KEY = "local_drafts_header"
private const val LOCAL_DRAFT_KEY_PREFIX = "local_draft_"
