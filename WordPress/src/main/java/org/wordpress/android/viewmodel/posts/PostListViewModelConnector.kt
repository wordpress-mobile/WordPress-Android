package org.wordpress.android.viewmodel.posts

import android.arch.lifecycle.Lifecycle
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.ui.posts.PostListType

class PostListViewModelConnector(
    val postListType: PostListType,
    val newPost: () -> Unit,
    val createPagedListWrapper: (Lifecycle) -> PagedListWrapper<PostListItemType>
)
