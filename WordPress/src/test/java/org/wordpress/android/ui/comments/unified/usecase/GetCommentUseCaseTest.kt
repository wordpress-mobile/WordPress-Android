package org.wordpress.android.ui.comments.unified.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.CommentsActionData
import org.wordpress.android.ui.comments.utils.generateMockComments

@ExperimentalCoroutinesApi
class GetCommentUseCaseTest : BaseUnitTest() {
    private val commentsStore: CommentsStore = mock()

    private val classToTest = GetCommentUseCase(commentsStore)

    private val siteModel = SiteModel()

    private val remoteCommentId = 12345L

    private val commentEntityList = generateMockComments(1)

    @Test
    fun `Should get LOCAL comment if found in local DB`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId))
            .thenReturn(generateMockComments(1))
        classToTest.execute(siteModel, remoteCommentId)
        verify(commentsStore, times(1)).getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId)
    }

    @Test
    fun `Should return LOCAL comment if found in local DB`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId))
            .thenReturn(commentEntityList)
        val actual = classToTest.execute(siteModel, remoteCommentId)
        val expected = commentEntityList.first()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should fetch REMOTE comment if comment NOT found in local DB`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId))
            .thenReturn(emptyList())
        whenever(commentsStore.fetchComment(siteModel, remoteCommentId, null))
            .thenReturn(CommentsActionPayload(CommentsActionData(commentEntityList, 0)))
        classToTest.execute(siteModel, remoteCommentId)
        verify(commentsStore, times(1)).fetchComment(siteModel, remoteCommentId, null)
    }

    @Test
    fun `Should return REMOTE comment if comment NOT found in local DB`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId))
            .thenReturn(emptyList())
        whenever(commentsStore.fetchComment(siteModel, remoteCommentId, null))
            .thenReturn(CommentsActionPayload(CommentsActionData(commentEntityList, 0)))
        val actual = classToTest.execute(siteModel, remoteCommentId)
        val expected = commentEntityList.first()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return null if LOCAL and REMOTE comment entity lists are EMPTY`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId))
            .thenReturn(emptyList())
        whenever(commentsStore.fetchComment(siteModel, remoteCommentId, null))
            .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        val actual = classToTest.execute(siteModel, remoteCommentId)
        val expected = null
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return null if get LOCAL is null and fetch REMOTE is EMPTY`() = test {
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId))
            .thenReturn(null)
        whenever(commentsStore.fetchComment(siteModel, remoteCommentId, null))
            .thenReturn(CommentsActionPayload(CommentsActionData(emptyList(), 0)))
        val actual = classToTest.execute(siteModel, remoteCommentId)
        val expected = null
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return the FIRST element of comment entity LOCAL list`() = test {
        val mockComments = generateMockComments(2)
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId))
            .thenReturn(mockComments)
        classToTest.execute(siteModel, remoteCommentId)
        val actual = classToTest.execute(siteModel, remoteCommentId)
        val expected = mockComments.first()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should return the FIRST element of comment entity REMOTE list`() = test {
        val mockComments = generateMockComments(2)
        whenever(commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId))
            .thenReturn(emptyList())
        whenever(commentsStore.fetchComment(siteModel, remoteCommentId, null))
            .thenReturn(CommentsActionPayload(CommentsActionData(mockComments, 0)))
        classToTest.execute(siteModel, remoteCommentId)
        val actual = classToTest.execute(siteModel, remoteCommentId)
        val expected = mockComments.first()
        assertThat(actual).isEqualTo(expected)
    }
}
