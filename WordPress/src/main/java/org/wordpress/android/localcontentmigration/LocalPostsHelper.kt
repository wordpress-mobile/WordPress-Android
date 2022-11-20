package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import javax.inject.Inject

class LocalPostsHelper @Inject constructor(
    private val dispatcher: Dispatcher,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun migratePosts() = localMigrationContentResolver.getResultForEntityType<PostsData>(Post).thenWith { postsData ->
        postsData.localIds.foldAllToSingleResult(::dispatchPost)
    }

    @Suppress("ForbiddenComment")
    // TODO: Currently, this only emits an event and doesn't reveal failures. This should be changed to a synchronous
    // write to the db (similar to the sites migration).
    private fun dispatchPost(localPostId: Int) =
            localMigrationContentResolver.getResultForEntityType<PostData>(Post, localPostId).thenWith { postData ->
                dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(postData.post))
                Success(postData)
            }
}
