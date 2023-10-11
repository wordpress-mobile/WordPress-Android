package org.wordpress.android.fluxc.network.rest.wpcom.comment

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.common.comments.CommentsApiPayload
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse.CommentsWPComRestResponse
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.utils.CommentErrorUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class CommentsRestClient @Inject constructor(
    appContext: Context?,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val commentErrorUtilsWrapper: CommentErrorUtilsWrapper,
    private val commentsMapper: CommentsMapper
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchCommentsPage(
        site: SiteModel,
        number: Int,
        offset: Int,
        status: CommentStatus
    ): CommentsApiPayload<CommentEntityList> {
        val url = WPCOMREST.sites.site(site.siteId).comments.urlV1_1

        val params = mutableMapOf(
                "status" to status.toString(),
                "offset" to offset.toString(),
                "number" to number.toString(),
                "force" to "wpcom"
        )

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                CommentsWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(response.data.comments?.map { commentDto ->
                    commentsMapper.commentDtoToEntity(commentDto, site)
                } ?: listOf())
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun pushComment(site: SiteModel, comment: CommentEntity): CommentsApiPayload<CommentEntity> {
        val request = mutableMapOf(
                "content" to comment.content.orEmpty(),
                "date" to comment.datePublished.orEmpty(),
                "status" to comment.status.orEmpty()
        )

        return updateCommentFields(site, comment, request)
    }

    suspend fun updateEditComment(site: SiteModel, comment: CommentEntity): CommentsApiPayload<CommentEntity> {
        val request = mutableMapOf(
                "content" to comment.content.orEmpty(),
                "author" to comment.authorName.orEmpty(),
                "author_email" to comment.authorEmail.orEmpty(),
                "author_url" to comment.authorUrl.orEmpty()
        )

        return updateCommentFields(site, comment, request)
    }

    private suspend fun updateCommentFields(
        site: SiteModel,
        comment: CommentEntity,
        request: Map<String, String>
    ): CommentsApiPayload<CommentEntity> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(comment.remoteCommentId).urlV1_1

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                request,
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(commentsMapper.commentDtoToEntity(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun fetchComment(site: SiteModel, remoteCommentId: Long): CommentsApiPayload<CommentEntity> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).urlV1_1

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                mapOf(),
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(commentsMapper.commentDtoToEntity(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun deleteComment(site: SiteModel, remoteCommentId: Long): CommentsApiPayload<CommentEntity> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).delete.urlV1_1

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                null,
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(commentsMapper.commentDtoToEntity(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun createNewReply(
        site: SiteModel,
        remoteCommentId: Long,
        replayContent: String?
    ): CommentsApiPayload<CommentEntity> {
        val url = WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).replies.new_.urlV1_1

        val request = mutableMapOf(
                "content" to replayContent.orEmpty()
        )

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                request,
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(commentsMapper.commentDtoToEntity(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun createNewComment(
        site: SiteModel,
        remotePostId: Long,
        content: String?
    ): CommentsApiPayload<CommentEntity> {
        val url = WPCOMREST.sites.site(site.siteId).posts.post(remotePostId).replies.new_.urlV1_1

        val request = mutableMapOf(
                "content" to content.orEmpty()
        )

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                request,
                CommentWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(commentsMapper.commentDtoToEntity(response.data, site))
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }

    suspend fun likeComment(
        site: SiteModel,
        remoteCommentId: Long,
        isLike: Boolean
    ): CommentsApiPayload<CommentLikeWPComRestResponse> {
        val url = if (isLike) {
            WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).likes.new_.urlV1_1
        } else {
            WPCOMREST.sites.site(site.siteId).comments.comment(remoteCommentId).likes.mine.delete.urlV1_1
        }

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                null,
                CommentLikeWPComRestResponse::class.java
        )

        return when (response) {
            is Success -> {
                CommentsApiPayload(response.data)
            }
            is Error -> {
                CommentsApiPayload(commentErrorUtilsWrapper.networkToCommentError(response.error))
            }
        }
    }
}
