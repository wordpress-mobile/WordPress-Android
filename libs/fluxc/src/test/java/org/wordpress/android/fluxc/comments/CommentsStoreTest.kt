package org.wordpress.android.fluxc.comments

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.common.comments.CommentsApiPayload
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentLikeWPComRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentsRestClient
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentsXMLRPCClient
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionEntityIds
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.PagingData
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper

@RunWith(MockitoJUnitRunner::class)
class CommentsStoreTest {
    @Mock lateinit var restClient: CommentsRestClient
    @Mock lateinit var xmlRpcClient: CommentsXMLRPCClient
    @Mock lateinit var commentsDao: CommentsDao
    @Mock lateinit var mapper: CommentsMapper
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var site: SiteModel
    @Mock lateinit var appLogWrapper: AppLogWrapper

    private lateinit var commentsStore: CommentsStore
    private val commentError = CommentError(GENERIC_ERROR, "")

    @Before
    fun setUp() {
        commentsStore = CommentsStore(
                commentsRestClient = restClient,
                commentsXMLRPCClient = xmlRpcClient,
                commentsDao = commentsDao,
                commentsMapper = mapper,
                coroutineEngine = initCoroutineEngine(),
                dispatcher = dispatcher,
                appLogWrapper = appLogWrapper
        )
        whenever(site.id).thenReturn(SITE_LOCAL_ID)
        whenever(site.isUsingWpComRestApi).thenReturn(true)

        test {
            whenever(commentsDao.removeGapsFromTheTop(any(), any(), any(), any())).thenReturn(0)
            whenever(commentsDao.removeGapsFromTheBottom(any(), any(), any(), any())).thenReturn(0)
            whenever(commentsDao.removeGapsFromTheMiddle(any(), any(), any(), any(), any())).thenReturn(0)
        }
    }

    @Test
    fun `getCommentsForSite returns comments from cache`() = test {
        commentsStore.getCommentsForSite(
                site = site,
                orderByDateAscending = false,
                limit = -1,
                statuses = listOf(APPROVED).toTypedArray()
        )

        verify(commentsDao, times(1)).getCommentsByLocalSiteId(
                site.id,
                listOf(APPROVED.toString()),
                -1,
                false
        )
    }

    @Test
    fun `fetchComments returns fetched ids for WPCom`() = test {
        val comments = getDefaultCommentList()
        whenever(restClient.fetchCommentsPage(any(), any(), any(), any())).thenReturn(CommentsApiPayload(comments))
        whenever(commentsDao.insertOrUpdateComment(any())).thenReturn(10)

        val result = commentsStore.fetchComments(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                networkStatusFilter = APPROVED
        )

        verify(restClient, times(1)).fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                status = APPROVED
        )

        assertThat((result.data as CommentsActionEntityIds).entityIds.size).isEqualTo(comments.size)
    }

    @Test
    fun `fetchComments returns fetched ids for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comments = getDefaultCommentList()
        whenever(xmlRpcClient.fetchCommentsPage(any(), any(), any(), any())).thenReturn(CommentsApiPayload(comments))
        whenever(commentsDao.insertOrUpdateComment(any())).thenReturn(10)

        val result = commentsStore.fetchComments(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                networkStatusFilter = APPROVED
        )

        verify(xmlRpcClient, times(1)).fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                status = APPROVED
        )

        assertThat((result.data as CommentsActionEntityIds).entityIds.size).isEqualTo(comments.size)
    }

    @Test
    fun `fetchComments returns error on failure`() = test {
        whenever(restClient.fetchCommentsPage(any(), any(), any(), any())).thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.fetchComments(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                networkStatusFilter = APPROVED
        )

        verify(restClient, times(1)).fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                status = APPROVED
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `fetchComment returns fetched comment for WPCom`() = test {
        val comment = getDefaultCommentList().first()
        whenever(restClient.fetchComment(any(), any())).thenReturn(CommentsApiPayload(comment))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(comment))

        val result = commentsStore.fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment
        )

        verify(restClient, times(1)).fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(comment)
    }

    @Test
    fun `fetchComment returns fetched comment for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comment = getDefaultCommentList().first()
        whenever(xmlRpcClient.fetchComment(any(), any())).thenReturn(CommentsApiPayload(comment))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(comment))

        val result = commentsStore.fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment
        )

        verify(xmlRpcClient, times(1)).fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(comment)
    }

    @Test
    fun `fetchComment returns error on failure`() = test {
        val comment = getDefaultCommentList().first()
        whenever(restClient.fetchComment(any(), any())).thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment
        )

        verify(restClient, times(1)).fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `createNewComment returns new comment for WPCom`() = test {
        val comment = getDefaultCommentList().first()
        whenever(restClient.createNewComment(any(), anyLong(), anyOrNull())).thenReturn(CommentsApiPayload(comment))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(comment))

        val result = commentsStore.createNewComment(
                site = site,
                comment
        )

        verify(restClient, times(1)).createNewComment(
                site = site,
                remotePostId = comment.remotePostId,
                content = comment.content
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(comment)
    }

    @Test
    fun `createNewComment returns new comment for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comment = getDefaultCommentList().first()
        whenever(xmlRpcClient.createNewComment(any(), anyLong(), any())).thenReturn(CommentsApiPayload(comment))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(comment))

        val result = commentsStore.createNewComment(
                site = site,
                comment
        )

        verify(xmlRpcClient, times(1)).createNewComment(
                site = site,
                remotePostId = comment.remotePostId,
                comment = comment
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(comment)
    }

    @Test
    fun `createNewComment returns error on failure`() = test {
        val comment = getDefaultCommentList().first()
        whenever(restClient.createNewComment(any(), anyLong(), anyOrNull()))
                .thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.createNewComment(
                site = site,
                comment
        )

        verify(restClient, times(1)).createNewComment(
                site = site,
                remotePostId = comment.remotePostId,
                content = comment.content
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `createNewReply returns new reply for WPCom`() = test {
        val comment = getDefaultCommentList().first()
        val reply = comment.copy(id = comment.id + 1, content = "this is a reply")
        whenever(restClient.createNewReply(any(), anyLong(), anyOrNull())).thenReturn(CommentsApiPayload(reply))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(reply))

        val result = commentsStore.createNewReply(
                site = site,
                comment,
                reply
        )

        verify(restClient, times(1)).createNewReply(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                replayContent = reply.content
        )

        assertThat((result.data as CommentsActionData).comments.first().content).isEqualTo(reply.content)
    }

    @Test
    fun `createNewReply returns new reply for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comment = getDefaultCommentList().first()
        val reply = comment.copy(id = comment.id + 1, content = "this is a reply")
        whenever(xmlRpcClient.createNewReply(any(), any(), any())).thenReturn(CommentsApiPayload(reply))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(reply))

        val result = commentsStore.createNewReply(
                site = site,
                comment,
                reply
        )

        verify(xmlRpcClient, times(1)).createNewReply(
                site = site,
                comment = comment,
                reply = reply
        )

        assertThat((result.data as CommentsActionData).comments.first().content).isEqualTo(reply.content)
    }

    @Test
    fun `createNewReply returns error on failure`() = test {
        val comment = getDefaultCommentList().first()
        val reply = comment.copy(id = comment.id + 1, content = "this is a reply")
        whenever(restClient.createNewReply(any(), anyLong(), anyOrNull())).thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.createNewReply(
                site = site,
                comment = comment,
                reply = reply
        )

        verify(restClient, times(1)).createNewReply(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                replayContent = reply.content
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `pushComment returns updated comment for WPCom`() = test {
        val comment = getDefaultCommentList().first().copy(id = 220)
        val commentFromEndpoint = comment.copy(id = 0)
        whenever(restClient.pushComment(any(), any())).thenReturn(CommentsApiPayload(commentFromEndpoint))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(comment))

        val result = commentsStore.pushComment(
                site = site,
                comment
        )

        verify(restClient, times(1)).pushComment(
                site = site,
                comment = comment
        )
        verify(commentsDao, times(1)).insertOrUpdateCommentForResult(
                comment
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(comment)
    }

    @Test
    fun `pushComment returns updated comment for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comment = getDefaultCommentList().first().copy(id = 220)
        val commentFromEndpoint = comment.copy(id = 0)
        whenever(xmlRpcClient.pushComment(any(), any())).thenReturn(CommentsApiPayload(commentFromEndpoint))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(comment))

        val result = commentsStore.pushComment(
                site = site,
                comment
        )

        verify(xmlRpcClient, times(1)).pushComment(
                site = site,
                comment = comment
        )
        verify(commentsDao, times(1)).insertOrUpdateCommentForResult(
                comment
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(comment)
    }

    @Test
    fun `pushComment returns error on failure`() = test {
        val comment = getDefaultCommentList().first()
        whenever(restClient.pushComment(any(), any())).thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.pushComment(
                site = site,
                comment = comment
        )

        verify(restClient, times(1)).pushComment(
                site = site,
                comment = comment
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `updateEditComment returns updated comment for WPCom`() = test {
        val comment = getDefaultCommentList().first().copy(id = 220)
        val commentFromEndpoint = comment.copy(id = 0)
        whenever(restClient.updateEditComment(any(), any())).thenReturn(CommentsApiPayload(commentFromEndpoint))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(comment))

        val result = commentsStore.updateEditComment(
                site = site,
                comment
        )

        verify(restClient, times(1)).updateEditComment(
                site = site,
                comment = comment
        )
        verify(commentsDao, times(1)).insertOrUpdateCommentForResult(
                comment
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(comment)
    }

    @Test
    fun `updateEditComment returns updated comment for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comment = getDefaultCommentList().first().copy(id = 220)
        val commentFromEndpoint = comment.copy(id = 0)
        whenever(xmlRpcClient.updateEditComment(any(), any())).thenReturn(CommentsApiPayload(commentFromEndpoint))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(comment))

        val result = commentsStore.updateEditComment(
                site = site,
                comment
        )

        verify(xmlRpcClient, times(1)).updateEditComment(
                site = site,
                comment = comment
        )
        verify(commentsDao, times(1)).insertOrUpdateCommentForResult(
                comment
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(comment)
    }

    @Test
    fun `updateEditComment returns error on failure`() = test {
        val comment = getDefaultCommentList().first()
        whenever(restClient.updateEditComment(any(), any())).thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.updateEditComment(
                site = site,
                comment = comment
        )

        verify(restClient, times(1)).updateEditComment(
                site = site,
                comment = comment
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `deleteComment returns updated comment for WPCom`() = test {
        val comment = getDefaultCommentList().first()
        val commentApiResponse = comment.copy(status = DELETED.toString(), id = 0)
        whenever(restClient.deleteComment(any(), anyLong())).thenReturn(CommentsApiPayload(commentApiResponse))
        whenever(commentsDao.deleteComment(any())).thenReturn(1)

        val result = commentsStore.deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment
        )

        verify(restClient, times(1)).deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        val deletedComment = commentApiResponse.copy(id = comment.id)

        verify(commentsDao, times(1)).deleteComment(
                deletedComment
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(deletedComment)
    }

    @Test
    fun `deleteComment returns updated comment for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comment = getDefaultCommentList().first().copy(status = TRASH.toString())
        val commentApiResponse = comment.copy(status = DELETED.toString(), id = 0)
        whenever(xmlRpcClient.deleteComment(any(), anyLong())).thenReturn(CommentsApiPayload(commentApiResponse))
        whenever(commentsDao.deleteComment(any())).thenReturn(1)

        val result = commentsStore.deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment
        )

        verify(xmlRpcClient, times(1)).deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        val deletedComment = commentApiResponse.copy(id = comment.id)

        verify(commentsDao, times(1)).deleteComment(
                deletedComment
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(deletedComment)
    }

    @Test
    fun `deleteComment returns error on failure`() = test {
        val comment = getDefaultCommentList().first()
        whenever(restClient.deleteComment(any(), anyLong())).thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment
        )

        verify(restClient, times(1)).deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `likeComment returns updated comment for WPCom`() = test {
        val comment = getDefaultCommentList().first().copy(id = 220, iLike = false)
        val commentApiResponse = comment.copy(iLike = true)
        whenever(restClient.likeComment(any(), anyLong(), anyBoolean())).thenReturn(CommentsApiPayload(
                CommentLikeWPComRestResponse().apply { i_like = true }
        ))
        whenever(commentsDao.insertOrUpdateCommentForResult(any())).thenReturn(listOf(commentApiResponse))

        val result = commentsStore.likeComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment,
                isLike = commentApiResponse.iLike
        )

        verify(restClient, times(1)).likeComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                isLike = commentApiResponse.iLike

        )
        verify(commentsDao, times(1)).insertOrUpdateCommentForResult(
                commentApiResponse
        )

        assertThat((result.data as CommentsActionData).comments.first()).isEqualTo(commentApiResponse)
    }

    @Test
    fun `likeComment give error for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comment = getDefaultCommentList().first().copy(id = 220, iLike = false)
        val commentApiResponse = comment.copy(iLike = true)

        val result = commentsStore.likeComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment,
                isLike = commentApiResponse.iLike
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `likeComment returns error on failure`() = test {
        val comment = getDefaultCommentList().first().copy(id = 220, iLike = false)
        val commentApiResponse = comment.copy(iLike = true)
        whenever(restClient.likeComment(any(), anyLong(), anyBoolean())).thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.likeComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                comment = comment,
                isLike = commentApiResponse.iLike
        )

        verify(restClient, times(1)).likeComment(
                site = site,
                remoteCommentId = comment.remoteCommentId,
                isLike = commentApiResponse.iLike
        )

        assertThat(result.isError).isTrue
    }

    @Test
    fun `fetchCommentsPage returns fetched paging data for WPCom`() = test {
        val comments = getDefaultCommentList()
        whenever(restClient.fetchCommentsPage(any(), any(), any(), any())).thenReturn(CommentsApiPayload(comments))
        whenever(commentsDao.appendOrUpdateComments(any())).thenReturn(comments.size)
        whenever(commentsDao.getCommentsByLocalSiteId(anyInt(), any(), anyInt(), anyBoolean())).thenReturn(comments)

        val result = commentsStore.fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                networkStatusFilter = APPROVED,
                cacheStatuses = listOf(APPROVED)
        )

        verify(restClient, times(1)).fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                status = APPROVED
        )

        verify(commentsDao, times(1)).getCommentsByLocalSiteId(
                localSiteId = site.id,
                statuses = listOf(APPROVED.toString()),
                limit = 0 + comments.size,
                orderAscending = false
        )

        assertThat((result.data as PagingData).comments).isEqualTo(comments)
    }

    @Test
    fun `fetchCommentsPage returns fetched paging data for Self-Hosted`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        val comments = getDefaultCommentList()
        whenever(xmlRpcClient.fetchCommentsPage(any(), any(), any(), any())).thenReturn(CommentsApiPayload(comments))
        whenever(commentsDao.appendOrUpdateComments(any())).thenReturn(comments.size)
        whenever(commentsDao.getCommentsByLocalSiteId(anyInt(), any(), anyInt(), anyBoolean())).thenReturn(comments)

        val result = commentsStore.fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                networkStatusFilter = APPROVED,
                cacheStatuses = listOf(APPROVED)
        )

        verify(xmlRpcClient, times(1)).fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                status = APPROVED
        )

        verify(commentsDao, times(1)).getCommentsByLocalSiteId(
                localSiteId = site.id,
                statuses = listOf(APPROVED.toString()),
                limit = 0 + comments.size,
                orderAscending = false
        )

        assertThat((result.data as PagingData).comments).isEqualTo(comments)
    }

    @Test
    fun `fetchCommentsPage returns error on failure`() = test {
        whenever(restClient.fetchCommentsPage(any(), any(), any(), any())).thenReturn(CommentsApiPayload(commentError))

        val result = commentsStore.fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                networkStatusFilter = APPROVED,
                cacheStatuses = listOf(APPROVED)
        )

        verify(restClient, times(1)).fetchCommentsPage(
                site = site,
                number = NUMBER_PER_PAGE,
                offset = 0,
                status = APPROVED
        )

        assertThat(result.isError).isTrue
    }

    private fun getDefaultComment() = CommentEntity(
            id = 1,
            remoteCommentId = 10,
            remotePostId = 100,
            authorId = 1_000,
            localSiteId = 10_000,
            remoteSiteId = 100_000,
            authorUrl = null,
            authorName = null,
            authorEmail = null,
            authorProfileImageUrl = null,
            postTitle = null,
            status = APPROVED.toString(),
            datePublished = null,
            publishedTimestamp = 1_000_000,
            content = null,
            url = null,
            hasParent = false,
            parentId = 10_000_000,
            iLike = false
    )

    private fun getDefaultCommentList(): CommentEntityList {
        val comment = getDefaultComment()
        return listOf(
                comment.copy(
                        id = 1,
                        remoteCommentId = 10,
                        datePublished = "2021-07-24T00:51:43+02:00",
                        status = APPROVED.toString()
                ),
                comment.copy(
                        id = 2,
                        remoteCommentId = 20,
                        datePublished = "2021-07-24T00:52:43+02:00",
                        status = UNAPPROVED.toString()
                ),
                comment.copy(
                        id = 3,
                        remoteCommentId = 30,
                        datePublished = "2021-07-24T00:53:43+02:00",
                        status = APPROVED.toString()
                ),
                comment.copy(
                        id = 4,
                        remoteCommentId = 40,
                        datePublished = "2021-07-24T00:54:43+02:00",
                        status = SPAM.toString()
                )
        )
    }

    companion object {
        private const val SITE_LOCAL_ID = 200
        private const val NUMBER_PER_PAGE = 30
    }
}
