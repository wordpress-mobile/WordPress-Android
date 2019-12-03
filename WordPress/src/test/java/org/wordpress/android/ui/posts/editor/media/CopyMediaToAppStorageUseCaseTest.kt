package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.util.MediaUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
@UseExperimental(InternalCoroutinesApi::class)
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
        verify(mediaUtilsWrapper, never()).copyFileToAppStorage(any())
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
        verify(mediaUtilsWrapper).copyFileToAppStorage(uris[0])
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
        fun createCopyMediaToAppStorageUseCase(mediaUtilsWrapper: MediaUtilsWrapper = createMediaUtilsWrapper()) =
                CopyMediaToAppStorageUseCase(mediaUtilsWrapper, TEST_DISPATCHER)

        fun createMediaUtilsWrapper(
            resultForIsInMediaStore: Boolean = false,
            resultForCopiedFileUri: Pair<Uri, Uri?>? = null
        ) =
                mock<MediaUtilsWrapper> {
                    on { isInMediaStore(any()) }.thenReturn(resultForIsInMediaStore)
                    on { copyFileToAppStorage(any()) }.thenReturn(mock())
                    resultForCopiedFileUri?.let {
                        on { copyFileToAppStorage(resultForCopiedFileUri.first) }.thenReturn(
                                resultForCopiedFileUri.second
                        )
                    }
                }
    }
}
