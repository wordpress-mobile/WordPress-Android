package org.wordpress.android.viewmodel.posts

import android.support.annotation.ColorRes
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.RemotePostId
import org.wordpress.android.widgets.PostListButtonType
import java.util.Objects

sealed class PostListItemType {
    class PostListItemUiState(
        val data: PostListItemUiStateData,
        val compactData: PostListItemCompactUiStateData,
        val actions: List<PostListItemAction>,
        val compactActions: List<PostListItemAction>,
        val onSelected: () -> Unit
    ) : PostListItemType()
    class LoadingItem(val localOrRemoteId: LocalOrRemoteId) : PostListItemType()
    object EndListIndicatorItem : PostListItemType()
}

class PostListItemUiStateData(
    remotePostId: RemotePostId,
    localPostId: LocalPostId,
    title: UiString?,
    val excerpt: UiString?,
    imageUrl: String?,
    date: UiString?,
    @ColorRes statusesColor: Int?,
    statuses: List<UiString>,
    statusesDelimiter: UiString,
    val showProgress: Boolean,
    val showOverlay: Boolean
) : PostListItemUiData(remotePostId, localPostId, title, date, imageUrl, statuses, statusesDelimiter, statusesColor) {

    override fun hashCode(): Int {
        return Objects.hash(remotePostId,
                localPostId,
                title,
                excerpt,
                imageUrl,
                date,
                statusesColor,
                statuses,
                statusesDelimiter,
                showProgress,
                showOverlay)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is PostListItemUiStateData) {
            return false
        }

        return remotePostId == other.remotePostId &&
                localPostId == other.localPostId &&
                title == other.title &&
                excerpt == other.excerpt &&
                imageUrl == other.imageUrl &&
                date == other.date &&
                statusesColor == other.statusesColor &&
                statuses == other.statuses &&
                statusesDelimiter == other.statusesDelimiter &&
                showProgress == other.showProgress &&
                showOverlay == other.showOverlay
    }
}

class PostListItemCompactUiStateData(
    remotePostId: RemotePostId,
    localPostId: LocalPostId,
    title: UiString?,
    date: UiString?,
    imageUrl: String?,
    statuses: List<UiString>,
    statusesDelimiter: UiString,
    @ColorRes statusesColor: Int?
) : PostListItemUiData(remotePostId, localPostId, title, date, imageUrl, statuses, statusesDelimiter, statusesColor) {
    override fun hashCode(): Int {
        return Objects.hash(remotePostId,
                localPostId,
                title,
                imageUrl,
                date,
                statusesColor,
                statuses,
                statusesDelimiter)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is PostListItemUiStateData) {
            return false
        }

        return remotePostId == other.remotePostId &&
                localPostId == other.localPostId &&
                title == other.title &&
                imageUrl == other.imageUrl &&
                date == other.date &&
                statusesColor == other.statusesColor &&
                statuses == other.statuses &&
                statusesDelimiter == other.statusesDelimiter
    }
}

abstract class PostListItemUiData(
    val remotePostId: RemotePostId,
    val localPostId: LocalPostId,
    val title: UiString?,
    val date: UiString?,
    val imageUrl: String?,
    val statuses: List<UiString>,
    val statusesDelimiter: UiString,
    @ColorRes val statusesColor: Int?
)

sealed class PostListItemAction(val buttonType: PostListButtonType, val onButtonClicked: (PostListButtonType) -> Unit) {
    class SingleItem(buttonType: PostListButtonType, onButtonClicked: (PostListButtonType) -> Unit) :
            PostListItemAction(buttonType, onButtonClicked)

    class MoreItem(
        val actions: List<PostListItemAction>,
        onButtonClicked: (PostListButtonType) -> Unit
    ) : PostListItemAction(PostListButtonType.BUTTON_MORE, onButtonClicked)
}
