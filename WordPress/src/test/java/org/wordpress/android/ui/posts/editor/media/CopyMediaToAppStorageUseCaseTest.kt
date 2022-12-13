package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers.or
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.util.MediaUtilsWrapper

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CopyMediaToAppStorageUseCaseTest : BaseUnitTest() {
    @Test
    fun `do NOT copy files which are present in media store`() = test {
        // Arrange
        val uris = listOf<Uri>(mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForIsInMediaStore = true)
        // Act
        val result = createCopyMediaToAppStorageUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .copyFilesToAppStorageIfNecessary(uris)

        // Assert
        verify(mediaUtilsWrapper, never()).copyFileToAppStorage(any(), any())
        assertThat(result.permanentlyAccessibleUris[0]).isEqualTo(uris[0])
    }

    @Test
    fun `do NOT copy files which are local to the device`() = test {
        // Arrange
        val uris = listOf<Uri>(mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForIsFile = true)
        // Act
        val result = createCopyMediaToAppStorageUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .copyFilesToAppStorageIfNecessary(uris)

        // Assert
        verify(mediaUtilsWrapper, never()).copyFileToAppStorage(any(), any())
        assertThat(result.permanentlyAccessibleUris[0]).isEqualTo(uris[0])
    }

    @Test
    fun `copy only files which are NOT present in media store`() = test {
        // Arrange
        val expectedCopiedFileUri = mock<Uri>()
        val uris = listOf<Uri>(mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(
                resultForIsInMediaStore = false,
                resultForCopiedFileUri = (uris[0] to expectedCopiedFileUri)
        )
        // Act
        val result = createCopyMediaToAppStorageUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .copyFilesToAppStorageIfNecessary(uris)

        // Assert
        verify(mediaUtilsWrapper).copyFileToAppStorage(uris[0], null)
        assertThat(result.permanentlyAccessibleUris[0]).isEqualTo(expectedCopiedFileUri)
    }

    @Test
    fun `filters out uris which cannot be copied`() = test {
        // Arrange
        val uris = listOf<Uri>(mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForCopiedFileUri = (uris[0] to null))
        // Act
        val result = createCopyMediaToAppStorageUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .copyFilesToAppStorageIfNecessary(uris)

        // Assert
        assertThat(result.permanentlyAccessibleUris.size).isEqualTo(uris.size - 1)
    }

    @Test
    fun `copyingSomeMediaFailed is set to true when a file cannot be copied`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForCopiedFileUri = (uris[1] to null))
        // Act
        val result = createCopyMediaToAppStorageUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .copyFilesToAppStorageIfNecessary(uris)

        // Assert
        assertThat(result.copyingSomeMediaFailed).isTrue()
    }

    @Test
    fun `copyingSomeMediaFailed is set to false on success`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        // Act
        val result = createCopyMediaToAppStorageUseCase()
                .copyFilesToAppStorageIfNecessary(uris)
        // Assert
        assertThat(result.copyingSomeMediaFailed).isFalse()
    }

    private companion object Fixtures {
        @InternalCoroutinesApi
        fun createCopyMediaToAppStorageUseCase(
            mediaUtilsWrapper: MediaUtilsWrapper = createMediaUtilsWrapper(),
            authenticationUtils: AuthenticationUtils = createAuthenticationUtils()
        ) =
                CopyMediaToAppStorageUseCase(mediaUtilsWrapper, authenticationUtils, TEST_DISPATCHER)

        fun createMediaUtilsWrapper(
            resultForIsInMediaStore: Boolean = false,
            resultForIsFile: Boolean = false,
            resultForCopiedFileUri: Pair<Uri, Uri?>? = null
        ) =
                mock<MediaUtilsWrapper> {
                    on { isInMediaStore(any()) }.thenReturn(resultForIsInMediaStore)
                    on { isFile(any()) }.thenReturn(resultForIsFile)
                    on { copyFileToAppStorage(any(), or(isNull(), anyMap())) }.thenReturn(mock())
                    resultForCopiedFileUri?.let {
                        on { copyFileToAppStorage(resultForCopiedFileUri.first, null) }.thenReturn(
                                resultForCopiedFileUri.second
                        )
                    }
                }

        fun createAuthenticationUtils(
            resultForAuthHeader: Map<String, String>? = null
        ) =
                mock<AuthenticationUtils> {
                    on { getAuthHeaders(any()) }.thenReturn(resultForAuthHeader)
                }
    }
}
