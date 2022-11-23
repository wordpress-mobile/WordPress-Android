package org.wordpress.android.fluxc.network.rest.wpcom.comments

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentLikeWPComRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentParent
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse.CommentsWPComRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentsRestClient
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.CommentErrorUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class CommentsRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var commentErrorUtilsWrapper: CommentErrorUtilsWrapper
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var commentsMapper: CommentsMapper

    private lateinit var restClient: CommentsRestClient
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var bodyCaptor: KArgumentCaptor<Map<String, Any>>

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        bodyCaptor = argumentCaptor()

        restClient = CommentsRestClient(
                appContext = null,
                dispatcher = dispatcher,
                requestQueue = requestQueue,
                accessToken = accessToken,
                userAgent = userAgent,
                wpComGsonRequestBuilder = wpComGsonRequestBuilder,
                commentErrorUtilsWrapper = commentErrorUtilsWrapper,
                commentsMapper = commentsMapper
        )
        whenever(site.siteId).thenReturn(SITE_ID)
    }

    @Test
    fun `fetchCommentsPage returns fetched page`() = test {
        val response = getDefaultDto()

        val commentsReponse = response.CommentsWPComRestResponse()
        commentsReponse.comments = listOf(response, response, response)

        whenever(commentsMapper.commentDtoToEntity(response, site)).thenReturn(response.toEntity())

        initFetchPageResponse(commentsReponse)

        val payload = restClient.fetchCommentsPage(
                site = site,
                number = PAGE_LEN,
                offset = 0,
                status = APPROVED
        )

        assertThat(payload.isError).isFalse

        val comments = payload.response!!
        assertThat(comments.size).isEqualTo(commentsReponse.comments.size)
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/${site.siteId}/comments/"
        )
        assertThat(paramsCaptor.lastValue).isEqualTo(
                mutableMapOf(
                        "status" to APPROVED.toString(),
                        "offset" to 0.toString(),
                        "number" to PAGE_LEN.toString(),
                        "force" to "wpcom"
                )
        )
    }

    @Test
    fun `fetchCommentsPage returns an error on API fail`() = test {
        val errorMessage = "this is an error"
        whenever(commentErrorUtilsWrapper.networkToCommentError(any()))
                .thenReturn(CommentError(INVALID_RESPONSE, errorMessage))

        initFetchPageResponse(error = WPComGsonNetworkError(
                BaseNetworkError(
                        NETWORK_ERROR,
                        errorMessage,
                        VolleyError(errorMessage)
                )
        ))

        val payload = restClient.fetchCommentsPage(
                site = site,
                number = PAGE_LEN,
                offset = 0,
                status = APPROVED
        )

        assertThat(payload.isError).isTrue
        assertThat(payload.error.type).isEqualTo(INVALID_RESPONSE)
        assertThat(payload.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `pushComment returns updated comment`() = test {
        val response = getDefaultDto()
        val comment = response.toEntity()

        whenever(commentsMapper.commentDtoToEntity(response, site)).thenReturn(comment)
        initPushResponse(response)

        val payload = restClient.pushComment(
                site = site,
                comment = comment
        )

        assertThat(payload.isError).isFalse
        val commentResponse = payload.response!!
        assertThat(commentResponse).isEqualTo(comment)
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/${site.siteId}/comments/${comment.remoteCommentId}/"
        )
        assertThat(bodyCaptor.lastValue).isEqualTo(
                mutableMapOf(
                        "content" to comment.content.orEmpty(),
                        "date" to comment.datePublished.orEmpty(),
                        "status" to comment.status.orEmpty()
                )
        )
    }

    @Test
    fun `pushComment returns an error on API fail`() = test {
        val errorMessage = "this is an error"
        val comment = getDefaultDto().toEntity()
        whenever(commentErrorUtilsWrapper.networkToCommentError(any()))
                .thenReturn(CommentError(INVALID_RESPONSE, errorMessage))

        initPushResponse(error = WPComGsonNetworkError(
                BaseNetworkError(
                        NETWORK_ERROR,
                        errorMessage,
                        VolleyError(errorMessage)
                )
        ))

        val payload = restClient.pushComment(
                site = site,
                comment = comment
        )

        assertThat(payload.isError).isTrue
        assertThat(payload.error.type).isEqualTo(INVALID_RESPONSE)
        assertThat(payload.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `updateEditComment returns updated comment`() = test {
        val response = getDefaultDto()
        val comment = response.toEntity()

        whenever(commentsMapper.commentDtoToEntity(response, site)).thenReturn(comment)
        initPushResponse(response)

        val payload = restClient.updateEditComment(
                site = site,
                comment = comment
        )

        assertThat(payload.isError).isFalse
        val commentResponse = payload.response!!
        assertThat(commentResponse).isEqualTo(comment)
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/${site.siteId}/comments/${comment.remoteCommentId}/"
        )
        assertThat(bodyCaptor.lastValue).isEqualTo(
                mutableMapOf(
                        "content" to comment.content.orEmpty(),
                        "author" to comment.authorName.orEmpty(),
                        "author_email" to comment.authorEmail.orEmpty(),
                        "author_url" to comment.authorUrl.orEmpty()
                )
        )
    }

    @Test
    fun `updateEditComment returns an error on API fail`() = test {
        val errorMessage = "this is an error"
        val comment = getDefaultDto().toEntity()
        whenever(commentErrorUtilsWrapper.networkToCommentError(any()))
                .thenReturn(CommentError(INVALID_RESPONSE, errorMessage))

        initPushResponse(error = WPComGsonNetworkError(
                BaseNetworkError(
                        NETWORK_ERROR,
                        errorMessage,
                        VolleyError(errorMessage)
                )
        ))

        val payload = restClient.updateEditComment(
                site = site,
                comment = comment
        )

        assertThat(payload.isError).isTrue
        assertThat(payload.error.type).isEqualTo(INVALID_RESPONSE)
        assertThat(payload.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `fetchComment returns comment`() = test {
        val response = getDefaultDto()
        val comment = response.toEntity()

        whenever(commentsMapper.commentDtoToEntity(response, site)).thenReturn(comment)

        initFetchResponse(response)

        val payload = restClient.fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(payload.isError).isFalse

        val commentResponse = payload.response!!
        assertThat(commentResponse).isEqualTo(comment)
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/${site.siteId}/comments/${comment.remoteCommentId}/"
        )
        assertThat(paramsCaptor.lastValue).isEqualTo(
                mapOf<String, String>()
        )
    }

    @Test
    fun `fetchComment returns an error on API fail`() = test {
        val errorMessage = "this is an error"
        val comment = getDefaultDto().toEntity()

        whenever(commentErrorUtilsWrapper.networkToCommentError(any()))
                .thenReturn(CommentError(INVALID_RESPONSE, errorMessage))

        initFetchResponse(error = WPComGsonNetworkError(
                BaseNetworkError(
                        NETWORK_ERROR,
                        errorMessage,
                        VolleyError(errorMessage)
                )
        ))

        val payload = restClient.fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(payload.isError).isTrue
        assertThat(payload.error.type).isEqualTo(INVALID_RESPONSE)
        assertThat(payload.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `deleteComment returns deleted comment`() = test {
        val response = getDefaultDto()
        val comment = response.toEntity()

        whenever(commentsMapper.commentDtoToEntity(response, site)).thenReturn(comment)
        initDeleteResponse(response)

        val payload = restClient.deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(payload.isError).isFalse
        val commentResponse = payload.response!!
        assertThat(commentResponse).isEqualTo(comment)
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/${site.siteId}/comments/" +
                        "${comment.remoteCommentId}/delete/"
        )
        assertThat(bodyCaptor.lastValue).isNull()
    }

    @Test
    fun `deleteComment returns an error on API fail`() = test {
        val errorMessage = "this is an error"
        val comment = getDefaultDto().toEntity()
        whenever(commentErrorUtilsWrapper.networkToCommentError(any()))
                .thenReturn(CommentError(INVALID_RESPONSE, errorMessage))

        initDeleteResponse(error = WPComGsonNetworkError(
                BaseNetworkError(
                        NETWORK_ERROR,
                        errorMessage,
                        VolleyError(errorMessage)
                )
        ))

        val payload = restClient.deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(payload.isError).isTrue
        assertThat(payload.error.type).isEqualTo(INVALID_RESPONSE)
        assertThat(payload.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `createNewReply returns reply comment`() = test {
        val response = getDefaultDto()
        val comment = response.toEntity()

        whenever(commentsMapper.commentDtoToEntity(response, site)).thenReturn(comment)
        initReplyCreateResponse(response)

        val payload = restClient.createNewReply(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment.content
        )

        assertThat(payload.isError).isFalse
        val commentResponse = payload.response!!
        assertThat(commentResponse).isEqualTo(comment)
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/" +
                        "${site.siteId}/comments/${comment.remoteCommentId}/replies/new/"
        )
        assertThat(bodyCaptor.lastValue).isEqualTo(
                mutableMapOf("content" to comment.content.orEmpty())
        )
    }

    @Test
    fun `createNewReply returns an error on API fail`() = test {
        val errorMessage = "this is an error"
        val comment = getDefaultDto().toEntity()
        whenever(commentErrorUtilsWrapper.networkToCommentError(any()))
                .thenReturn(CommentError(INVALID_RESPONSE, errorMessage))

        initReplyCreateResponse(error = WPComGsonNetworkError(
                BaseNetworkError(
                        NETWORK_ERROR,
                        errorMessage,
                        VolleyError(errorMessage)
                )
        ))

        val payload = restClient.createNewReply(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment.content
        )

        assertThat(payload.isError).isTrue
        assertThat(payload.error.type).isEqualTo(INVALID_RESPONSE)
        assertThat(payload.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `createNewComment returns new comment`() = test {
        val response = getDefaultDto()
        val comment = response.toEntity()

        whenever(commentsMapper.commentDtoToEntity(response, site)).thenReturn(comment)
        initReplyCreateResponse(response)

        val payload = restClient.createNewComment(
                site = site,
                remotePostId = comment.remotePostId,
                comment.content
        )

        assertThat(payload.isError).isFalse
        val commentResponse = payload.response!!
        assertThat(commentResponse).isEqualTo(comment)
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/" +
                        "${site.siteId}/posts/${comment.remotePostId}/replies/new/"
        )
        assertThat(bodyCaptor.lastValue).isEqualTo(
                mutableMapOf("content" to comment.content.orEmpty())
        )
    }

    @Test
    fun `createNewComment returns an error on API fail`() = test {
        val errorMessage = "this is an error"
        val comment = getDefaultDto().toEntity()
        whenever(commentErrorUtilsWrapper.networkToCommentError(any()))
                .thenReturn(CommentError(INVALID_RESPONSE, errorMessage))

        initReplyCreateResponse(error = WPComGsonNetworkError(
                BaseNetworkError(
                        NETWORK_ERROR,
                        errorMessage,
                        VolleyError(errorMessage)
                )
        ))

        val payload = restClient.createNewComment(
                site = site,
                remotePostId = comment.remotePostId,
                comment.content
        )

        assertThat(payload.isError).isTrue
        assertThat(payload.error.type).isEqualTo(INVALID_RESPONSE)
        assertThat(payload.error.message).isEqualTo(errorMessage)
    }

    @Test
    fun `likeComment returns new liked comment`() = test {
        val comment = getDefaultDto().toEntity()
        val response = getDefaultLikeResponse(comment.iLike)

        initLikeResponse(response)

        val payload = restClient.likeComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment.iLike
        )

        assertThat(payload.isError).isFalse
        val commentResponse = payload.response!!
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/" +
                        "${site.siteId}/comments/${comment.remoteCommentId}/likes/new/"
        )
        assertThat(bodyCaptor.lastValue).isNull()
    }

    @Test
    fun `likeComment returns new unliked comment`() = test {
        val comment = getDefaultDto().toEntity().copy(iLike = false)
        val response = getDefaultLikeResponse(comment.iLike)

        initLikeResponse(response)

        val payload = restClient.likeComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment.iLike
        )

        assertThat(payload.isError).isFalse
        assertThat(urlCaptor.lastValue).isEqualTo(
                "https://public-api.wordpress.com/rest/v1.1/sites/" +
                        "${site.siteId}/comments/${comment.remoteCommentId}/likes/mine/delete/"
        )
        assertThat(bodyCaptor.lastValue).isNull()
    }

    @Test
    fun `likeComment returns an error on API fail`() = test {
        val errorMessage = "this is an error"
        val comment = getDefaultDto().toEntity()
        whenever(commentErrorUtilsWrapper.networkToCommentError(any()))
                .thenReturn(CommentError(INVALID_RESPONSE, errorMessage))

        initLikeResponse(error = WPComGsonNetworkError(
                BaseNetworkError(
                        NETWORK_ERROR,
                        errorMessage,
                        VolleyError(errorMessage)
                )
        ))

        val payload = restClient.likeComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment.iLike
        )

        assertThat(payload.isError).isTrue
        assertThat(payload.error.type).isEqualTo(INVALID_RESPONSE)
        assertThat(payload.error.message).isEqualTo(errorMessage)
    }

    private suspend fun initFetchPageResponse(
        data: CommentsWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<CommentsWPComRestResponse> {
        return initGetResponse(CommentsWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initPushResponse(
        data: CommentWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<CommentWPComRestResponse> {
        return initPostResponse(CommentWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initFetchResponse(
        data: CommentWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<CommentWPComRestResponse> {
        return initGetResponse(CommentWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initDeleteResponse(
        data: CommentWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<CommentWPComRestResponse> {
        return initPostResponse(CommentWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initReplyCreateResponse(
        data: CommentWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<CommentWPComRestResponse> {
        return initPostResponse(CommentWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun initLikeResponse(
        data: CommentLikeWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<CommentLikeWPComRestResponse> {
        return initPostResponse(CommentLikeWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun <T> initGetResponse(
        kclass: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        val response = if (error != null) Response.Error(error) else Success(data)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(kclass),
                        any(),
                        any(),
                        any()

                )
        ).thenReturn(response)
        return response
    }

    private suspend fun <T> initPostResponse(
        kclass: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        val response = if (error != null) Response.Error(error) else Success(data)
        whenever(
                wpComGsonRequestBuilder.syncPostRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        bodyCaptor.capture(),
                        eq(kclass),
                        anyOrNull()
                )
        ).thenReturn(response)
        return response
    }

    private fun getDefaultDto(): CommentWPComRestResponse {
        return CommentWPComRestResponse().apply {
            ID = 137
            URL = "https://test-site.wordpress.com/2021/02/25/again/#comment-137"
            author = Author().apply {
                ID = 0
                URL = "https://debugging-test.wordpress.com"
                avatar_URL = "https://gravatar.com/avatar/avatarurl"
                email = "email@mydomain.com"
                name = "This is my name"
            }
            content = "example content"
            date = "2021-05-12T15:10:40+02:00"
            i_like = true
            parent = CommentParent().apply { ID = 41 }
            post = Post().apply {
                ID = 85
                link = "https://public-api.wordpress.com/rest/v1.1/sites/11111111/posts/85"
                title = "again"
                type = "post"
            }
            status = "approved"
        }
    }

    private fun getDefaultLikeResponse(iLike: Boolean): CommentLikeWPComRestResponse {
        return CommentLikeWPComRestResponse().apply {
            success = true
            i_like = iLike
            like_count = 100
        }
    }

    private fun CommentWPComRestResponse.toEntity(): CommentEntity {
        val dto = this
        return CommentEntity(
                id = 0,
                remoteCommentId = dto.ID,
                remotePostId = dto.post.ID,
                authorId = dto.author.ID,
                localSiteId = 10,
                remoteSiteId = 200,
                authorUrl = dto.author.URL,
                authorName = dto.author.name,
                authorEmail = dto.author.email,
                authorProfileImageUrl = dto.author.avatar_URL,
                postTitle = dto.post.title,
                status = dto.status,
                datePublished = dto.date,
                publishedTimestamp = 132456,
                content = dto.content,
                url = dto.URL,
                hasParent = dto.parent != null,
                parentId = dto.parent.ID,
                iLike = dto.i_like
        )
    }

    companion object {
        private const val SITE_ID = 200L
        private const val PAGE_LEN = 30
    }
}
