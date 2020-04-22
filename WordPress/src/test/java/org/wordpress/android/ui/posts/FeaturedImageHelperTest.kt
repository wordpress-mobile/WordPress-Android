package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.MediaAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.FeaturedImageHelper.EnqueueFeaturedImageResult
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageState
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class FeaturedImageHelperTest {
    private val uploadStore: UploadStore = mock()
    private val mediaStore: MediaStore = mock()
    private val uploadServiceFacade: UploadServiceFacade = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val readerUtilsWrapper: ReaderUtilsWrapper = mock()
    private val fluxCUtilsWrapper: FluxCUtilsWrapper = mock()
    private val siteUtilsWrapper: SiteUtilsWrapper = mock()
    private val dispatcher: Dispatcher = mock()

    private lateinit var featuredImageHelper: FeaturedImageHelper

    @Before
    fun setUp() {
        featuredImageHelper = FeaturedImageHelper(
                uploadStore,
                mediaStore,
                uploadServiceFacade,
                resourceProvider,
                readerUtilsWrapper,
                fluxCUtilsWrapper,
                siteUtilsWrapper,
                dispatcher,
                mock()
        )
    }

    @Test
    fun `getFailedFeaturedImageUpload returns null when there isn't any failed featured image upload`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull()))
                .thenReturn(setOf(createMediaModel(markedLocallyAsFeatured = false)))
        // Act
        val failedFeaturedImage = featuredImageHelper.getFailedFeaturedImageUpload(mock())
        // Assert
        assertThat(failedFeaturedImage).isNull()
    }

    @Test
    fun `getFailedFeaturedImageUpload returns MediaModel when an featured image upload failes`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull()))
                .thenReturn(setOf(createMediaModel(markedLocallyAsFeatured = true)))
        // Act
        val failedFeaturedImage = featuredImageHelper.getFailedFeaturedImageUpload(mock())
        // Assert
        assertThat(failedFeaturedImage).isNotNull
    }

    @Test
    fun `retryFeaturedImageUpload sets MediaUploadState to QUEUED`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull()))
                .thenReturn(setOf(createMediaModel(markedLocallyAsFeatured = true)))
        // Act
        val mediaModel = featuredImageHelper.retryFeaturedImageUpload(mock(), mock())
        // Assert
        assertThat(MediaUploadState.fromString(mediaModel!!.uploadState)).isEqualTo(MediaUploadState.QUEUED)
    }

    @Test
    fun `retryFeaturedImageUpload dispatches updateMediaAction`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull()))
                .thenReturn(setOf(createMediaModel(markedLocallyAsFeatured = true)))
        val captor: KArgumentCaptor<Action<MediaModel>> = argumentCaptor()
        // Act
        val mediaModel = featuredImageHelper.retryFeaturedImageUpload(mock(), mock())
        // Assert
        verify(dispatcher).dispatch(captor.capture())
        assertThat(captor.firstValue).matches { action -> action.type == MediaAction.UPDATE_MEDIA }
        assertThat(captor.firstValue).matches { action -> action.payload == mediaModel }
    }

    @Test
    fun `retryFeaturedImageUpload starts the UploadService`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull()))
                .thenReturn(setOf(createMediaModel(markedLocallyAsFeatured = true)))
        // Act
        val mediaModel = featuredImageHelper.retryFeaturedImageUpload(mock(), mock())
        // Assert
        verify(uploadServiceFacade).uploadMedia(argThat { this[0] == mediaModel })
    }

    @Test
    fun `queueFeaturedImageForUpload marks the MediaModel as featuredImage`() {
        // Arrange
        val mediaModel = createMediaModel(markedLocallyAsFeatured = false)
        whenever(fluxCUtilsWrapper.mediaModelFromLocalUri(anyOrNull(), anyOrNull(), anyInt())).thenReturn(mediaModel)
        // Act
        featuredImageHelper.queueFeaturedImageForUpload(0, createSiteModel(), mock(), "")
        // Assert
        assertThat(mediaModel.markedLocallyAsFeatured).isTrue()
    }

    @Test
    fun `queueFeaturedImageForUpload returns fileNotFound when the uri can't be converted to MediaModel`() {
        // Arrange
        whenever(fluxCUtilsWrapper.mediaModelFromLocalUri(anyOrNull(), anyOrNull(), anyInt())).thenReturn(null)
        // Act
        val result = featuredImageHelper.queueFeaturedImageForUpload(0, createSiteModel(), mock(), "")
        // Assert
        assertThat(result).isEqualTo(EnqueueFeaturedImageResult.FILE_NOT_FOUND)
    }

    @Test
    fun `queueFeaturedImageForUpload returns invalidPostId when the provided localPostId is empty`() {
        // Arrange
        whenever(fluxCUtilsWrapper.mediaModelFromLocalUri(anyOrNull(), anyOrNull(), anyInt()))
                .thenReturn(createMediaModel(markedLocallyAsFeatured = false))
        // Act
        val result = featuredImageHelper
                .queueFeaturedImageForUpload(EMPTY_LOCAL_POST_ID, createSiteModel(), mock(), "")
        // Assert
        assertThat(result).isEqualTo(EnqueueFeaturedImageResult.INVALID_POST_ID)
    }

    @Test
    fun `queueFeaturedImageForUpload returns success when it succeeds`() {
        // Arrange
        whenever(fluxCUtilsWrapper.mediaModelFromLocalUri(anyOrNull(), anyOrNull(), anyInt()))
                .thenReturn(createMediaModel(markedLocallyAsFeatured = false))
        // Act
        val result = featuredImageHelper.queueFeaturedImageForUpload(0, createSiteModel(), mock(), "")
        // Assert
        assertThat(result).isEqualTo(EnqueueFeaturedImageResult.SUCCESS)
    }

    @Test
    fun `queueFeaturedImageForUpload dispatches updateMediaAction`() {
        // Arrange
        val mediaModel = createMediaModel(markedLocallyAsFeatured = false)
        whenever(fluxCUtilsWrapper.mediaModelFromLocalUri(anyOrNull(), anyOrNull(), anyInt())).thenReturn(mediaModel)
        val captor: KArgumentCaptor<Action<MediaModel>> = argumentCaptor()
        // Act
        featuredImageHelper.queueFeaturedImageForUpload(0, createSiteModel(), mock(), "")
        // Assert
        verify(dispatcher).dispatch(captor.capture())
        assertThat(captor.firstValue).matches { action -> action.type == MediaAction.UPDATE_MEDIA }
        assertThat(captor.firstValue).matches { action -> action.payload == mediaModel }
    }

    @Test
    fun `queueFeaturedImageForUpload starts the UploadService`() {
        // Arrange
        val mediaModel = createMediaModel(markedLocallyAsFeatured = false)
        whenever(fluxCUtilsWrapper.mediaModelFromLocalUri(anyOrNull(), anyOrNull(), anyInt())).thenReturn(mediaModel)
        // Act
        featuredImageHelper.queueFeaturedImageForUpload(0, createSiteModel(), mock(), "")
        // Assert
        verify(uploadServiceFacade).uploadMedia(argThat { this[0] == mediaModel })
    }

    @Test
    fun `cancelFeaturedImageUpload dispatches CancelMediaUploadAction`() {
        // Arrange
        val mediaModel = createMediaModel(markedLocallyAsFeatured = true)
        whenever(uploadStore.getFailedMediaForPost(anyOrNull())).thenReturn(setOf(mediaModel))

        val captor: KArgumentCaptor<Action<MediaModel>> = argumentCaptor()
        // Act
        featuredImageHelper.cancelFeaturedImageUpload(createSiteModel(), mock(), false)
        // Assert
        verify(dispatcher).dispatch(captor.capture())
        assertThat(captor.firstValue).matches { action -> action.type == MediaAction.CANCEL_MEDIA_UPLOAD }
    }

    @Test
    fun `cancelFeaturedImageUpload cancels final notifications`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull()))
                .thenReturn(setOf(createMediaModel(markedLocallyAsFeatured = true)))
        // Act
        featuredImageHelper.cancelFeaturedImageUpload(createSiteModel(), mock(), cancelFailedOnly = false)
        // Assert
        verify(uploadServiceFacade).cancelFinalNotification(anyOrNull())
        verify(uploadServiceFacade).cancelFinalNotificationForMedia(anyOrNull())
    }

    @Test
    fun `cancelFeaturedImageUpload loads model from uploads when cancelFailedOnly is false`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull())).thenReturn(setOf())

        // Act
        featuredImageHelper.cancelFeaturedImageUpload(createSiteModel(), mock(), cancelFailedOnly = false)
        // Assert
        verify(uploadServiceFacade).getPendingOrInProgressFeaturedImageUploadForPost(anyOrNull())
    }

    @Test
    fun `cancelFeaturedImageUpload does NOT load model from uploads when cancelFailedOnly is true`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull())).thenReturn(setOf())

        // Act
        featuredImageHelper.cancelFeaturedImageUpload(createSiteModel(), mock(), cancelFailedOnly = true)
        // Assert
        verify(uploadServiceFacade, never()).getPendingOrInProgressFeaturedImageUploadForPost(anyOrNull())
    }

    @Test
    fun `createCurrent-State returns IMAGE_UPLOAD_IN_PROGRESS when uploadService has pending or ongoing uploads`() {
        // Arrange
        whenever(uploadServiceFacade.getPendingOrInProgressFeaturedImageUploadForPost(anyOrNull()))
                .thenReturn(createMediaModel(markedLocallyAsFeatured = true))

        // Act
        val result = featuredImageHelper.createCurrentFeaturedImageState(createSiteModel(), mock())
        // Assert
        assertThat(result).matches { it.uiState == FeaturedImageState.IMAGE_UPLOAD_IN_PROGRESS }
    }

    @Test
    fun `createCurrent-State returns IMAGE_UPLOAD_FAILED when uploadStore returns failed upload`() {
        // Arrange
        whenever(uploadStore.getFailedMediaForPost(anyOrNull()))
                .thenReturn(setOf(createMediaModel(markedLocallyAsFeatured = true)))

        // Act
        val result = featuredImageHelper.createCurrentFeaturedImageState(createSiteModel(), mock())
        // Assert
        assertThat(result).matches { it.uiState == FeaturedImageState.IMAGE_UPLOAD_FAILED }
    }

    @Test
    fun `createCurrent-State returns IMAGE_EMPTY when post doesn't have an attached featured image`() {
        // Arrange
        val post: PostImmutableModel = mock()
        whenever(post.hasFeaturedImage()).thenReturn(false)
        // Act
        val result = featuredImageHelper.createCurrentFeaturedImageState(createSiteModel(), post)
        // Assert
        assertThat(result).matches { it.uiState == FeaturedImageState.IMAGE_EMPTY }
    }

    @Test
    fun `createCurrent-State returns IMAGE_EMPTY when media item isn't found in the local database`() {
        // Arrange
        val post: PostImmutableModel = mock()
        whenever(post.hasFeaturedImage()).thenReturn(true)
        whenever(mediaStore.getSiteMediaWithId(anyOrNull(), anyLong())).thenReturn(null)
        // Act
        val result = featuredImageHelper.createCurrentFeaturedImageState(createSiteModel(), post)
        // Assert
        assertThat(result).matches { it.uiState == FeaturedImageState.IMAGE_EMPTY }
    }

    @Test
    fun `createCurrent-State returns REMOTE_IMAGE_LOADING when image found in local database`() {
        // Arrange
        val post: PostImmutableModel = mock()
        whenever(post.hasFeaturedImage()).thenReturn(true)
        whenever(mediaStore.getSiteMediaWithId(anyOrNull(), anyLong())).thenReturn(mock())
        // Act
        val result = featuredImageHelper.createCurrentFeaturedImageState(createSiteModel(), post)
        // Assert
        assertThat(result).matches { it.uiState == FeaturedImageState.REMOTE_IMAGE_LOADING }
    }

    @Test
    fun `createCurrent-State uses media url when thumbnailUrl is empty`() {
        // Arrange
        val post: PostImmutableModel = mock()
        whenever(post.hasFeaturedImage()).thenReturn(true)

        val media: MediaModel = mock()
        whenever(media.thumbnailUrl).thenReturn(null)
        whenever(media.url).thenReturn("https://testing.com/url.jpg")
        whenever(mediaStore.getSiteMediaWithId(anyOrNull(), anyLong())).thenReturn(media)

        val site = createSiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
        }

        // Act
        featuredImageHelper.createCurrentFeaturedImageState(site, post)
        // Assert
        verify(readerUtilsWrapper).getResizedImageUrl(
                eq("https://testing.com/url.jpg"),
                anyInt(),
                anyInt(),
                anyBoolean(),
                anyBoolean()
        )
    }

    @Test
    fun `createCurrent-State uses thumbnailUrl if it is not empty`() {
        // Arrange
        val post: PostImmutableModel = mock()
        whenever(post.hasFeaturedImage()).thenReturn(true)

        val media: MediaModel = mock()
        whenever(media.thumbnailUrl).thenReturn("https://testing.com/thumbnail.jpg")
        whenever(mediaStore.getSiteMediaWithId(anyOrNull(), anyLong())).thenReturn(media)

        val site = createSiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
        }

        // Act
        featuredImageHelper.createCurrentFeaturedImageState(site, post)
        // Assert
        verify(readerUtilsWrapper).getResizedImageUrl(eq(
                "https://testing.com/thumbnail.jpg"),
                anyInt(),
                anyInt(),
                anyBoolean(),
                anyBoolean()
        )
    }

    companion object Fixtures {
        fun createMediaModel(markedLocallyAsFeatured: Boolean): MediaModel {
            return MediaModel().apply {
                this.markedLocallyAsFeatured = markedLocallyAsFeatured
            }
        }

        fun createSiteModel(): SiteModel = SiteModel().apply { id = 1 }
    }
}
