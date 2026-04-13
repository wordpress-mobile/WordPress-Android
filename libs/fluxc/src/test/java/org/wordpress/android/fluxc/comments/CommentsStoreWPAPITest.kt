package org.wordpress.android.fluxc.comments

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentsRestClient
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentsXMLRPCClient
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper

@RunWith(MockitoJUnitRunner::class)
class CommentsStoreWPAPITest {
    @Mock lateinit var restClient: CommentsRestClient
    @Mock lateinit var xmlRpcClient: CommentsXMLRPCClient
    @Mock lateinit var commentsDao: CommentsDao
    @Mock lateinit var mapper: CommentsMapper
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var appLogWrapper: AppLogWrapper
    @Mock lateinit var site: SiteModel

    private lateinit var commentsStore: CommentsStore

    @Before
    fun setUp() {
        commentsStore = CommentsStore(
                restClient,
                xmlRpcClient,
                commentsDao,
                mapper,
                initCoroutineEngine(),
                appLogWrapper,
                dispatcher
        )
        whenever(site.isUsingWpComRestApi).thenReturn(false)
        whenever(site.origin).thenReturn(SiteModel.ORIGIN_WPAPI)
    }

    @Test
    fun `fetchComments returns GENERIC_ERROR for WPAPI site`() =
        test {
            val result = commentsStore.fetchComments(
                    site = site,
                    number = NUMBER_PER_PAGE,
                    offset = 0,
                    networkStatusFilter = APPROVED
            )

            assertThat(result.isError).isTrue
            assertThat(result.error.type)
                .isEqualTo(GENERIC_ERROR)
            verifyNoInteractions(restClient)
            verifyNoInteractions(xmlRpcClient)
        }

    @Test
    fun `fetchCommentsPage returns GENERIC_ERROR for WPAPI site`() =
        test {
            val result = commentsStore.fetchCommentsPage(
                    site = site,
                    number = NUMBER_PER_PAGE,
                    offset = 0,
                    networkStatusFilter = APPROVED,
                    cacheStatuses = listOf(APPROVED)
            )

            assertThat(result.isError).isTrue
            assertThat(result.error.type)
                .isEqualTo(GENERIC_ERROR)
            verifyNoInteractions(restClient)
            verifyNoInteractions(xmlRpcClient)
        }

    @Test
    fun `pushComment returns GENERIC_ERROR for WPAPI site`() =
        test {
            val result = commentsStore.pushComment(
                    site = site,
                    comment = getDefaultComment()
            )

            assertThat(result.isError).isTrue
            assertThat(result.error.type)
                .isEqualTo(GENERIC_ERROR)
            verifyNoInteractions(restClient)
            verifyNoInteractions(xmlRpcClient)
        }

    @Test
    fun `fetchComment returns GENERIC_ERROR for WPAPI site`() =
        test {
            val result = commentsStore.fetchComment(
                    site = site,
                    remoteCommentId = REMOTE_COMMENT_ID,
                    comment = null
            )

            assertThat(result.isError).isTrue
            assertThat(result.error.type)
                .isEqualTo(GENERIC_ERROR)
            verifyNoInteractions(restClient)
            verifyNoInteractions(xmlRpcClient)
        }

    @Test
    fun `createNewComment returns GENERIC_ERROR for WPAPI site`() =
        test {
            val result = commentsStore.createNewComment(
                    site = site,
                    comment = getDefaultComment()
            )

            assertThat(result.isError).isTrue
            assertThat(result.error.type)
                .isEqualTo(GENERIC_ERROR)
            verifyNoInteractions(restClient)
            verifyNoInteractions(xmlRpcClient)
        }

    @Test
    fun `createNewReply returns GENERIC_ERROR for WPAPI site`() =
        test {
            val result = commentsStore.createNewReply(
                    site = site,
                    comment = getDefaultComment(),
                    reply = getDefaultComment()
            )

            assertThat(result.isError).isTrue
            assertThat(result.error.type)
                .isEqualTo(GENERIC_ERROR)
            verifyNoInteractions(restClient)
            verifyNoInteractions(xmlRpcClient)
        }

    @Test
    fun `updateEditComment returns GENERIC_ERROR for WPAPI site`() =
        test {
            val result = commentsStore.updateEditComment(
                    site = site,
                    comment = getDefaultComment()
            )

            assertThat(result.isError).isTrue
            assertThat(result.error.type)
                .isEqualTo(GENERIC_ERROR)
            verifyNoInteractions(restClient)
            verifyNoInteractions(xmlRpcClient)
        }

    @Test
    fun `deleteComment returns GENERIC_ERROR for WPAPI site`() =
        test {
            val result = commentsStore.deleteComment(
                    site = site,
                    remoteCommentId = REMOTE_COMMENT_ID,
                    comment = getDefaultComment()
            )

            assertThat(result.isError).isTrue
            assertThat(result.error.type)
                .isEqualTo(GENERIC_ERROR)
            verifyNoInteractions(restClient)
            verifyNoInteractions(xmlRpcClient)
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

    companion object {
        private const val NUMBER_PER_PAGE = 30
        private const val REMOTE_COMMENT_ID = 10L
    }
}
