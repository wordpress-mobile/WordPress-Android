package org.wordpress.android.ui.uploads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import java.util.function.Consumer

private val POST_STATE_DRAFT = PostStatus.DRAFT.toString()

@RunWith(MockitoJUnitRunner::class)
class UploadActionUseCaseTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    @Test
    fun `uploadAction is UPLOAD when changes confirmed`() {
        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(changesConfirmed = true)
        // Act
        val action = uploadActionUseCase.getUploadAction(post)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    @Test
    fun `uploadAction is UPLOAD_AS_DRAFT when changes not confirmed and is local draft`() {
        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(changesConfirmed = false, isLocalDraft = true)
        // Act
        val action = uploadActionUseCase.getUploadAction(post)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD_AS_DRAFT)
    }

    @Test
    fun `uploadAction is REMOTE_AUTO_SAVE when changes not confirmed and is not local draft`() {
        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(changesConfirmed = false, isLocalDraft = false)
        // Act
        val action = uploadActionUseCase.getUploadAction(post)

        // Assert
        assertThat(action).isEqualTo(UploadAction.REMOTE_AUTO_SAVE)
    }

    @Test
    fun `uploadAction is DO_NOTHING when the post does not contain any local changes and is not local draft`() {
        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(isLocallyChanged = false, isLocalDraft = false)
        // Act
        val action = uploadActionUseCase.getUploadAction(post)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    @Test
    fun `autoUploadAction is DO NOTHING when the post is older than 2 days`() {
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
    fun `autoUploadAction is DO NOTHING when the post is older than 2 days no matter other parameters`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase()

        val twoDaysAgoTimestamp = run {
            val twoDaysInSeconds = 60 * 60 * 24 * 2
            val twoDaysAgo = (Date().time / 1000) - twoDaysInSeconds
            DateTimeUtils.iso8601FromTimestamp(twoDaysAgo)
        }

        val posts = listOf(
                createPostModel(dateLocallyChanged = twoDaysAgoTimestamp),
                createPostModel(dateLocallyChanged = twoDaysAgoTimestamp, isLocalDraft = true, changesConfirmed = true),
                createPostModel(dateLocallyChanged = twoDaysAgoTimestamp, isLocalDraft = false, changesConfirmed = true)
        )

        val siteModel: SiteModel = createSiteModel()

        // Act and Assert
        assertThat(posts).allSatisfy(Consumer { post ->
            val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

            assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
        })
    }

    @Test
    fun `autoUploadAction is REMOTE_AUTO_SAVE when the post is younger than 2 days and changes are NOT confirmed`() {
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
    fun `autoUploadAction is UPLOAD when the post is younger than 2 days and changes are confirmed`() {
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
    fun `autoUploadAction is DO NOTHING when the post is NOT publishable`() {
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
    fun `autoUploadAction is REMOTE AUTO SAVE when the post is publishable and the changes are NOT confirmed`() {
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
    fun `autoUploadAction is UPLOAD when the post is publishable and the changes are confirmed`() {
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

    @Test
    fun `autoUploadAction is DO NOTHING when the post is in conflict`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                postUtilsWrapper = createdMockedPostUtilsWrapper(
                        isInConflict = true
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
    fun `autoUploadAction is REMOTE AUTO SAVE when the post is not in conflict and the changes are NOT confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                postUtilsWrapper = createdMockedPostUtilsWrapper(
                        isInConflict = false
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
    fun `autoUploadAction is UPLOAD when the post is not in conflict and the changes are confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                postUtilsWrapper = createdMockedPostUtilsWrapper(
                        isInConflict = false
                )
        )

        val post = createPostModel(changesConfirmed = true)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    @Test
    fun `autoUploadAction is DO NOTHING when upload failed MAXIMUM_AUTO_UPLOAD_RETRIES and NOT confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadStore = createdMockedUploadStore(MAXIMUM_AUTO_UPLOAD_RETRIES)
        )

        val post = createPostModel(changesConfirmed = false)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    @Test
    fun `autoUploadAction is DO_NOTHING when upload failed MAXIMUM_AUTO_UPLOAD_RETRIES+1 and changes confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadStore = createdMockedUploadStore(MAXIMUM_AUTO_UPLOAD_RETRIES)
        )

        val post = createPostModel(changesConfirmed = true)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    @Test
    fun `autoUploadAction is REMOTE AUTO SAVE when failed MAXIMUM_AUTO_UPLOAD_RETRIES - 1 and changes NOT confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadStore = createdMockedUploadStore(MAXIMUM_AUTO_UPLOAD_RETRIES - 1)
        )

        val post = createPostModel(changesConfirmed = false)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.REMOTE_AUTO_SAVE)
    }

    @Test
    fun `autoUploadAction is REMOTE AUTO SAVE when upload hasn't failed and the changes are NOT confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadStore = createdMockedUploadStore(0)
        )

        val post = createPostModel(changesConfirmed = false)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.REMOTE_AUTO_SAVE)
    }

    @Test
    fun `autoUploadAction is UPLOAD when upload hasn't failed and the changes are confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadStore = createdMockedUploadStore(0)
        )

        val post = createPostModel(changesConfirmed = true)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    @Test
    fun `autoUploadAction is UPLOAD when upload failed MAXIMUM_AUTO_UPLOAD_RETRIES - 1 and changes are confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadStore = createdMockedUploadStore(MAXIMUM_AUTO_UPLOAD_RETRIES - 1)
        )

        val post = createPostModel(changesConfirmed = true)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    @Test
    fun `autoUploadAction is DO NOTHING when the post is uploading or queued`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadServiceFacade = createdMockedUploadServiceFacade(isPostUploadingOrQueued = true)
        )

        val post = createPostModel()
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    @Test
    fun `autoUploadAction is REMOTE AUTO SAVE when not uploading or queued and the changes are NOT confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadServiceFacade = createdMockedUploadServiceFacade(isPostUploadingOrQueued = false)
        )

        val post = createPostModel(changesConfirmed = false)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.REMOTE_AUTO_SAVE)
    }

    @Test
    fun `autoUploadAction is UPLOAD when the post is not uploading or queued and the changes are confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                uploadServiceFacade = createdMockedUploadServiceFacade(isPostUploadingOrQueued = false)
        )

        val post = createPostModel(changesConfirmed = true)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    @Test
    fun `autoUploadAction is DO NOTHING when the site is NOT wpcom and the changes are NOT confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(changesConfirmed = false)
        val siteModel: SiteModel = createSiteModel(isWpCom = false)
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    @Test
    fun `autoUploadAction is UPLOAD when the site is NOT wpcom and the changes are confirmed`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(changesConfirmed = true)
        val siteModel: SiteModel = createSiteModel(isWpCom = false)
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    @Test
    fun `autoUploadAction is DO_NOTHING when changes were already remote-auto-saved and are not confirmed`() {
        // Arrange

        val timestampNow = Date().time / 1000
        val dateNow = DateTimeUtils.iso8601FromTimestamp(timestampNow)
        val datePast = DateTimeUtils.iso8601FromTimestamp(timestampNow - 9999)

        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(changesConfirmed = false, autoSaveModified = dateNow, dateLocallyChanged = datePast)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    @Test
    fun `autoUploadAction is REMOTE_AUTO_SAVE when changes were NOT remote-auto-saved and are not confirmed`() {
        // Arrange

        val timestampNow = Date().time / 1000
        val dateNow = DateTimeUtils.iso8601FromTimestamp(timestampNow)
        val datePast = DateTimeUtils.iso8601FromTimestamp(timestampNow - 9999)

        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(changesConfirmed = false, autoSaveModified = datePast, dateLocallyChanged = dateNow)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.REMOTE_AUTO_SAVE)
    }

    @Test
    fun `autoUploadAction is UPLOAD when changes were already remote-auto-saved but are confirmed now`() {
        // Arrange

        val timestampNow = Date().time / 1000
        val dateNow = DateTimeUtils.iso8601FromTimestamp(timestampNow)
        val datePast = DateTimeUtils.iso8601FromTimestamp(timestampNow - 9999)

        val uploadActionUseCase = createUploadActionUseCase()

        val post = createPostModel(changesConfirmed = true, autoSaveModified = dateNow, dateLocallyChanged = datePast)
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.UPLOAD)
    }

    @Test
    fun `autoUploadAction is DO_NOTHING when the post is currently being edited`() {
        // Arrange
        val uploadActionUseCase = createUploadActionUseCase(
                postUtilsWrapper = createdMockedPostUtilsWrapper(
                        isPostBeingEdited = true
                )
        )

        val post = createPostModel()
        val siteModel: SiteModel = createSiteModel()
        // Act
        val action = uploadActionUseCase.getAutoUploadAction(post, siteModel)

        // Assert
        assertThat(action).isEqualTo(UploadAction.DO_NOTHING)
    }

    private companion object Fixtures {
        private fun createUploadActionUseCase(
            uploadStore: UploadStore = createdMockedUploadStore(),
            postUtilsWrapper: PostUtilsWrapper = createdMockedPostUtilsWrapper(),
            uploadServiceFacade: UploadServiceFacade = createdMockedUploadServiceFacade()
        ) = UploadActionUseCase(uploadStore, postUtilsWrapper, uploadServiceFacade)

        private fun createdMockedUploadStore(numberOfAutoUploadAttempts: Int = 0): UploadStore {
            val uploadStore: UploadStore = mock()
            whenever(uploadStore.getNumberOfPostAutoUploadAttempts(any())).thenReturn(numberOfAutoUploadAttempts)
            return uploadStore
        }

        private fun createdMockedPostUtilsWrapper(
            isPublishable: Boolean = true,
            isInConflict: Boolean = false,
            isPostBeingEdited: Boolean = false
        ): PostUtilsWrapper {
            val postUtilsWrapper: PostUtilsWrapper = mock()
            whenever(postUtilsWrapper.isPublishable(any())).thenReturn(isPublishable)
            whenever(postUtilsWrapper.isPostInConflictWithRemote(any())).thenReturn(isInConflict)
            whenever(postUtilsWrapper.isPostCurrentlyBeingEdited(any())).thenReturn(isPostBeingEdited)
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
            changesConfirmed: Boolean = false,
            autoSaveModified: String? = null
        ): PostModel = PostModel().apply {
            this.setStatus(status)
            this.setIsLocalDraft(isLocalDraft)
            this.setIsLocallyChanged(isLocallyChanged)
            this.setDateLocallyChanged(dateLocallyChanged)
            this.setAutoSaveModified(autoSaveModified)
            if (changesConfirmed) {
                this.setChangesConfirmedContentHashcode(this.contentHashcode())
            }
        }

        private fun createSiteModel(isWpCom: Boolean = true) = SiteModel().apply {
            setIsWPCom(isWpCom)
        }
    }
}
