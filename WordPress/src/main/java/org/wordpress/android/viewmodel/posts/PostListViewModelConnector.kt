package org.wordpress.android.viewmodel.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState

class PostListViewModelConnector(
    val site: SiteModel,
    val postListType: PostListType,
    val authorFilter: AuthorFilterSelection,
    val newPost: () -> Unit,
    val transformPostModelToPostListItemUiState: (PostModel) -> PostListItemUiState
)
