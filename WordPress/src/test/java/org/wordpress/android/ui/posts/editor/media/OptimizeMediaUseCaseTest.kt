package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.EditorTracker
import org.wordpress.android.util.MediaUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
@InternalCoroutinesApi
class OptimizeMediaUseCaseTest : BaseUnitTest() {
    @Test
    fun `Uri filtered out when getRealPathFromURI returns null`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForGetRealPath = uris[1] to null)
        // Act
        val optimizeMediaResult = createOptimizeMediaUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .optimizeMediaIfSupportedAsync(SiteModel(), FRESHLY_TAKEN, uris)
        // Assert
        assertThat(optimizeMediaResult.optimizedMediaUris.size).isEqualTo(uris.size - 1)
    }

    @Test
    fun `LoadingSomeMediaFailed set to true when getRealPathFromURI returns null`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForGetRealPath = uris[1] to null)
        // Act
        val optimizeMediaResult = createOptimizeMediaUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .optimizeMediaIfSupportedAsync(SiteModel(), FRESHLY_TAKEN, uris)
        // Assert
        assertThat(optimizeMediaResult.loadingSomeMediaFailed).isTrue()
    }

    @Test
    fun `LoadingSomeMediaFailed set to false on success`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        // Act
        val optimizeMediaResult = createOptimizeMediaUseCase()
                .optimizeMediaIfSupportedAsync(SiteModel(), FRESHLY_TAKEN, uris)
        // Assert
        assertThat(optimizeMediaResult.loadingSomeMediaFailed).isFalse()
    }

    @Test
    fun `Optimization enabled, result contains optimized uris`() = test {
        // Arrange
        val uris = listOf<Uri>(mock())
        val optimizedUri = mock<Uri>()
        // OptimizeMediaResult is not null => optimization supported
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForGetOptimizeMedia = optimizedUri)

        // Act
        val optimizeMediaResult = createOptimizeMediaUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .optimizeMediaIfSupportedAsync(SiteModel(), FRESHLY_TAKEN, uris)

        // Assert
        assertThat(optimizeMediaResult.optimizedMediaUris).isEqualTo(listOf(optimizedUri))
        assertThat(optimizeMediaResult.optimizedMediaUris).isNotEqualTo(uris)
    }

    @Test
    fun `Optimization disabled, is WPCom, result contains original uris`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        val siteModel = SiteModel().apply { setIsWPCom(true) }
        // OptimizeMediaResult is null => optimization disabled
        val mediaUtilsWrapper = createMediaUtilsWrapper(resultForGetOptimizeMedia = null)

        // Act
        val optimizeMediaResult = createOptimizeMediaUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .optimizeMediaIfSupportedAsync(siteModel, FRESHLY_TAKEN, uris)

        // Assert
        assertThat(optimizeMediaResult.optimizedMediaUris).isEqualTo(uris)
    }

    @Test
    fun `Optimization disabled, self-hosted, result contains uris with fixed orientation`() =
            test {
                // Arrange
                val uris = listOf<Uri>(mock())
                val fixedOrientationUri = mock<Uri>()
                // Is self-hosted
                val siteModel = SiteModel().apply { setIsWPCom(false) }
                // OptimizeMediaResult is null => optimization disabled
                val mediaUtilsWrapper = createMediaUtilsWrapper(
                        resultForGetOptimizeMedia = null,
                        resultForFixOrientation = fixedOrientationUri
                )

                // Act
                val optimizeMediaResult = createOptimizeMediaUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                        .optimizeMediaIfSupportedAsync(siteModel, FRESHLY_TAKEN, uris)

                // Assert
                assertThat(optimizeMediaResult.optimizedMediaUris).isEqualTo(
                        listOf(
                                fixedOrientationUri
                        )
                )
                assertThat(optimizeMediaResult.optimizedMediaUris).isNotEqualTo(uris)
            }

    @Test
    fun `Optimization disabled, self-hosted, fixOrientationIssue returns null, result contains original uris`() =
            test {
                // Arrange
                val uris = listOf<Uri>(mock())
                val fixedOrientationUri = null
                // Is self-hosted
                val siteModel = SiteModel().apply { setIsWPCom(false) }
                // OptimizeMediaResult is null => optimization disabled
                val mediaUtilsWrapper = createMediaUtilsWrapper(
                        resultForGetOptimizeMedia = null,
                        resultForFixOrientation = fixedOrientationUri
                )

                // Act
                val optimizeMediaResult = createOptimizeMediaUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                        .optimizeMediaIfSupportedAsync(siteModel, FRESHLY_TAKEN, uris)

                // Assert
                assertThat(optimizeMediaResult.optimizedMediaUris).isEqualTo(uris)
            }

    private companion object Fixtures {
        private const val FRESHLY_TAKEN = false

        private fun createOptimizeMediaUseCase(
            editorTracker: EditorTracker = mock(),
            mediaUtilsWrapper: MediaUtilsWrapper = createMediaUtilsWrapper()
        ): OptimizeMediaUseCase {
            return OptimizeMediaUseCase(editorTracker, mediaUtilsWrapper, TEST_DISPATCHER)
        }

        private fun createMediaUtilsWrapper(
            resultForGetRealPath: Pair<Uri, String?>? = null,
            resultForGetOptimizeMedia: Uri? = mock(),
            resultForFixOrientation: Uri? = mock()
        ) =
                mock<MediaUtilsWrapper> {
                    on { getOptimizedMedia(any(), any()) }.thenReturn(resultForGetOptimizeMedia)
                    on { fixOrientationIssue(any(), any()) }.thenReturn(resultForFixOrientation)
                    on { getRealPathFromURI(any()) }.thenReturn("")
                    resultForGetRealPath?.let {
                        on { getRealPathFromURI(resultForGetRealPath.first) }.thenReturn(
                                resultForGetRealPath.second
                        )
                    }
                }
    }
}
