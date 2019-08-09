package org.wordpress.android.ui.uploads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

private val POST_STATE_PUBLISH = PostStatus.PUBLISHED.toString()
private val POST_STATE_SCHEDULED = PostStatus.SCHEDULED.toString()
private val POST_STATE_PRIVATE = PostStatus.PRIVATE.toString()
private val POST_STATE_PENDING = PostStatus.PENDING.toString()
private val POST_STATE_DRAFT = PostStatus.DRAFT.toString()
private val POST_STATE_TRASHED = PostStatus.TRASHED.toString()

@RunWith(MockitoJUnitRunner::class)
class UploadActionUseCaseTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Test
    fun `auto upload action is DO NOTHING when the post is older than 2 days`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase()

        val twoDaysInSeconds = 60 * 60 * 24 * 2
        val twoDaysAgo = (Date().time / 1000) - twoDaysInSeconds

        val post = createPostModel(dateLocallyChanged = DateTimeUtils.iso8601FromTimestamp(twoDaysAgo))
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    @Test
    fun `auto upload action is REMOTE_AUTO_SAVE when the post is younger than 2 days and changes are NOT confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase()

        val twoDaysInSeconds = 60 * 60 * 24 * 2
        val twoDaysAgo = (Date().time / 1000) - twoDaysInSeconds

        val post = createPostModel(
                dateLocallyChanged = DateTimeUtils.iso8601FromTimestamp(twoDaysAgo + 99),
                changesConfirmed = false
        )
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.REMOTE_AUTO_SAVE)
    }

    @Test
    fun `auto upload action is UPLOAD when the post is younger than 2 days and changes are confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase()

        val twoDaysInSeconds = 60 * 60 * 24 * 2
        val twoDaysAgo = (Date().time / 1000) - twoDaysInSeconds

        val post = createPostModel(
                dateLocallyChanged = DateTimeUtils.iso8601FromTimestamp(twoDaysAgo + 99),
                changesConfirmed = true
        )
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    @Test
    fun `auto upload action is DO NOTHING when the post is NOT publishable`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                postUtilsWrapper = createdMockedPostUtilsWrapper(
                        isPublishable = false
                )
        )

        val post = createPostModel()
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    @Test
    fun `auto upload action is REMOTE AUTO SAVE when the post is publishable and the changes are NOT confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                postUtilsWrapper = createdMockedPostUtilsWrapper(
                        isPublishable = true
                )
        )

        val post = createPostModel(changesConfirmed = false)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.REMOTE_AUTO_SAVE)
    }

    @Test
    fun `auto upload action is UPLOAD when the post is publishable and the changes are confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                postUtilsWrapper = createdMockedPostUtilsWrapper(
                        isPublishable = true
                )
        )

        val post = createPostModel(changesConfirmed = true)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    // add similar methods for the remaining conditions

    private companion object Fixtures {
        private fun createUploadActionUseCase(
            uploadStore: UploadStore = createdMockedUploadStore(),
            postUtilsWrapper: PostUtilsWrapper = createdMockedPostUtilsWrapper(),
            uploadServiceFacade: UploadServiceFacade = createdMockedUploadServiceFacade()
        ) = UploadActionUseCase(uploadStore, postUtilsWrapper, uploadServiceFacade)

        private fun createdMockedUploadStore(numberOfPostUploadErrors: Int = 0): UploadStore {
            val uploadStore: UploadStore = mock()
            whenever(uploadStore.getNumberOfPostUploadErrorsOrCancellations(any())).thenReturn(numberOfPostUploadErrors)
            return uploadStore
        }

        private fun createdMockedPostUtilsWrapper(
            isPublishable: Boolean = true,
            isInConflict: Boolean = false
        ): PostUtilsWrapper {
            val postUtilsWrapper: PostUtilsWrapper = mock()
            whenever(postUtilsWrapper.isPublishable(any())).thenReturn(isPublishable)
            whenever(postUtilsWrapper.isPostInConflictWithRemote(any())).thenReturn(isInConflict)
            return postUtilsWrapper
        }

        private fun createdMockedUploadServiceFacade(isPostUploadingOrQueued: Boolean = false): UploadServiceFacade {
            val uploadServiceFacade: UploadServiceFacade = mock()
            whenever(uploadServiceFacade.isPostUploadingOrQueued(any())).thenReturn(isPostUploadingOrQueued)
            return uploadServiceFacade
        }

        private fun createPostModel(
            status: String = POST_STATE_DRAFT,
            isLocalDraft: Boolean = false,
            isLocallyChanged: Boolean = true,
            dateLocallyChanged: String = DateTimeUtils.iso8601FromTimestamp(Date().time / 1000),
            changesConfirmed: Boolean = false
        ): PostModel = PostModel().apply {
            this.status = status
            this.setIsLocalDraft(isLocalDraft)
            this.setIsLocallyChanged(isLocallyChanged)
            this.dateLocallyChanged = dateLocallyChanged
            if (changesConfirmed) {
                this.changesConfirmedContentHashcode = this.contentHashcode()
            }
        }

        private fun createSiteModel(isWpCom: Boolean = true) = SiteModel().apply {
            setIsWPCom(isWpCom)
        }
    }
}
