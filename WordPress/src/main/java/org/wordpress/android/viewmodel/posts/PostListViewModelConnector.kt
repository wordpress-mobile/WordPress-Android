package org.wordpress.android.viewmodel.posts

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.PostActionHandler
import org.wordpress.android.ui.posts.PostConflictResolver
import org.wordpress.android.ui.posts.PostListFeaturedImageTracker
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostListUploadStatusTracker

class PostListViewModelConnector(
    val site: SiteModel,
    val postListType: PostListType,
    val authorFilter: AuthorFilterSelection,
    val postActionHandler: PostActionHandler,
    val featuredImageTracker: PostListFeaturedImageTracker,
    val uploadStatusTracker: PostListUploadStatusTracker,
    val postConflictResolver: PostConflictResolver
)
