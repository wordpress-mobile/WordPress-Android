package org.wordpress.android.viewmodel.posts

import android.support.annotation.ColorRes
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.RemotePostId
import org.wordpress.android.widgets.PostListButtonType

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

interface PostListItemBasicUiStateData {
    val title: UiString?
    val imageUrl: String?
    val date: UiString?
    val statusesColor: Int?
    val statuses: List<UiString>
    val statusesDelimiter: UiString
}

data class PostListItemUiStateData(
    val remotePostId: RemotePostId,
    val localPostId: LocalPostId,
    override val title: UiString?,
    val excerpt: UiString?,
    override val imageUrl: String?,
    override val date: UiString?,
    @ColorRes override val statusesColor: Int?,
    override val statuses: List<UiString>,
    override val statusesDelimiter: UiString,
    val showProgress: Boolean,
    val showOverlay: Boolean
) : PostListItemBasicUiStateData

data class PostListItemCompactUiStateData(
    val remotePostId: RemotePostId,
    val localPostId: LocalPostId,
    override val title: UiString?,
    override val date: UiString?,
    override val imageUrl: String?,
    override val statuses: List<UiString>,
    override val statusesDelimiter: UiString,
    @ColorRes override val statusesColor: Int?
) : PostListItemBasicUiStateData

sealed class PostListItemAction(val buttonType: PostListButtonType, val onButtonClicked: (PostListButtonType) -> Unit) {
    class SingleItem(buttonType: PostListButtonType, onButtonClicked: (PostListButtonType) -> Unit) :
            PostListItemAction(buttonType, onButtonClicked)

    class MoreItem(
        val actions: List<PostListItemAction>,
        onButtonClicked: (PostListButtonType) -> Unit
    ) : PostListItemAction(PostListButtonType.BUTTON_MORE, onButtonClicked)
}
