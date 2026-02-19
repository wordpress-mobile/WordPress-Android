package org.wordpress.android.fluxc.network.rest.wpapi.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.MediaAction
import org.wordpress.android.fluxc.action.UploadAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.media.MediaRSApiRestClient.FileCheckWrapper
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaCaptionWithEditContext
import uniffi.wp_api.MediaDeleteResponse
import uniffi.wp_api.MediaDescriptionWithEditContext
import uniffi.wp_api.MediaDetails
import uniffi.wp_api.MediaDetailsPayload
import uniffi.wp_api.MediaRequestCreateResponse
import uniffi.wp_api.MediaRequestDeleteResponse
import uniffi.wp_api.MediaRequestListWithEditContextResponse
import uniffi.wp_api.MediaRequestRetrieveWithEditContextResponse
import uniffi.wp_api.MediaRequestUpdateResponse
import uniffi.wp_api.MediaStatus
import uniffi.wp_api.MediaType
import uniffi.wp_api.MediaWithEditContext
import uniffi.wp_api.PostCommentStatus
import uniffi.wp_api.PostGuidWithEditContext
import uniffi.wp_api.PostPingStatus
import uniffi.wp_api.PostTitleWithEditContext
import uniffi.wp_api.WpNetworkHeaderMap
import java.util.Date

private const val PATH_SEPARATOR = "/"
private const val SUFFIX_SEPARATOR = "?"

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MediaRsApiRestClientTest {
    @Mock
    private lateinit var dispatcher: Dispatcher
    @Mock
    private lateinit var appLogWrapper: AppLogWrapper
    @Mock
    private lateinit var wpApiClientProvider: WpApiClientProvider
    @Mock
    private lateinit var wpApiClient: WpApiClient
    @Mock
    private lateinit var fileCheckWrapper: FileCheckWrapper

    private lateinit var testScope: CoroutineScope
    private lateinit var restClient: MediaRSApiRestClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val testScheduler = TestCoroutineScheduler()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        testScope = CoroutineScope(testDispatcher)

        whenever(wpApiClientProvider.getWpApiClient(any(), any())).thenReturn(wpApiClient)
        whenever(wpApiClientProvider.getWpApiClient(any(), eq(null))).thenReturn(wpApiClient)

        restClient = MediaRSApiRestClient(
            scope = testScope,
            dispatcher = dispatcher,
            appLogWrapper = appLogWrapper,
            wpApiClientProvider = wpApiClientProvider,
            fileCheckWrapper = fileCheckWrapper,
        )
    }

    @Test
    fun `fetchMedia with null media dispatches error action immediately`() = runTest {
        val testSite = createTestSite()

        restClient.fetchMedia(testSite, null)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as MediaPayload
        assertEquals(capturedAction.type, MediaAction.FETCHED_MEDIA)
        assertEquals(testSite, payload.site)
        assertNull(payload.media)
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.NULL_MEDIA_ARG, payload.error?.type)
    }

    @Test
    fun `fetchMedia calls wpApiClientProvider getWpApiClient when media is not null and dispatch media action`() =
        runTest {
            val testSite = createTestSite()
            val testMedia = createTestMedia()
            val mediaWithEditContext = createTestMediaWithEditContext(testSite.id.toLong())
            val mediaRequestResult: WpRequestResult<MediaRequestRetrieveWithEditContextResponse> =
                WpRequestResult.Success(
                    response = MediaRequestRetrieveWithEditContextResponse(
                        mediaWithEditContext,
                        mock<WpNetworkHeaderMap>()
                    )
                )
            val mediaResult = mediaWithEditContext.toMediaModel(siteId = testSite.id)

            whenever(wpApiClient.request<MediaRequestRetrieveWithEditContextResponse>(any()))
                .thenReturn(mediaRequestResult)

            restClient.fetchMedia(testSite, testMedia)

            // Verify dispatcher was called with the media
            val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
            verify(dispatcher).dispatch(actionCaptor.capture())

            val capturedAction = actionCaptor.value
            val payload = capturedAction.payload as MediaPayload
            assertEquals(capturedAction.type, MediaAction.FETCHED_MEDIA)
            assertEquals(testSite, payload.site)
            assertEquals(mediaResult, payload.media)
            assertNull(payload.error)
        }

    @Test
    fun `fetchMedia with error response dispatches error action`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia()

        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<MediaWithEditContext>(
            statusCode = 500u,
            response = "Internal Server Error"
        )

        whenever(wpApiClient.request<MediaWithEditContext>(any())).thenReturn(errorResponse)

        restClient.fetchMedia(testSite, testMedia)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as MediaPayload
        assertEquals(capturedAction.type, MediaAction.FETCHED_MEDIA)
        assertEquals(testSite, payload.site)
        assertEquals(testMedia, payload.media) // Error case returns original media
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.GENERIC_ERROR, payload.error?.type)
        assertEquals("Unknown error occurred", payload.error?.message)
    }

    @Test
    fun `fetchMediaList with success response dispatches success action`() = runTest {
        val testSite = createTestSite()
        val mediaWithEditContext = listOf(
            createTestMediaWithEditContext(testSite.id.toLong()),
            createTestMediaWithEditContext(testSite.id.toLong())
        )
        val mediaRequestResult: WpRequestResult<MediaRequestListWithEditContextResponse> =
            WpRequestResult.Success(
                response = MediaRequestListWithEditContextResponse(
                    mediaWithEditContext,
                    mock<WpNetworkHeaderMap>(),
                    null,
                    null)
            )
        val mediaResult = mediaWithEditContext.map { it.toMediaModel(siteId = testSite.id) }

        whenever(wpApiClient.request<MediaRequestListWithEditContextResponse>(any())).thenReturn(mediaRequestResult)

        restClient.fetchMediaList(testSite, 10, 0, MimeType.Type.IMAGE)

        // Verify dispatcher was called - the actual implementation will handle the response parsing
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchMediaListResponsePayload
        assertEquals(capturedAction.type, MediaAction.FETCHED_MEDIA_LIST)
        assertEquals(testSite, payload.site)
        assertEquals(false, payload.loadedMore) // offset was 0
        assertEquals(mediaResult, payload.mediaList)
        assertNull(payload.error)
        assertEquals(MimeType.Type.IMAGE, payload.mimeType)
    }

    @Test
    fun `fetchMediaList with error response dispatches empty list action`() = runTest {
        val testSite = createTestSite()
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<MediaRequestListWithEditContextResponse>(
            statusCode = 500u,
            response = "Internal Server Error"
        )

        whenever(wpApiClient.request<MediaRequestListWithEditContextResponse>(any())).thenReturn(errorResponse)

        restClient.fetchMediaList(testSite, 10, 0, MimeType.Type.IMAGE)

        // Verify dispatcher was called with empty list
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchMediaListResponsePayload
        assertEquals(capturedAction.type, MediaAction.FETCHED_MEDIA_LIST)
        assertEquals(testSite, payload.site)
        assertEquals(false, payload.loadedMore)
        assertEquals(false, payload.canLoadMore)
        assertEquals(MimeType.Type.IMAGE, payload.mimeType)
    }

    @Test
    fun `deleteMedia with null media dispatches error action immediately`() = runTest {
        val testSite = createTestSite()

        restClient.deleteMedia(testSite, null)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as MediaPayload
        assertEquals(capturedAction.type, MediaAction.DELETED_MEDIA)
        assertEquals(testSite, payload.site)
        assertNull(payload.media)
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.NULL_MEDIA_ARG, payload.error?.type)
        assertEquals("Media to delete is null", payload.error?.logMessage)
    }

    @Test
    fun `deleteMedia with success response dispatches success action`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia()
        val mediaWithEditContext = createTestMediaWithEditContext(testSite.id.toLong())
        val mediaDeleteResponse = MediaDeleteResponse(true, mediaWithEditContext)
        val mediaRequestResult: WpRequestResult<MediaRequestDeleteResponse> =
            WpRequestResult.Success(
                response = MediaRequestDeleteResponse(
                    mediaDeleteResponse, mock<WpNetworkHeaderMap>()
                )
            )
        val mediaResult = mediaWithEditContext.toMediaModel(siteId = testSite.id)

        whenever(wpApiClient.request<MediaRequestDeleteResponse>(any())).thenReturn(mediaRequestResult)

        restClient.deleteMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as MediaPayload
        assertEquals(capturedAction.type, MediaAction.DELETED_MEDIA)
        assertEquals(testSite, payload.site)
        assertEquals(mediaResult, payload.media)
        assertNull(payload.error)
    }

    @Test
    fun `deleteMedia with error response dispatches error action`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia()

        whenever(wpApiClient.request<Any>(any())).thenReturn(
            WpRequestResult.UnknownError(statusCode = 404u, response = "Media not found")
        )

        restClient.deleteMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as MediaPayload
        assertEquals(capturedAction.type, MediaAction.DELETED_MEDIA)
        assertEquals(testSite, payload.site)
        assertEquals(testMedia, payload.media) // Error case returns original media
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `uploadMedia with null media dispatches error action immediately`() = runTest {
        val testSite = createTestSite()

        restClient.uploadMedia(testSite, null)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as ProgressPayload
        assertEquals(capturedAction.type, UploadAction.UPLOADED_MEDIA)
        assertNull(payload.media)
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.INVALID_ID, payload.error?.type)
        assertEquals("Media object is null on upload", payload.error?.logMessage)
        assertEquals(1f, payload.progress, 0.01f)
        assertEquals(false, payload.completed)
    }

    @Test
    fun `uploadMedia with media ID 0 dispatches error action immediately`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia().apply {
            id = 0 // Invalid ID
        }

        restClient.uploadMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as ProgressPayload
        assertEquals(capturedAction.type, UploadAction.UPLOADED_MEDIA)
        assertEquals(testMedia, payload.media)
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.INVALID_ID, payload.error?.type)
        assertEquals("Media ID is 0 on upload", payload.error?.logMessage)
        assertEquals(1f, payload.progress, 0.01f)
        assertEquals(false, payload.completed)
    }

    @Test
    fun `uploadMedia with null filePath dispatches error action immediately`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia().apply {
            filePath = null
        }

        restClient.uploadMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as ProgressPayload
        assertEquals(capturedAction.type, UploadAction.UPLOADED_MEDIA)
        assertEquals(testMedia, payload.media)
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.FS_READ_PERMISSION_DENIED, payload.error?.type)
        assertEquals("Can't read file on upload", payload.error?.logMessage)
        assertEquals(1f, payload.progress, 0.01f)
        assertEquals(false, payload.completed)
    }

    @Test
    fun `uploadMedia with unreadable file dispatches error action immediately`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia().apply {
            filePath = ""  // Empty file path will fail MediaUtils.canReadFile
        }

        restClient.uploadMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as ProgressPayload
        assertEquals(capturedAction.type, UploadAction.UPLOADED_MEDIA)
        assertEquals(testMedia, payload.media)
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.FS_READ_PERMISSION_DENIED, payload.error?.type)
        assertEquals("Can't read file on upload", payload.error?.logMessage)
        assertEquals(1f, payload.progress, 0.01f)
        assertEquals(false, payload.completed)
    }

    @Test
    fun `uploadMedia with success response dispatches success action`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia().apply {
            filePath = "/valid/path/file.jpg"
        }
        val mediaWithEditContext = createTestMediaWithEditContext(testMedia.id.toLong())
        val mediaRequestResult: WpRequestResult<MediaRequestCreateResponse> =
            WpRequestResult.Success(
                response = MediaRequestCreateResponse(mediaWithEditContext, mock<WpNetworkHeaderMap>())
            )
        val mediaResult = mediaWithEditContext.toMediaModel(siteId = testSite.id)

        whenever(fileCheckWrapper.canReadFile(any())).thenReturn(true)
        whenever(wpApiClient.request<MediaRequestCreateResponse>(any())).thenReturn(mediaRequestResult)

        restClient.uploadMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as ProgressPayload
        assertEquals(capturedAction.type, UploadAction.UPLOADED_MEDIA)
        assertEquals(mediaResult.mediaId, payload.media?.mediaId)
        assertTrue(payload.completed)
        assertNull(payload.error)
    }

    @Test
    fun `uploadMedia preserves local metadata from original media model`() = runTest {
        val testSite = createTestSite()
        val localPostId = 42
        val testMedia = createTestMedia().apply {
            filePath = "/valid/path/file.jpg"
            this.localPostId = localPostId
            markedLocallyAsFeatured = true
        }
        val mediaWithEditContext = createTestMediaWithEditContext(testMedia.id.toLong())
        val mediaRequestResult: WpRequestResult<MediaRequestCreateResponse> =
            WpRequestResult.Success(
                response = MediaRequestCreateResponse(
                    mediaWithEditContext, mock<WpNetworkHeaderMap>()
                )
            )

        whenever(fileCheckWrapper.canReadFile(any())).thenReturn(true)
        whenever(wpApiClient.request<MediaRequestCreateResponse>(any()))
            .thenReturn(mediaRequestResult)

        restClient.uploadMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as ProgressPayload
        assertEquals(testMedia.id, payload.media?.id)
        assertEquals(localPostId, payload.media?.localPostId)
        assertTrue(payload.media?.markedLocallyAsFeatured == true)
    }

    @Test
    fun `uploadMedia with error response dispatches error action`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia().apply {
            filePath = "/valid/path/file.jpg"
        }

        whenever(fileCheckWrapper.canReadFile(any())).thenReturn(true)
        // Mock an error response
        whenever(wpApiClient.request<Any>(any())).thenReturn(
            WpRequestResult.UnknownError(statusCode = 413u, response = "File too large")
        )

        restClient.uploadMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as ProgressPayload
        assertEquals(capturedAction.type, UploadAction.UPLOADED_MEDIA)
        assertEquals(testMedia, payload.media)
        assertEquals(MediaUploadState.FAILED.toString(), payload.media?.uploadState)
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.GENERIC_ERROR, payload.error?.type)
        assertEquals(1f, payload.progress, 0.01f)
        assertEquals(false, payload.completed)
    }

    @Test
    fun `pushMedia with null media dispatches error action immediately`() = runTest {
        val testSite = createTestSite()

        restClient.pushMedia(testSite, null)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as MediaPayload
        assertEquals(capturedAction.type, MediaAction.PUSHED_MEDIA)
        assertEquals(testSite, payload.site)
        assertNull(payload.media)
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.NULL_MEDIA_ARG, payload.error?.type)
        assertEquals("Pushed media is null", payload.error?.logMessage)
    }

    @Test
    fun `pushMedia with success response dispatches success action`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia()
        val mediaWithEditContext = createTestMediaWithEditContext(testMedia.mediaId)
        val mediaRequestResult: WpRequestResult<MediaRequestUpdateResponse> =
            WpRequestResult.Success(
                response = MediaRequestUpdateResponse(
                    mediaWithEditContext,
                    mock<WpNetworkHeaderMap>()
                )
            )

        whenever(wpApiClient.request<MediaRequestUpdateResponse>(any())).thenReturn(mediaRequestResult)

        restClient.pushMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as MediaPayload
        assertEquals(capturedAction.type, MediaAction.PUSHED_MEDIA)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.media)
        assertEquals(testMedia.mediaId, payload.media?.mediaId)
        assertEquals(mediaWithEditContext.sourceUrl, payload.media?.url)
        assertEquals(mediaWithEditContext.title.raw, payload.media?.title)
        assertNull(payload.error)
    }

    @Test
    fun `pushMedia with error response dispatches error action`() = runTest {
        val testSite = createTestSite()
        val testMedia = createTestMedia()

        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<MediaRequestUpdateResponse>(
            statusCode = 500u,
            response = "Internal Server Error"
        )

        whenever(wpApiClient.request<MediaRequestUpdateResponse>(any())).thenReturn(errorResponse)

        restClient.pushMedia(testSite, testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as MediaPayload
        assertEquals(capturedAction.type, MediaAction.PUSHED_MEDIA)
        assertEquals(testSite, payload.site)
        assertEquals(testMedia, payload.media) // Error case returns original media
        assertNotNull(payload.error)
        assertEquals(MediaErrorType.GENERIC_ERROR, payload.error?.type)
        assertEquals("Unknown error occurred", payload.error?.message)
    }

    @Test
    fun `cancelUpload with null media logs error and returns without dispatching action`() = runTest {
        restClient.cancelUpload(null)

        // Verify that no action was dispatched
        verify(dispatcher, never()).dispatch(any())

        // Verify that error was logged
        verify(appLogWrapper).e(AppLog.T.MEDIA, "Error: no media passed to cancel upload")
    }

    @Test
    fun `cancelUpload with valid media dispatches cancel action`() = runTest {
        val testMedia = createTestMedia()

        restClient.cancelUpload(testMedia)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as ProgressPayload
        assertEquals(capturedAction.type, MediaAction.CANCELED_MEDIA_UPLOAD)
        assertEquals(testMedia, payload.media)
        assertEquals(0f, payload.progress, 0.01f)
        assertEquals(false, payload.completed)
        assertEquals(true, payload.canceled)
    }

    private fun createTestSite() = SiteModel().apply {
        id = 123
        url = "https://example.wordpress.com"
        wpApiRestUrl = "https://example.wordpress.com/wp-json"
        apiRestUsernamePlain = "testuser"
        apiRestPasswordPlain = "testpass"
    }

    private fun createTestMedia() = MediaModel(123, 456L).apply {
        id = 123
        mediaId = 456L
        title = "Test Media"
        url = "https://example.com/media.jpg"
        mimeType = "image/jpeg"
        uploadState = MediaUploadState.UPLOADED.toString()
    }

    /**
     * Creates a real MediaWithEditContext object with all required parameters.
     */
    private fun createTestMediaWithEditContext(mediaId: Long): MediaWithEditContext {
        return MediaWithEditContext(
            id = mediaId,
            sourceUrl = "https://example.com/test-media.jpg",
            link = "https://example.com/media-link",
            title = createTestPostTitle(),
            caption = createTestMediaCaption(),
            description = createTestMediaDescription(),
            altText = "Test alt text",
            postId = null,
            mimeType = "image/jpeg",
            mediaType = MediaType.Image,
            date = "2023-01-01T00:00:00",
            dateGmt = Date(),
            author = 1L,
            mediaDetails = mock<MediaDetails>(),
            guid = createTestPostGuid(),
            modified = "2023-01-01T00:00:00",
            modifiedGmt = Date(),
            slug = "test-media-slug",
            status = MediaStatus.Inherit,
            postType = "attachment",
            password = "",
            permalinkTemplate = "https://example.com/%postname%",
            generatedSlug = "test-media-slug",
            commentStatus = PostCommentStatus.Open,
            pingStatus = PostPingStatus.Open,
            template = "",
            missingImageSizes = emptyList()
        )
    }

    private fun createTestPostTitle(): PostTitleWithEditContext {
        return PostTitleWithEditContext(
            raw = "Test Media Title",
            rendered = "Test Media Title"
        )
    }

    private fun createTestMediaCaption(): MediaCaptionWithEditContext {
        return MediaCaptionWithEditContext(
            raw = "Test Caption",
            rendered = "Test Caption"
        )
    }

    private fun createTestMediaDescription(): MediaDescriptionWithEditContext {
        return MediaDescriptionWithEditContext(
            raw = "Test Description",
            rendered = "Test Description"
        )
    }

    private fun createTestPostGuid(): PostGuidWithEditContext {
        return PostGuidWithEditContext(
            raw = "https://example.com/media-guid",
            rendered = "https://example.com/media-guid"
        )
    }

    private fun MediaWithEditContext.toMediaModel(
        siteId: Int
    ): MediaModel = MediaModel(siteId, id).apply {
        url = this@toMediaModel.sourceUrl
        fileExtension = this@toMediaModel.mimeType
        guid = this@toMediaModel.link
        title = this@toMediaModel.title.raw
        caption = this@toMediaModel.caption.raw
        description = this@toMediaModel.description.raw
        alt = this@toMediaModel.altText
        postId = this@toMediaModel.postId ?: 0
        mimeType = this@toMediaModel.mimeType
        uploadDate = this@toMediaModel.date
        authorId = this@toMediaModel.author
        uploadState = MediaUploadState.UPLOADED.toString()

        // Parse the media details
        when (val parsedType = this@toMediaModel.mediaDetails.parseAsMimeType(this@toMediaModel.mimeType)) {
            is MediaDetailsPayload.Audio -> length = parsedType.v1.length.toInt()
            is MediaDetailsPayload.Image -> {
                fileName = parseFileNameFromPath(parsedType.v1.file)
                width = parsedType.v1.width.toInt()
                height = parsedType.v1.height.toInt()
                thumbnailUrl = parsedType.v1.sizes?.get("thumbnail")?.sourceUrl
                fileUrlMediumSize = parsedType.v1.sizes?.get("medium")?.sourceUrl
                fileUrlLargeSize = parsedType.v1.sizes?.get("large")?.sourceUrl
            }
            is MediaDetailsPayload.Video -> {
                width = parsedType.v1.width.toInt()
                height = parsedType.v1.height.toInt()
                length = parsedType.v1.length.toInt()
            }
            is MediaDetailsPayload.Document,
            null -> {}
        }

        if (fileName.isNullOrEmpty()) {
            fileName = parseFileNameFromUrl(url)
        }
    }

    private fun parseFileNameFromUrl(url: String): String = if (url.contains(PATH_SEPARATOR)) {
        val lastUrlPart = url.substringAfterLast(PATH_SEPARATOR)
        if (lastUrlPart.contains(SUFFIX_SEPARATOR)) {
            lastUrlPart.substringBefore(SUFFIX_SEPARATOR)
        } else {
            lastUrlPart
        }
    } else {
        url
    }

    private fun parseFileNameFromPath(fileNameWithPath: String): String =
        if (fileNameWithPath.contains(PATH_SEPARATOR)) {
            fileNameWithPath.substringAfterLast(PATH_SEPARATOR)
        } else {
            fileNameWithPath
        }
}
