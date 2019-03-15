package org.wordpress.android.viewmodel.posts

import android.support.annotation.ColorRes
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

data class PostListItemAction(val buttonType: PostListButtonType, val onButtonClicked: (PostListButtonType) -> Unit)


