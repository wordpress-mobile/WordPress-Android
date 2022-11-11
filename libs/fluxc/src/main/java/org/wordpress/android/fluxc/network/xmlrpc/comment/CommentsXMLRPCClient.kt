package org.wordpress.android.fluxc.network.xmlrpc.comment

import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.common.comments.CommentsApiPayload
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Success
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.utils.CommentErrorUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CommentsXMLRPCClient @Inject constructor(
    dispatcher: Dispatcher?,
    @Named("custom-ssl") requestQueue: RequestQueue?,
    userAgent: UserAgent?,
    httpAuthManager: HTTPAuthManager?,
    private val commentErrorUtilsWrapper: CommentErrorUtilsWrapper,
    private val xmlrpcRequestBuilder: XMLRPCRequestBuilder,
    private val commentsMapper: CommentsMapper
) : BaseXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager) {
    suspend fun fetchCommentsPage(
        site: SiteModel,
        number: Int,
        offset: Int,
        status: CommentStatus
    ): CommentsApiPayload<CommentEntityList> {
        val params: MutableList<Any> = ArrayList()

        val commentParams = mutableMapOf<String, Any>(
                "number" to number,
                "offset" to offset
        )

        if (status != CommentStatus.ALL) {
            commentParams["status"] = getXMLRPCCommentStatus(status)
        }

        params.add(site.selfHostedSiteId)
        params.add(site.notNullUserName())
        params.add(site.notNullPassword())
        params.add(commentParams)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.GET_COMMENTS,
                params = params,
                clazz = Array<Any>::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(commentsMapper.commentXmlRpcDTOToEntityList(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun pushComment(site: SiteModel, comment: CommentEntity): CommentsApiPayload<CommentEntity> {
        val commentParams = mutableMapOf<String, Any?>(
                "content" to comment.content,
                "date" to comment.datePublished,
                "status" to getXMLRPCCommentStatus(CommentStatus.fromString(comment.status))
        )

        return updateCommentFields(site, comment, commentParams)
    }

    suspend fun updateEditComment(site: SiteModel, comment: CommentEntity): CommentsApiPayload<CommentEntity> {
        val commentParams = mutableMapOf<String, Any?>(
                "content" to comment.content,
                "author" to comment.authorName,
                "author_email" to comment.authorEmail,
                "author_url" to comment.authorUrl
        )

        return updateCommentFields(site, comment, commentParams)
    }

    private suspend fun updateCommentFields(
        site: SiteModel,
        comment: CommentEntity,
        commentParams: Map<String, Any?>
    ): CommentsApiPayload<CommentEntity> {
        val params: MutableList<Any> = ArrayList()

        params.add(site.selfHostedSiteId)
        params.add(site.notNullUserName())
        params.add(site.notNullPassword())
        params.add(comment.remoteCommentId)
        params.add(commentParams)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.EDIT_COMMENT,
                params = params,
                clazz = Any::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(comment)
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun fetchComment(site: SiteModel, remoteCommentId: Long): CommentsApiPayload<CommentEntity> {
        val params: MutableList<Any> = ArrayList()

        params.add(site.selfHostedSiteId)
        params.add(site.notNullUserName())
        params.add(site.notNullPassword())
        params.add(remoteCommentId)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.GET_COMMENT,
                params = params,
                clazz = Map::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(commentsMapper.commentXmlRpcDTOToEntity(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun deleteComment(site: SiteModel, remoteCommentId: Long): CommentsApiPayload<CommentEntity?> {
        val params: MutableList<Any> = ArrayList()

        params.add(site.selfHostedSiteId)
        params.add(site.notNullUserName())
        params.add(site.notNullPassword())
        params.add(remoteCommentId)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.DELETE_COMMENT,
                params = params,
                clazz = Any::class.java
        )

        return when (response) {
            is Success -> {
                // This is ugly but the XMLRPC response doesn't contain any info about the updated comment.
                CommentsApiPayload(null)
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun createNewReply(
        site: SiteModel,
        comment: CommentEntity,
        reply: CommentEntity
    ): CommentsApiPayload<CommentEntity> {
        val commentParams = mutableMapOf<String, Any?>(
                "content" to reply.content,
                "comment_parent" to comment.remoteCommentId
        )

        if (reply.authorName != null) {
            commentParams["author"] = reply.authorName
        }

        if (reply.authorUrl != null) {
            commentParams["author_url"] = reply.authorUrl
        }

        if (reply.authorEmail != null) {
            commentParams["author_email"] = reply.authorEmail
        }

        return newComment(site, comment.remotePostId, reply, comment.remoteCommentId, commentParams)
    }

    suspend fun createNewComment(
        site: SiteModel,
        remotePostId: Long,
        comment: CommentEntity
    ): CommentsApiPayload<CommentEntity> {
        val commentParams = mutableMapOf<String, Any?>(
                "content" to comment.content
        )

        if (comment.parentId != 0L) {
            commentParams["comment_parent"] = comment.parentId
        }
        if (comment.authorName != null) {
            commentParams["author"] = comment.authorName
        }
        if (comment.authorUrl != null) {
            commentParams["author_url"] = comment.authorUrl
        }
        if (comment.authorEmail != null) {
            commentParams["author_email"] = comment.authorEmail
        }

        return newComment(site, remotePostId, comment, comment.parentId, commentParams)
    }

    private suspend fun newComment(
        site: SiteModel,
        remotePostId: Long,
        comment: CommentEntity,
        parentId: Long,
        commentParams: Map<String, Any?>
    ): CommentsApiPayload<CommentEntity> {
        val params: MutableList<Any> = ArrayList()

        params.add(site.selfHostedSiteId)
        params.add(site.notNullUserName())
        params.add(site.notNullPassword())
        params.add(remotePostId)
        params.add(commentParams)

        val response = xmlrpcRequestBuilder.syncGetRequest(
                restClient = this,
                url = site.xmlRpcUrl,
                method = XMLRPC.NEW_COMMENT,
                params = params,
                clazz = Any::class.java
        )

        return when (response) {
            is Success -> {
                if (response.data is Int) {
                    val newComment = comment.copy(
                            parentId = parentId,
                            remoteCommentId = response.data.toLong()
                    )
                    CommentsApiPayload(newComment)
                } else {
                    val newComment = comment.copy(parentId = parentId)
                    CommentsApiPayload(CommentError(GENERIC_ERROR, ""), newComment)
                }
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error), comment)
            }
        }
    }

    private fun getXMLRPCCommentStatus(status: CommentStatus): String {
        return when (status) {
            APPROVED -> "approve"
            UNAPPROVED -> "hold"
            SPAM -> "spam"
            TRASH -> "trash"
            else -> "approve"
        }
    }

    // This functions are part of a containment fix to avoid a crash happening in the Jetpack app for My Site > Comments
    // on self-hosted sites not having the full Jetpack plugin but only one of the standalone plugins (like the
    // jetpack backup plugin). This only avoids the crash allowing the relevant error to be displayed.
    // For sites like those, the full rest api is not available but the username and password are actually null as well.
    // This creates some not consistent behaviours in various areas of the app that needs a more broad fix and review
    // (more details in the internal p2 post and comments pe8j1f-V-p2); numbers of such cases are pretty low actually
    // and this fix prioritizes the mentioned crash.
    private fun SiteModel.notNullUserName() = this.username ?: ""
    private fun SiteModel.notNullPassword() = this.password ?: ""
}
