package org.wordpress.android.models.usecases

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named

@Reusable
class ModerateCommentsResourceProvider @Inject constructor(
    val commentsStore: CommentsStore,
    val localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher
)
