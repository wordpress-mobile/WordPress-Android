package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.CommentAction
import org.wordpress.android.fluxc.action.CommentsAction
import org.wordpress.android.fluxc.action.CommentsAction.CREATED_NEW_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.CREATE_NEW_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.DELETED_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.DELETE_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.FETCHED_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.FETCHED_COMMENTS
import org.wordpress.android.fluxc.action.CommentsAction.FETCH_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.FETCH_COMMENTS
import org.wordpress.android.fluxc.action.CommentsAction.LIKED_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.LIKE_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.PUSHED_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.PUSH_COMMENT
import org.wordpress.android.fluxc.action.CommentsAction.UPDATE_COMMENT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.ALL
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentsRestClient
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentsXMLRPCClient
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.INVALID_INPUT
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.CommentStore.FetchCommentsPayload
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteCreateCommentPayload
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionEntityIds
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.PagingData
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.AppLog.T.COMMENTS
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LargeClass")
@Singleton
class CommentsStore @Inject constructor(
    private val commentsRestClient: CommentsRestClient,
    private val commentsXMLRPCClient: CommentsXMLRPCClient,
    private val commentsDao: CommentsDao,
    private val commentsMapper: CommentsMapper,
    private val coroutineEngine: CoroutineEngine,
    private val appLogWrapper: AppLogWrapper,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    data class CommentsActionPayload<T>(
        val data: T? = null
    ) : Payload<CommentError>() {
        constructor(error: CommentError) : this() {
            this.error = error
        }

        constructor(error: CommentError, data: T?) : this(data) {
            this.error = error
        }
    }

    sealed class CommentsData {
        data class PagingData(val comments: CommentEntityList, val hasMore: Boolean) : CommentsData() {
            companion object {
                fun empty() = PagingData(comments = listOf(), hasMore = false)
            }
        }
        data class CommentsActionData(val comments: CommentEntityList, val rowsAffected: Int) : CommentsData()
        data class CommentsActionEntityIds(val entityIds: List<Long>, val rowsAffected: Int) : CommentsData()
        object DoNotCare : CommentsData()
    }

    suspend fun getCommentsForSite(
        site: SiteModel?,
        orderByDateAscending: Boolean,
        limit: Int,
        vararg statuses: CommentStatus
    ): CommentEntityList {
        if (site == null) return listOf()

        return commentsDao.getCommentsByLocalSiteId(
                localSiteId = site.id,
                statuses = if (statuses.asList().contains(ALL)) listOf() else statuses.map { it.toString() },
                limit = limit,
                orderAscending = orderByDateAscending
        )
    }

    suspend fun fetchComments(
        site: SiteModel,
        number: Int,
        offset: Int,
        networkStatusFilter: CommentStatus
    ): CommentsActionPayload<CommentsActionEntityIds> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.fetchCommentsPage(
                    site = site,
                    number = number,
                    offset = offset,
                    status = networkStatusFilter
            )
        } else {
            commentsXMLRPCClient.fetchCommentsPage(
                    site = site,
                    number = number,
                    offset = offset,
                    status = networkStatusFilter
            )
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error)
        } else {
            payload.response?.let { comments ->
                removeCommentGaps(site, comments, number, offset, networkStatusFilter)

                val entityIds = comments.map { comment ->
                    commentsDao.insertOrUpdateComment(comment)
                }
                CommentsActionPayload(CommentsActionEntityIds(entityIds, entityIds.size))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, "Network response was valid but empty!"))
        }
    }

    suspend fun fetchComment(
        site: SiteModel,
        remoteCommentId: Long,
        comment: CommentEntity?
    ): CommentsActionPayload<CommentsActionData> {
        val remoteCommentIdToFetch = comment?.remoteCommentId ?: remoteCommentId

        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.fetchComment(site, remoteCommentIdToFetch)
        } else {
            commentsXMLRPCClient.fetchComment(site, remoteCommentIdToFetch)
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(comment.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val cachedCommentAsList = commentsDao.insertOrUpdateCommentForResult(it)
                CommentsActionPayload(CommentsActionData(cachedCommentAsList, cachedCommentAsList.size))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, "Network response was valid but empty!"))
        }
    }

    suspend fun createNewComment(site: SiteModel, comment: CommentEntity): CommentsActionPayload<CommentsActionData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.createNewComment(site, comment.remotePostId, comment.content)
        } else {
            commentsXMLRPCClient.createNewComment(site, comment.remotePostId, comment)
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(comment.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val commentUpdated = it.copy(id = comment.id)
                val cachedCommentAsList = commentsDao.insertOrUpdateCommentForResult(commentUpdated)
                CommentsActionPayload(CommentsActionData(cachedCommentAsList, cachedCommentAsList.size))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, "Network response was valid but empty!"))
        }
    }

    suspend fun createNewReply(
        site: SiteModel,
        comment: CommentEntity,
        reply: CommentEntity
    ): CommentsActionPayload<CommentsActionData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.createNewReply(site, comment.remoteCommentId, reply.content)
        } else {
            commentsXMLRPCClient.createNewReply(site, comment, reply)
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(reply.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val commentUpdated = it.copy(id = reply.id)
                val cachedCommentAsList = commentsDao.insertOrUpdateCommentForResult(commentUpdated)
                CommentsActionPayload(CommentsActionData(cachedCommentAsList, cachedCommentAsList.size))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, "Network response was valid but empty!"))
        }
    }

    suspend fun pushComment(site: SiteModel, comment: CommentEntity): CommentsActionPayload<CommentsActionData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.pushComment(site, comment)
        } else {
            commentsXMLRPCClient.pushComment(site, comment)
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(comment.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val commentUpdated = it.copy(id = comment.id)
                val cachedCommentAsList = commentsDao.insertOrUpdateCommentForResult(commentUpdated)
                CommentsActionPayload(CommentsActionData(cachedCommentAsList, cachedCommentAsList.size))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, "Network response was valid but empty!"))
        }
    }

    suspend fun updateEditComment(site: SiteModel, comment: CommentEntity): CommentsActionPayload<CommentsActionData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.updateEditComment(site, comment)
        } else {
            commentsXMLRPCClient.updateEditComment(site, comment)
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(comment.toListOrEmpty(), 0))
        } else {
            payload.response?.let {
                val commentUpdated = it.copy(id = comment.id)
                val cachedCommentAsList = commentsDao.insertOrUpdateCommentForResult(commentUpdated)
                CommentsActionPayload(CommentsActionData(cachedCommentAsList, cachedCommentAsList.size))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, "Network response was valid but empty!"))
        }
    }

    @Suppress("ComplexMethod", "ReturnCount")
    suspend fun deleteComment(
        site: SiteModel,
        remoteCommentId: Long,
        comment: CommentEntity?
    ): CommentsActionPayload<CommentsActionData> {
        // If the comment is stored locally, we want to update it locally (needed because in some
        // cases we use this to update comments by remote id).
        val commentToDelete = comment ?: commentsDao.getCommentsByLocalSiteAndRemoteCommentId(
                site.id,
                remoteCommentId
        ).firstOrNull()

        val remoteCommentIdToDelete = commentToDelete?.remoteCommentId ?: remoteCommentId

        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.deleteComment(site, remoteCommentIdToDelete)
        } else {
            commentsXMLRPCClient.deleteComment(site, remoteCommentIdToDelete)
        }

        if (payload.isError) {
            return CommentsActionPayload(payload.error, CommentsActionData(commentToDelete.toListOrEmpty(), 0))
        } else {
            val targetComment = when {
                site.isUsingWpComRestApi && payload.response == null -> {
                    return CommentsActionPayload(CommentError(
                            INVALID_RESPONSE,
                            "Network response was valid but empty!"
                    ))
                }
                site.isUsingWpComRestApi && payload.response != null -> {
                    val commentFromEndpoint: CommentEntity = payload.response
                    commentToDelete?.let { entity ->
                        commentFromEndpoint.copy(id = entity.id)
                    } ?: commentFromEndpoint
                }
                else -> { // this means !site.isUsingWpComRestApi is true
                    // This is ugly but the XMLRPC response doesn't contain any info about the update comment.
                    // So we're copying the logic here: if the comment status was "trash" before and the delete
                    // call is successful, then we want to delete this comment. Setting the "deleted" status
                    // will ensure the comment is deleted in the rest of the logic.
                    commentToDelete?.let {
                        it.copy(
                                status = if (DELETED.toString() == it.status || TRASH.toString() == it.status) {
                                    DELETED.toString()
                                } else {
                                    TRASH.toString()
                                }
                        )
                    }
                }
            }

            return targetComment?.let {
                // Delete once means "send to trash", so we don't want to remove it from the DB, just update it's
                // status. Delete twice means "farewell comment, we won't see you ever again". Only delete from the
                // DB if the status is "deleted".
                val deletedCommentAsList = if (it.status?.equals(DELETED.toString()) == true) {
                    commentsDao.deleteComment(it)
                    it.toListOrEmpty()
                } else {
                    // Update the local copy, only the status should have changed ("trash")
                    commentsDao.insertOrUpdateCommentForResult(it)
                }

                CommentsActionPayload(CommentsActionData(deletedCommentAsList, deletedCommentAsList.size))
            } ?: CommentsActionPayload(CommentsActionData(listOf(), 0))
        }
    }

    suspend fun likeComment(
        site: SiteModel,
        remoteCommentId: Long,
        comment: CommentEntity?,
        isLike: Boolean
    ): CommentsActionPayload<CommentsActionData> {
        // If the comment is stored locally, we want to update it locally (needed because in some
        // cases we use this to update comments by remote id).
        val commentToLike = comment ?: commentsDao.getCommentsByLocalSiteAndRemoteCommentId(
                site.id,
                remoteCommentId
        ).firstOrNull()
        val remoteCommentIdToLike = commentToLike?.remoteCommentId ?: remoteCommentId

        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.likeComment(site, remoteCommentIdToLike, isLike)
        } else {
            return CommentsActionPayload(
                    CommentError(
                            INVALID_INPUT,
                            "Can't like a comment on XMLRPC API"
                    ),
                    CommentsActionData(
                            commentToLike.toListOrEmpty(),
                            0
                    )
            )
        }

        return if (payload.isError) {
            CommentsActionPayload(payload.error, CommentsActionData(commentToLike.toListOrEmpty(), 0))
        } else {
            payload.response?.let { endpointResponse ->
                val updatedComment = commentToLike?.copy(iLike = endpointResponse.i_like)

                val (rowsAffected, likedCommentAsList) = updatedComment?.let {
                    Pair(1, commentsDao.insertOrUpdateCommentForResult(it))
                } ?: Pair(0, updatedComment.toListOrEmpty())

                CommentsActionPayload(CommentsActionData(likedCommentAsList, rowsAffected))
            } ?: CommentsActionPayload(CommentError(INVALID_RESPONSE, "Network response was valid but empty!"))
        }
    }

    suspend fun updateComment(
        isError: Boolean,
        commentId: Long,
        comment: CommentEntity
    ): CommentsActionPayload<CommentsActionEntityIds> {
        val (entityId, rowsAffected) = if (isError) {
            Pair(commentId, 0)
        } else {
            Pair(commentsDao.insertOrUpdateComment(comment), 1)
        }

        return CommentsActionPayload(CommentsActionEntityIds(listOf(entityId), rowsAffected))
    }

    suspend fun fetchCommentsPage(
        site: SiteModel,
        number: Int,
        offset: Int,
        networkStatusFilter: CommentStatus,
        cacheStatuses: List<CommentStatus>
    ): CommentsActionPayload<PagingData> {
        val payload = if (site.isUsingWpComRestApi) {
            commentsRestClient.fetchCommentsPage(
                    site = site,
                    number = number,
                    offset = offset,
                    status = networkStatusFilter
            )
        } else {
            commentsXMLRPCClient.fetchCommentsPage(
                    site = site,
                    number = number,
                    offset = offset,
                    status = networkStatusFilter
            )
        }

        return if (payload.isError) {
            val cachedComments = if (offset > 0) {
                commentsDao.getCommentsByLocalSiteId(
                        localSiteId = site.id,
                        statuses = cacheStatuses.map { it.toString() },
                        limit = offset,
                        orderAscending = false
                )
            } else {
                listOf()
            }
            CommentsActionPayload(payload.error, PagingData(
                    comments = cachedComments,
                    hasMore = cachedComments.isNotEmpty()
            ))
        } else {
            val comments = payload.response?.map { it } ?: listOf()

            removeCommentGaps(site, comments, number, offset, networkStatusFilter)

            commentsDao.appendOrUpdateComments(comments = comments)

            val cachedComments = commentsDao.getCommentsByLocalSiteId(
                    localSiteId = site.id,
                    statuses = cacheStatuses.map { it.toString() },
                    limit = offset + comments.size,
                    orderAscending = false
            )

            CommentsActionPayload(PagingData(comments = cachedComments, hasMore = comments.size == number))
        }
    }

    suspend fun moderateCommentLocally(
        site: SiteModel,
        remoteCommentId: Long,
        newStatus: CommentStatus
    ): CommentsActionPayload<CommentsActionData> {
        val comment = commentsDao.getCommentsByLocalSiteAndRemoteCommentId(
                site.id,
                remoteCommentId
        ).firstOrNull() ?: return CommentsActionPayload(CommentError(INVALID_INPUT, "Comment cannot be null!"))

        val commentToModerate = comment.copy(status = newStatus.toString())
        val cachedCommentAsList = commentsDao.insertOrUpdateCommentForResult(commentToModerate)

        return CommentsActionPayload(CommentsActionData(
                comments = cachedCommentAsList,
                rowsAffected = cachedCommentAsList.size
        ))
    }

    suspend fun getCommentByLocalId(localId: Long) = commentsDao.getCommentById(localId)

    suspend fun getCommentByLocalSiteAndRemoteId(localSiteId: Int, remoteCommentId: Long) =
            commentsDao.getCommentsByLocalSiteAndRemoteCommentId(localSiteId, remoteCommentId)

    suspend fun pushLocalCommentByRemoteId(
        site: SiteModel,
        remoteCommentId: Long
    ): CommentsActionPayload<CommentsActionData> {
        val comment = commentsDao.getCommentsByLocalSiteAndRemoteCommentId(
                site.id,
                remoteCommentId
        ).firstOrNull() ?: return CommentsActionPayload(CommentError(INVALID_INPUT, "Comment cannot be null!"))

        return pushComment(site, comment)
    }

    suspend fun getCachedComments(
        site: SiteModel,
        cacheStatuses: List<CommentStatus>,
        imposeHasMore: Boolean
    ): CommentsActionPayload<PagingData> {
        val cachedComments = commentsDao.getFilteredComments(
                localSiteId = site.id,
                statuses = cacheStatuses.map { it.toString() }
        )

        return CommentsActionPayload(PagingData(comments = cachedComments, imposeHasMore))
    }

    @Deprecated(
            "Action and event bus support should be gradually replaced while the Comments Unification project proceeds"
    )
    override fun onRegister() {
        // We cannot use the AppLogWrapper here since it's still null at this point
        AppLog.d(API, this.javaClass.name + ": onRegister")
    }

    @Deprecated(
            "Action and event bus support should be gradually replaced while the Comments Unification project proceeds"
    )
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? CommentsAction ?: return

        when (actionType) {
            FETCH_COMMENTS -> {
                coroutineEngine.launch(API, this, "CommentsStore: On FETCH_COMMENTS") {
                    emitChange(onFetchComments(action.payload as FetchCommentsPayload))
                }
            }
            FETCH_COMMENT -> {
                coroutineEngine.launch(API, this, "CommentsStore: On FETCH_COMMENT") {
                    emitChange(onFetchComment(action.payload as RemoteCommentPayload))
                }
            }
            CREATE_NEW_COMMENT -> {
                coroutineEngine.launch(API, this, "CommentsStore: On CREATE_NEW_COMMENT") {
                    emitChange(onCreateNewComment(action.payload as RemoteCreateCommentPayload))
                }
            }
            PUSH_COMMENT -> {
                coroutineEngine.launch(API, this, "CommentsStore: On PUSH_COMMENT") {
                    emitChange(onPushComment(action.payload as RemoteCommentPayload))
                }
            }
            DELETE_COMMENT -> {
                coroutineEngine.launch(API, this, "CommentsStore: On DELETE_COMMENT") {
                    emitChange(onDeleteComment(action.payload as RemoteCommentPayload))
                }
            }
            LIKE_COMMENT -> {
                coroutineEngine.launch(API, this, "CommentsStore: On LIKE_COMMENT") {
                    emitChange(onLikeComment(action.payload as RemoteLikeCommentPayload))
                }
            }
            UPDATE_COMMENT -> {
                coroutineEngine.launch(API, this, "CommentsStore: On UPDATE_COMMENT") {
                    emitChange(onUpdateComment(action.payload as CommentModel))
                }
            }
            FETCHED_COMMENTS,
            FETCHED_COMMENT,
            CREATED_NEW_COMMENT,
            PUSHED_COMMENT,
            DELETED_COMMENT,
            LIKED_COMMENT -> throw IllegalArgumentException(
                    "CommentsStore > onAction: received illegal action type [$actionType]"
            )
        }
    }

    @Deprecated(
            "Action and event bus support should be gradually replaced while the Comments Unification project proceeds",
            ReplaceWith("use fetchComments suspend fun directly")
    )
    private suspend fun onFetchComments(payload: FetchCommentsPayload): OnCommentChanged {
        val response = fetchComments(
                site = payload.site,
                number = payload.number,
                offset = payload.offset,
                networkStatusFilter = payload.status
        )

        return createOnCommentChangedEvent(
                response.data?.rowsAffected.orNone(),
                CommentAction.FETCH_COMMENTS,
                response.error,
                response.data.toCommentIdsListOrEmpty(),
                payload.status,
                payload.offset
        )
    }

    @Deprecated(
        message = "Action and event bus support should be gradually replaced while the " +
                "Comments Unification project proceeds",
        replaceWith = ReplaceWith("use fetchComment suspend fun directly")
    )
    private suspend fun onFetchComment(payload: RemoteCommentPayload): OnCommentChanged {
        val response = fetchComment(
                payload.site,
                payload.remoteCommentId,
                payload.comment?.let { commentsMapper.commentLegacyModelToEntity(it) }
        )

        return createOnCommentChangedEvent(
                response.data?.rowsAffected.orNone(),
                CommentAction.FETCH_COMMENT,
                response.error,
                response.data.toCommentIdsListOrEmpty()
        )
    }

    @Deprecated(
            message = "Action and event bus support should be gradually replaced while the " +
                    "Comments Unification project proceeds",
            replaceWith = ReplaceWith("use updateComment suspend fun directly")
    )
    private suspend fun onUpdateComment(payload: CommentModel): OnCommentChanged {
        val response = updateComment(
                isError = payload.isError,
                commentId = payload.id.toLong(),
                comment = commentsMapper.commentLegacyModelToEntity(payload)
        )

        return createOnCommentChangedEvent(
                response.data?.rowsAffected.orNone(),
                CommentAction.UPDATE_COMMENT,
                null,
                response.data.toCommentIdsListOrEmpty()
        )
    }

    @Deprecated(
            message = "Action and event bus support should be gradually replaced while the " +
                    "Comments Unification project proceeds",
            replaceWith = ReplaceWith("use deleteComment suspend fun directly")
    )
    private suspend fun onDeleteComment(payload: RemoteCommentPayload): OnCommentChanged {
        val response = deleteComment(
                payload.site,
                payload.remoteCommentId,
                payload.comment?.let { commentsMapper.commentLegacyModelToEntity(it) }
        )

        return createOnCommentChangedEvent(
                // Keeping here the rowsAffected set to 0 as it is in original handleDeletedCommentResponse
                0,
                CommentAction.DELETE_COMMENT,
                response.error,
                response.data.toCommentIdsListOrEmpty()
        )
    }

    @Deprecated(
            message = "Action and event bus support should be gradually replaced while the " +
                    "Comments Unification project proceeds",
            replaceWith = ReplaceWith("use likeComment suspend fun directly")
    )
    private suspend fun onLikeComment(payload: RemoteLikeCommentPayload): OnCommentChanged {
        val response = likeComment(
                payload.site,
                payload.remoteCommentId,
                payload.comment?.let { commentsMapper.commentLegacyModelToEntity(it) },
                payload.like
        )

        if (!response.isError) {
            response.data?.comments?.firstOrNull()?.let { entity ->
                payload.comment?.apply {
                    this.iLike = entity.iLike
                }
            }
        }

        return createOnCommentChangedEvent(
                response.data?.rowsAffected.orNone(),
                CommentAction.LIKE_COMMENT,
                response.error,
                response.data.toCommentIdsListOrEmpty()
        )
    }

    @Deprecated(
            message = "Action and event bus support should be gradually replaced while the " +
                    "Comments Unification project proceeds",
            replaceWith = ReplaceWith("use pushComment suspend fun directly")
    )
    private suspend fun onPushComment(payload: RemoteCommentPayload): OnCommentChanged {
        if (payload.comment == null) {
            return OnCommentChanged(0, CommentAction.PUSH_COMMENT).apply {
                this.error = CommentError(INVALID_INPUT, "Comment can't be null")
            }
        }

        val response = pushComment(
                payload.site,
                commentsMapper.commentLegacyModelToEntity(payload.comment)
        )

        return createOnCommentChangedEvent(
                response.data?.rowsAffected.orNone(),
                CommentAction.PUSH_COMMENT,
                response.error,
                response.data.toCommentIdsListOrEmpty()
        )
    }

    @Deprecated(
            message = "Action and event bus support should be gradually replaced while the " +
                    "Comments Unification project proceeds",
            replaceWith = ReplaceWith("use createNewComment suspend fun directly")
    )
    @Suppress("UseCheckOrError")
    private suspend fun onCreateNewComment(payload: RemoteCreateCommentPayload): OnCommentChanged {
        val response = if (payload.post != null && payload.reply == null) {
            // Create a new comment on a specific Post
            createNewComment(
                    payload.site,
                    commentsMapper.commentLegacyModelToEntity(payload.comment)
            )
        } else if (payload.reply != null && payload.post == null) {
            // Create a new reply to a specific Comment
            createNewReply(
                    payload.site,
                    commentsMapper.commentLegacyModelToEntity(payload.comment),
                    commentsMapper.commentLegacyModelToEntity(payload.reply)
            )
        } else {
            throw IllegalStateException(
                "Either post or reply must be not null and both can't be not null at the same time!"
            )
        }

        return createOnCommentChangedEvent(
                response.data?.rowsAffected.orNone(),
                CommentAction.CREATE_NEW_COMMENT,
                response.error,
                response.data.toCommentIdsListOrEmpty()
        )
    }

    private fun createOnCommentChangedEvent(
        rowsAffected: Int,
        actionType: CommentAction,
        error: CommentError?,
        commentLocalIds: List<Int>,
        status: CommentStatus? = null,
        offset: Int? = null
    ): OnCommentChanged {
        return OnCommentChanged(rowsAffected, actionType).apply {
            this.changedCommentsLocalIds.addAll(commentLocalIds)
            this.error = error
            status?.let {
                this.requestedStatus = it
            }
            offset?.let {
                this.offset = offset
            }
        }
    }

    private fun CommentsActionData?.toCommentIdsListOrEmpty(): List<Int> {
        return this?.comments?.map { it.id.toInt() } ?: listOf()
    }

    private fun CommentsActionEntityIds?.toCommentIdsListOrEmpty(): List<Int> {
        return this?.entityIds?.map { it.toInt() } ?: listOf()
    }

    private fun CommentEntity?.toListOrEmpty(): List<CommentEntity> {
        return this?.let {
            listOf(it)
        } ?: listOf()
    }

    private fun Int?.orNone(): Int {
        return this ?: 0
    }

    @Suppress("LongMethod", "ReturnCount")
    private suspend fun removeCommentGaps(
        site: SiteModel?,
        commentsList: CommentEntityList?,
        maxEntriesInResponse: Int,
        requestOffset: Int,
        vararg statuses: CommentStatus
    ): Int {
        if (site == null || commentsList == null) {
            return 0
        }

        val targetStatuses = if (listOf(*statuses).contains(ALL)) {
            listOf(APPROVED, UNAPPROVED)
        } else {
            listOf(*statuses)
        }.map { it.toString() }

        appLogWrapper.d(
                COMMENTS,
                "removeCommentGaps -> siteId [${site.siteId}]  targetStatuses [$targetStatuses]"
        )

        if (commentsList.isEmpty()) {
            return if (requestOffset == 0) {
                val numOfDeletedComments = commentsDao.clearAllBySiteIdAndFilters(
                        localSiteId = site.id,
                        statuses = targetStatuses
                )

                appLogWrapper.d(
                        COMMENTS,
                        "removeCommentGaps -> commentsList empty deleted $numOfDeletedComments items"
                )

                numOfDeletedComments
            } else {
                appLogWrapper.d(
                        COMMENTS,
                        "removeCommentGaps -> commentsList empty and requestOffset != 0"
                )
                0
            }
        }

        val comments = mutableListOf<CommentEntity>().apply { addAll(commentsList) }

        comments.sortWith { o1, o2 ->
            val x = o2.publishedTimestamp
            val y = o1.publishedTimestamp
            when {
                x < y -> -1
                x == y -> 0
                else -> 1
            }
        }

        val remoteIds = comments.map { it.remoteCommentId }

        val startOfRange = comments.first().publishedTimestamp
        val endOfRange = comments.last().publishedTimestamp

        appLogWrapper.d(
                COMMENTS,
                "removeCommentGaps -> startOfRange [" + startOfRange + " - " +
                        comments.first().datePublished + "] " + "endOfRange [" + endOfRange + " - " +
                        comments.last().datePublished + "]"
        )

        var numOfDeletedComments = 0

        // try to trim comments from the top
        if (requestOffset == 0) {
            numOfDeletedComments += commentsDao.removeGapsFromTheTop(
                    localSiteId = site.id,
                    statuses = targetStatuses,
                    remoteIds = remoteIds,
                    startOfRange = startOfRange
            )
            appLogWrapper.d(
                    COMMENTS,
                    "removeCommentGaps -> requestOffset == 0 -> numOfDeletedComments $numOfDeletedComments"
            )
        }

        // try to trim comments from the bottom
        if (comments.size < maxEntriesInResponse) {
            numOfDeletedComments += commentsDao.removeGapsFromTheBottom(
                    localSiteId = site.id,
                    statuses = targetStatuses,
                    remoteIds = remoteIds,
                    endOfRange = endOfRange
            )
            appLogWrapper.d(
                    COMMENTS,
                    "removeCommentGaps -> comments.size() [" +
                            comments.size + "] < maxEntriesInResponse [" +
                            maxEntriesInResponse + "]" + "-> numOfDeletedComments " + numOfDeletedComments
            )
        }

        // remove comments from the middle
        numOfDeletedComments += commentsDao.removeGapsFromTheMiddle(
                localSiteId = site.id,
                statuses = targetStatuses,
                remoteIds = remoteIds,
                startOfRange = startOfRange,
                endOfRange = endOfRange
        )

        appLogWrapper.d(
                COMMENTS,
                "removeCommentGaps -> removing from middle -> numOfDeletedComments $numOfDeletedComments"
        )

        return numOfDeletedComments
    }
}
