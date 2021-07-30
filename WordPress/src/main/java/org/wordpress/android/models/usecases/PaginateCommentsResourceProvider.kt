package org.wordpress.android.models.usecases

import dagger.Reusable
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.ui.comments.unified.UnrepliedCommentsUtils
import javax.inject.Inject

@Reusable
class PaginateCommentsResourceProvider @Inject constructor(
    val commentsStore: CommentsStore,
    val unrepliedCommentsUtils: UnrepliedCommentsUtils
)
