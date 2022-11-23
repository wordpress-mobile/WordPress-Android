package org.wordpress.android.localcontentmigration

import com.wellsql.generated.PostModelMapper
import com.yarolegovich.wellsql.mapper.MapperAdapter
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.PostsData
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError.LocalPostsPersistenceError.FailedToResetSequenceForPosts
import org.wordpress.android.localcontentmigration.LocalMigrationError.PersistenceError.LocalPostsPersistenceError.FailedToSaveLocalPost
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Companion.EmptyResult
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.resolver.DbWrapper
import javax.inject.Inject

private const val POST_TABLE_NAME = "PostModel"

class LocalPostsHelper @Inject constructor(
    private val dbWrapper: DbWrapper,
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun migratePosts() = localMigrationContentResolver.getResultForEntityType<PostsData>(Post).thenWith { (localIds) ->
        if (localIds.isEmpty()) return@thenWith EmptyResult

        resetSequence(POST_TABLE_NAME).then {
            localIds.sorted().foldAllToSingleResult(::copyPostWithId)
        }
    }

    // It's possible this may be reused for pages, so suppressing the warning for now
    @Suppress("SameParameterValue")
    private fun resetSequence(tableName: String) = runCatching {
        with(dbWrapper.giveMeWritableDb()) {
            // Not sure if this should be here. If the db is populated, we should probably fail and not delete anything.
//            delete(tableName, null, null) // copied from sites migration
            delete("sqlite_sequence", "name='$tableName'", null)
            execSQL("INSERT INTO SQLITE_SEQUENCE (name,seq) VALUES ('$tableName', 0)")
            EmptyResult
        }
    }.getOrDefault(Failure(FailedToResetSequenceForPosts))

    private fun copyPostWithId(localPostId: Int) =
            localMigrationContentResolver.getResultForEntityType<PostData>(Post, localPostId).thenWith { (post) ->
                insertPostWithId(post)
            }

    private fun insertPostWithId(post: PostModel) = runCatching {
        with(dbWrapper.giveMeWritableDb()) {
            compileStatement("UPDATE SQLITE_SEQUENCE SET seq=? WHERE name='$POST_TABLE_NAME'").apply {
                // Set the sequence number one less than the id so autoincrement will result in the correct id.
                bindLong(1, (post.id - 1).toLong())
                execute()
            }

            val postMapperAdapter = MapperAdapter(PostModelMapper())
            if (insert(POST_TABLE_NAME, null, postMapperAdapter.toCv(post)) == -1L) {
                Failure(FailedToSaveLocalPost(post))
            } else {
                EmptyResult
            }
        }
    }.getOrDefault(Failure(FailedToSaveLocalPost(post)))
}
