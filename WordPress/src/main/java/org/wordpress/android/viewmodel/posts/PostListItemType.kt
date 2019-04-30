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
        val actions: List<PostListItemAction>,
        val onSelected: () -> Unit
    ) : PostListItemType()

    class LoadingItem(val localOrRemoteId: LocalOrRemoteId) : PostListItemType()
    object EndListIndicatorItem : PostListItemType()
}

data class PostListItemUiStateData(
    val remotePostId: RemotePostId,
    val localPostId: LocalPostId,
    val title: UiString?,
    val excerpt: UiString?,
    val imageUrl: String?,
    val dateAndAuthor: UiString?,
    @ColorRes val statusesColor: Int?,
    val statuses: List<UiString>,
    val statusesDelimiter: UiString,
    val showProgress: Boolean,
    val showOverlay: Boolean
)

sealed class PostListItemAction(val buttonType: PostListButtonType, val onButtonClicked: (PostListButtonType) -> Unit) {
    class SingleItem(buttonType: PostListButtonType, onButtonClicked: (PostListButtonType) -> Unit) :
            PostListItemAction(buttonType, onButtonClicked)

    class MoreItem(
        val actions: List<PostListItemAction>,
        onButtonClicked: (PostListButtonType) -> Unit
    ) : PostListItemAction(PostListButtonType.BUTTON_MORE, onButtonClicked)
}
