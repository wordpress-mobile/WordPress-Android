package org.wordpress.android.fluxc.persistence.comments

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity

typealias CommentEntityList = List<CommentEntity>

@Dao
abstract class CommentsDao {
    // Public methods
    @Transaction
    open suspend fun insertOrUpdateComment(comment: CommentEntity): Long {
        return insertOrUpdateCommentInternal(comment)
    }

    @Transaction
    open suspend fun insertOrUpdateCommentForResult(comment: CommentEntity): CommentEntityList {
        val entityId = insertOrUpdateCommentInternal(comment)
        return getCommentById(entityId)
    }

    @Transaction
    open suspend fun getFilteredComments(localSiteId: Int, statuses: List<String>): CommentEntityList {
        return getFilteredCommentsInternal(localSiteId, statuses, statuses.isNotEmpty())
    }

    @Transaction
    open suspend fun getCommentsByLocalSiteId(
        localSiteId: Int,
        statuses: List<String>,
        limit: Int,
        orderAscending: Boolean
    ): CommentEntityList {
        return getCommentsByLocalSiteIdInternal(
                localSiteId = localSiteId,
                filterByStatuses = statuses.isNotEmpty(),
                statuses = statuses,
                limit = limit,
                orderAscending = orderAscending
        )
    }

    @Transaction
    open suspend fun deleteComment(comment: CommentEntity): Int {
        val result = deleteById(comment.id)

        return if (result > 0) {
            result
        } else {
            deleteByLocalSiteAndRemoteIds(comment.localSiteId, comment.remoteCommentId)
        }
    }

    @Transaction
    open suspend fun removeGapsFromTheTop(
        localSiteId: Int,
        statuses: List<String>,
        remoteIds: List<Long>,
        startOfRange: Long
    ): Int {
        return removeGapsFromTheTopInternal(
                localSiteId = localSiteId,
                filterByStatuses = statuses.isNotEmpty(),
                statuses = statuses,
                filterByIds = remoteIds.isNotEmpty(),
                remoteIds = remoteIds,
                startOfRange = startOfRange
        )
    }

    @Transaction
    open suspend fun removeGapsFromTheBottom(
        localSiteId: Int,
        statuses: List<String>,
        remoteIds: List<Long>,
        endOfRange: Long
    ): Int {
        return removeGapsFromTheBottomInternal(
                localSiteId = localSiteId,
                filterByStatuses = statuses.isNotEmpty(),
                statuses = statuses,
                filterByIds = remoteIds.isNotEmpty(),
                remoteIds = remoteIds,
                endOfRange = endOfRange
        )
    }

    @Transaction
    open suspend fun removeGapsFromTheMiddle(
        localSiteId: Int,
        statuses: List<String>,
        remoteIds: List<Long>,
        startOfRange: Long,
        endOfRange: Long
    ): Int {
        return removeGapsFromTheMiddleInternal(
                localSiteId = localSiteId,
                filterByStatuses = statuses.isNotEmpty(),
                statuses = statuses,
                filterByIds = remoteIds.isNotEmpty(),
                remoteIds = remoteIds,
                startOfRange = startOfRange,
                endOfRange = endOfRange
        )
    }

    @Query("SELECT * FROM Comments WHERE id = :localId LIMIT 1")
    abstract suspend fun getCommentById(localId: Long): CommentEntityList

    @Query("SELECT * FROM Comments WHERE localSiteId = :localSiteId AND remoteCommentId = :remoteCommentId")
    abstract suspend fun getCommentsByLocalSiteAndRemoteCommentId(
        localSiteId: Int,
        remoteCommentId: Long
    ): CommentEntityList

    @Transaction
    open suspend fun appendOrUpdateComments(comments: CommentEntityList): Int {
        val affectedIdList = insertOrUpdateCommentsInternal(comments)
        return affectedIdList.size
    }

    @Transaction
    open suspend fun clearAllBySiteIdAndFilters(localSiteId: Int, statuses: List<String>): Int {
        return clearAllBySiteIdAndFiltersInternal(
                localSiteId = localSiteId,
                filterByStatuses = statuses.isNotEmpty(),
                statuses = statuses
        )
    }

    // Protected methods

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insert(comment: CommentEntity): Long

    @Update
    protected abstract fun update(comment: CommentEntity): Int

    @Query("""
        SELECT * FROM Comments WHERE localSiteId = :localSiteId 
        AND CASE WHEN :filterByStatuses = 1 THEN status IN (:statuses) ELSE 1 END 
        ORDER BY datePublished DESC
    """)
    protected abstract fun getFilteredCommentsInternal(
        localSiteId: Int,
        statuses: List<String>,
        filterByStatuses: Boolean
    ): CommentEntityList

    @Query("""
        SELECT * FROM Comments 
        WHERE localSiteId = :localSiteId 
        AND CASE WHEN (:filterByStatuses = 1) THEN (status IN (:statuses)) ELSE 1 END
        ORDER BY 
        CASE WHEN :orderAscending = 1 THEN datePublished END ASC,
        CASE WHEN :orderAscending = 0 THEN datePublished END DESC
        LIMIT CASE WHEN :limit > 0 THEN :limit ELSE -1 END
    """)
    protected abstract fun getCommentsByLocalSiteIdInternal(
        localSiteId: Int,
        filterByStatuses: Boolean,
        statuses: List<String>,
        limit: Int,
        orderAscending: Boolean
    ): CommentEntityList

    @Query("""
        DELETE FROM Comments 
        WHERE localSiteId = :localSiteId 
        AND CASE WHEN (:filterByStatuses = 1) THEN (status IN (:statuses)) ELSE 1 END
    """)
    protected abstract fun clearAllBySiteIdAndFiltersInternal(
        localSiteId: Int,
        filterByStatuses: Boolean,
        statuses: List<String>
    ): Int

    @Query("""
        DELETE FROM Comments 
        WHERE localSiteId = :localSiteId 
        AND CASE WHEN (:filterByStatuses = 1) THEN (status IN (:statuses)) ELSE 1 END
        AND CASE WHEN (:filterByIds = 1) THEN (remoteCommentId NOT IN (:remoteIds)) ELSE 1 END
        AND publishedTimestamp >= :startOfRange
    """)
    @Suppress("LongParameterList")
    protected abstract fun removeGapsFromTheTopInternal(
        localSiteId: Int,
        filterByStatuses: Boolean,
        statuses: List<String>,
        filterByIds: Boolean,
        remoteIds: List<Long>,
        startOfRange: Long
    ): Int

    @Query("""
        DELETE FROM Comments 
        WHERE localSiteId = :localSiteId 
        AND CASE WHEN (:filterByStatuses = 1) THEN (status IN (:statuses)) ELSE 1 END
        AND CASE WHEN (:filterByIds = 1) THEN (remoteCommentId NOT IN (:remoteIds)) ELSE 1 END
        AND publishedTimestamp <= :endOfRange
    """)
    @Suppress("LongParameterList")
    protected abstract fun removeGapsFromTheBottomInternal(
        localSiteId: Int,
        filterByStatuses: Boolean,
        statuses: List<String>,
        filterByIds: Boolean,
        remoteIds: List<Long>,
        endOfRange: Long
    ): Int

    @Query("""
        DELETE FROM Comments 
        WHERE localSiteId = :localSiteId 
        AND CASE WHEN (:filterByStatuses = 1) THEN (status IN (:statuses)) ELSE 1 END
        AND CASE WHEN (:filterByIds = 1) THEN (remoteCommentId NOT IN (:remoteIds)) ELSE 1 END
        AND publishedTimestamp <= :startOfRange
        AND publishedTimestamp >= :endOfRange
    """)
    @Suppress("LongParameterList")
    protected abstract fun removeGapsFromTheMiddleInternal(
        localSiteId: Int,
        filterByStatuses: Boolean,
        statuses: List<String>,
        filterByIds: Boolean,
        remoteIds: List<Long>,
        startOfRange: Long,
        endOfRange: Long
    ): Int

    @Query("DELETE FROM Comments WHERE id = :commentId")
    protected abstract fun deleteById(commentId: Long): Int

    @Query("DELETE FROM Comments WHERE localSiteId = :localSiteId AND remoteCommentId = :remoteCommentId")
    protected abstract fun deleteByLocalSiteAndRemoteIds(localSiteId: Int, remoteCommentId: Long): Int

    // Private methods
    private suspend fun insertOrUpdateCommentsInternal(comments: CommentEntityList): List<Long> {
        return comments.map { comment ->
            insertOrUpdateCommentInternal(comment)
        }
    }

    private suspend fun insertOrUpdateCommentInternal(comment: CommentEntity): Long {
        val commentByLocalId = getCommentById(comment.id)

        val matchingComments = if (commentByLocalId.isEmpty()) {
            getCommentsByLocalSiteAndRemoteCommentId(comment.localSiteId, comment.remoteCommentId)
        } else {
            commentByLocalId
        }

        return if (matchingComments.isEmpty()) {
            insert(comment)
        } else {
            // We are forcing the id of the matching comment so the update can
            // act on the expected entity
            val matchingComment = matchingComments.first()

            update(comment.copy(id = matchingComment.id))
            matchingComment.id
        }
    }

    @Entity(
            tableName = "Comments"
    )
    data class CommentEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        val remoteCommentId: Long,
        val remotePostId: Long,
        val localSiteId: Int,
        val remoteSiteId: Long,
        val authorUrl: String?,
        val authorName: String?,
        val authorEmail: String?,
        val authorProfileImageUrl: String?,
        val authorId: Long,
        val postTitle: String?,
        val status: String?,
        val datePublished: String?,
        val publishedTimestamp: Long,
        val content: String?,
        val url: String?,
        val hasParent: Boolean,
        val parentId: Long,
        val iLike: Boolean
    ) {
        @Ignore
        @Suppress("DataClassShouldBeImmutable")
        var level: Int = 0
    }

    companion object {
        const val EMPTY_ID = -1L
    }
}
