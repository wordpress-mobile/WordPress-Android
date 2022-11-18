package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import javax.inject.Inject

class LocalPostsHelper @Inject constructor(
    private val dispatcher: Dispatcher,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun migratePosts() = localMigrationContentResolver.getResultForEntityType<PostsData>(Post).thenWith { postsData ->
        postsData.localIds.map { localPostId ->
            localMigrationContentResolver.getResultForEntityType<PostData>(Post, localPostId)
        }.reduce { thisResult, nextResult -> thisResult
                .thenWith(::dispatchPost)
                .then { nextResult } }
                .then { Success(postsData) }
    }

    @Suppress("ForbiddenComment")
    // TODO: Currently, this only emits an event and doesn't reveal failures. This should be changed to a synchronous
    // write to the db (similar to the sites migration).
    private fun dispatchPost(postData: PostData) = run {
        dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(postData.post))
        Success(postData)
    }
}

private fun <T: LocalContentEntityData, E: ProviderError> LocalMigrationResult<LocalContentEntityData, E>
        .then(next: () -> LocalMigrationResult<T, E>) = when (this) {
    is Success -> next()
    is Failure -> this
}
