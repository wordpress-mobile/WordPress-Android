package org.wordpress.android.viewmodel.posts

import android.support.annotation.ColorRes
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.widgets.PostListButtonType

data class PostListItemUiState(
    val remotePostId: Long,
    val localPostId: Int,
    val title: UiString?,
    val excerpt: UiString?,
    val imageUrl: String?,
    val dateAndAuthor: UiString?,
    val statusLabels: UiString?,
    @ColorRes val statusLabelsColor: Int?,
    val actions: List<PostListItemAction>,
    val showProgress: Boolean,
    val showOverlay: Boolean,
    val onSelected: () -> Unit
)

sealed class PostListItemAction(val buttonType: PostListButtonType, val onButtonClicked: (PostListButtonType) -> Unit) {
    class SingleItem(buttonType: PostListButtonType, onButtonClicked: (PostListButtonType) -> Unit) :
            PostListItemAction(buttonType, onButtonClicked)

    class MoreItem(
        val actions: List<PostListItemAction>,
        onButtonClicked: (PostListButtonType) -> Unit
    ) : PostListItemAction(PostListButtonType.BUTTON_MORE, onButtonClicked)
}

data class PostListItemUploadStatus(
    val uploadError: UploadError?,
    val mediaUploadProgress: Int,
    val isUploading: Boolean,
    val isUploadingOrQueued: Boolean,
    val isQueued: Boolean,
    val isUploadFailed: Boolean,
    val hasInProgressMediaUpload: Boolean,
    val hasPendingMediaUpload: Boolean
)
