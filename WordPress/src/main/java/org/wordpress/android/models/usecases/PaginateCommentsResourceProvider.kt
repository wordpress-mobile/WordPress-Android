package org.wordpress.android.models.usecases

import dagger.Reusable
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.ui.comments.unified.UnrepliedCommentsUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@Reusable
class PaginateCommentsResourceProvider @Inject constructor(
    val commentsStore: CommentsStore,
    val unrepliedCommentsUtils: UnrepliedCommentsUtils,
    val networkUtilsWrapper: NetworkUtilsWrapper,
    val resourceProvider: ResourceProvider
)
