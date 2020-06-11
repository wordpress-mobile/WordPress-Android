package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.test
import org.wordpress.android.util.FileProvider
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import java.io.File

@RunWith(MockitoJUnitRunner::class)
@UseExperimental(InternalCoroutinesApi::class)
class GetMediaModelUseCaseTest : BaseUnitTest() {
    @Test
    fun `loadMediaByLocalId loads models from db and returns them`() = test {
        // Arrange
        val localIds = listOf(1, 2, 3)
        val mediaStore = createMediaStore()
        // Act
        val result = createGetMediaModelUseCase(mediaStore = mediaStore)
                .loadMediaByLocalId(localIds)
        // Assert
        verify(mediaStore).getMediaWithLocalId(localIds[0])
        verify(mediaStore).getMediaWithLocalId(localIds[1])
        verify(mediaStore).getMediaWithLocalId(localIds[2])
        assertThat(result.size).isEqualTo(localIds.size)
    }

    @Test
    fun `loadMediaByLocalId filters out media which does not exist in the db`() = test {
        // Arrange
        val localIds = listOf(1, 2, 3)
        val mediaStore = createMediaStore(resultForGetMediaWithLocalId = (localIds[1] to null))
        // Act
        val result = createGetMediaModelUseCase(mediaStore = mediaStore)
                .loadMediaByLocalId(localIds)
        // Assert
        assertThat(result.size).isEqualTo(localIds.size - 1)
    }

    @Test
    fun `loadMediaByRemoteId loads models from db and returns them`() = test {
        // Arrange
        val remoteIds = listOf(1L, 2L, 3L)
        val siteModel = mock<SiteModel>()
        val mediaStore = createMediaStore()
        // Act
        val result = createGetMediaModelUseCase(mediaStore = mediaStore)
                .loadMediaByRemoteId(siteModel, remoteIds)
        // Assert
        verify(mediaStore).getSiteMediaWithId(siteModel, remoteIds[0])
        verify(mediaStore).getSiteMediaWithId(siteModel, remoteIds[1])
        verify(mediaStore).getSiteMediaWithId(siteModel, remoteIds[2])
        assertThat(result.size).isEqualTo(remoteIds.size)
    }

    @Test
    fun `loadMediaByRemoteId filters out media which does not exist in the db`() = test {
        // Arrange
        val remoteIds = listOf(1L, 2L, 3L)
        val siteModel = mock<SiteModel>()
        val mediaStore = createMediaStore(resultForGetSiteMediaWithId = (remoteIds[1] to null))
        // Act
        val result = createGetMediaModelUseCase(mediaStore = mediaStore)
                .loadMediaByRemoteId(siteModel, remoteIds)
        // Assert
        assertThat(result.size).isEqualTo(remoteIds.size - 1)
    }

    @Test
    fun `loadingSomeMediaFailed is true when getRealPathFromUri returns null`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper((uris[1] to null))

        // Act
        val result = createGetMediaModelUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .createMediaModelFromUri(LOCAL_SITE_ID, uris)

        // Assert
        assertThat(result.loadingSomeMediaFailed).isTrue()
    }

    @Test
    fun `loadingSomeMediaFailed is false when getRealPathFromUri does not return null`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper()

        // Act
        val result = createGetMediaModelUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .createMediaModelFromUri(LOCAL_SITE_ID, uris)

        // Assert
        assertThat(result.loadingSomeMediaFailed).isFalse()
    }

    @Test
    fun `loadingSomeMediaFailed is true when File creation fails`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock())
        val fileProvider = createFileProvider(createMockedFile(false))
        // Act
        val result = createGetMediaModelUseCase(fileProvider = fileProvider)
                .createMediaModelFromUri(LOCAL_SITE_ID, uris)

        // Assert
        assertThat(result.loadingSomeMediaFailed).isTrue()
    }

    @Test
    fun `createMediaModelFromUri creates mediaModels and returns them`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        // Act
        val result = createGetMediaModelUseCase()
                .createMediaModelFromUri(LOCAL_SITE_ID, uris)

        // Assert
        assertThat(result.mediaModels.size).isEqualTo(uris.size)
    }

    @Test
    fun `createMediaModelFromUri filters out invalid uris`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForGetRealPath = (uris[1] to null))
        // Act
        val result = createGetMediaModelUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .createMediaModelFromUri(LOCAL_SITE_ID, uris)

        // Assert
        assertThat(result.mediaModels.size).isEqualTo(uris.size - 1)
    }

    @Test
    fun `createMediaFromUri sets video thumbnail`() = test {
        // Arrange
        val expectedThumbnail = "expected_thumbnail"
        val uris = listOf<Uri>(mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(
                resultForIsVideoMimeType = true,
                resultForGetVideoThumbnail = expectedThumbnail
        )

        // Act
        val result = createGetMediaModelUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .createMediaModelFromUri(LOCAL_SITE_ID, uris)

        // Assert
        assertThat(result.mediaModels[0].thumbnailUrl).isEqualTo(expectedThumbnail)
    }

    private companion object Fixtures {
        private const val LOCAL_SITE_ID = 1

        private fun createGetMediaModelUseCase(
            fluxCUtilsWrapper: FluxCUtilsWrapper = createFluxCUtilsWrapper(),
            mediaUtilsWrapper: MediaUtilsWrapper = createMediaUtilsWrapper(),
            fileProvider: FileProvider = createFileProvider(),
            mediaStore: MediaStore = createMediaStore()
        ): GetMediaModelUseCase {
            return GetMediaModelUseCase(
                    fluxCUtilsWrapper,
                    mediaUtilsWrapper,
                    mediaStore,
                    fileProvider,
                    TEST_DISPATCHER
            )
        }

        private fun createMediaStore(
            resultForGetMediaWithLocalId: Pair<Int, MediaModel?>? = null,
            resultForGetSiteMediaWithId: Pair<Long, MediaModel?>? = null
        ) = mock<MediaStore> {
            on { getMediaWithLocalId(any()) }.thenReturn(mock())
            on { getSiteMediaWithId(any(), any()) }.thenReturn(mock())
            resultForGetMediaWithLocalId?.let {
                on { getMediaWithLocalId(resultForGetMediaWithLocalId.first) }.thenReturn(
                        resultForGetMediaWithLocalId.second
                )
            }
            resultForGetSiteMediaWithId?.let {
                on { getSiteMediaWithId(any(), eq(resultForGetSiteMediaWithId.first)) }.thenReturn(
                        resultForGetSiteMediaWithId.second
                )
            }
        }

        private fun createMediaUtilsWrapper(
            resultForGetRealPath: Pair<Uri, String?>? = null,
            resultForIsVideoMimeType: Boolean = false,
            resultForGetVideoThumbnail: String? = null
        ) =
                mock<MediaUtilsWrapper> {
                    on { isVideoMimeType(anyOrNull()) }.thenReturn(resultForIsVideoMimeType)
                    on { getRealPathFromURI(any()) }.thenReturn("")
                    on { getVideoThumbnail(any(), any()) }.thenReturn(resultForGetVideoThumbnail)
                    resultForGetRealPath?.let {
                        on { getRealPathFromURI(resultForGetRealPath.first) }.thenReturn(
                                resultForGetRealPath.second
                        )
                    }
                }

        private fun createFluxCUtilsWrapper() = mock<FluxCUtilsWrapper> {
            on { mediaModelFromLocalUri(any(), anyOrNull(), eq(LOCAL_SITE_ID)) }.thenReturn(
                    MediaModel()
            )
        }

        private fun createFileProvider(file: File = createMockedFile(true)) = mock<FileProvider> {
            on { createFile(any()) }.thenReturn(file)
        }

        private fun createMockedFile(fileExists: Boolean) = mock<File> {
            on { exists() }.thenReturn(fileExists)
        }
    }
}
