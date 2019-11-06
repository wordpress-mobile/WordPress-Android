package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.posts.editor.EditorTracker
import org.wordpress.android.util.MediaUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
@UseExperimental(InternalCoroutinesApi::class)
class OptimizeMediaUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Test
    fun `Uri filtered out when getRealPathFromURI returns null`() = test {
        // Arrange
        val uris = listOf<Uri>(mock(), mock(), mock())
        val mediaUtilsWrapper = createMediaUtilsWrapper(uris[1] to null)
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
        val mediaUtilsWrapper = createMediaUtilsWrapper(uris[1] to null)
        // Act
        val optimizeMediaResult = createOptimizeMediaUseCase(mediaUtilsWrapper = mediaUtilsWrapper)
                .optimizeMediaIfSupportedAsync(SiteModel(), FRESHLY_TAKEN, uris)
        // Assert
        assertThat(optimizeMediaResult.loadingSomeMediaFailed).isTrue()
    }

    @Test
    fun `LoadingSomeMediaFailed set to false when getRealPathFromURI returns path`() = test {
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
        val mediaUtilsWrapper = createMediaUtilsWrapper(optimizeMediaResult = optimizedUri)

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
        val mediaUtilsWrapper = createMediaUtilsWrapper(optimizeMediaResult = null)

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
                        optimizeMediaResult = null,
                        fixedOrientationResult = fixedOrientationUri
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
                        optimizeMediaResult = null,
                        fixedOrientationResult = fixedOrientationUri
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
            realPathResult: Pair<Uri, String?>? = null,
            optimizeMediaResult: Uri? = mock(),
            fixedOrientationResult: Uri? = mock()
        ) =
                mock<MediaUtilsWrapper> {
                    on { getOptimizedMedia(any(), any()) }.thenReturn(optimizeMediaResult)
                    on { fixOrientationIssue(any(), any()) }.thenReturn(fixedOrientationResult)
                    on { getRealPathFromURI(any()) }.thenReturn("")
                    realPathResult?.let {
                        on { getRealPathFromURI(realPathResult.first) }.thenReturn(
                                realPathResult.second
                        )
                    }
                }
    }
}
