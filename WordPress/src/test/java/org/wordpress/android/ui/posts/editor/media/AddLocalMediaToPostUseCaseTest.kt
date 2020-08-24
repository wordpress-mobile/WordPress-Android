package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.posts.editor.media.CopyMediaToAppStorageUseCase.CopyMediaResult
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase.CreateMediaModelsResult
import org.wordpress.android.ui.posts.editor.media.OptimizeMediaUseCase.OptimizeMediaResult

@RunWith(MockitoJUnitRunner::class)
@UseExperimental(InternalCoroutinesApi::class)
class AddLocalMediaToPostUseCaseTest : BaseUnitTest() {
    @Test
    fun `addNewMediaToEditorAsync returns true on success`() = test {
        // Arrange
        val localUris = listOf<Uri>(mock(), mock())
        // Act
        val result = createAddLocalMediaToPostUseCase()
                .addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock())
        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `addNewMediaToEditorAsync calls addToEditorAndUpload when doUploadAfterAdding is true`() = test {
        // Arrange
        val doUploadAfterAdding = true

        val localUris = listOf<Uri>(mock(), mock())

        val appendMediaToEditorUseCase: AppendMediaToEditorUseCase = mock()
        val uploadMediaUseCase: UploadMediaUseCase = mock()
        val updateMediaModelUseCase: UpdateMediaModelUseCase = mock()

        // Act
        val useCase = createAddLocalMediaToPostUseCase(
                appendMediaToEditorUseCase = appendMediaToEditorUseCase,
                uploadMediaUseCase = uploadMediaUseCase,
                updateMediaModelUseCase = updateMediaModelUseCase
        )

        useCase.addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock(), doUploadAfterAdding)

        // Assert
        verify(appendMediaToEditorUseCase).addMediaToEditor(any(), any())
        verify(uploadMediaUseCase).saveQueuedPostAndStartUpload(any(), any())
        verify(updateMediaModelUseCase).updateMediaModel(any(), anyOrNull(), any())
    }

    @Test
    fun `addNewMediaToEditorAsync calls addMediaToEditor when doUploadAfterAdding is false`() = test {
        // Arrange
        val doUploadAfterAdding = false

        val localUris = listOf<Uri>(mock(), mock())

        val appendMediaToEditorUseCase: AppendMediaToEditorUseCase = mock()
        val uploadMediaUseCase: UploadMediaUseCase = mock()
        val updateMediaModelUseCase: UpdateMediaModelUseCase = mock()

        // Act
        val useCase = createAddLocalMediaToPostUseCase(
                appendMediaToEditorUseCase = appendMediaToEditorUseCase,
                uploadMediaUseCase = uploadMediaUseCase,
                updateMediaModelUseCase = updateMediaModelUseCase
        )

        useCase.addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock(), doUploadAfterAdding)

        // Assert
        verify(appendMediaToEditorUseCase).addMediaToEditor(any(), any())
        verify(uploadMediaUseCase, times(0)).saveQueuedPostAndStartUpload(any(), any())
        verify(updateMediaModelUseCase, times(0)).updateMediaModel(any(), anyOrNull(), any())
    }

    @Test
    fun `addNewMediaToEditorAsync returns false when optimization for a media fails`() = test {
        // Arrange
        val localUris = listOf<Uri>(mock(), mock())
        val optimizeMediaUseCase = createOptimizeMediaUseCase(
                createOptimizeMediaResult(
                        loadingSomeMediaFailed = true
                )
        )
        // Act
        val result = createAddLocalMediaToPostUseCase(optimizeMediaUseCase = optimizeMediaUseCase)
                .addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock())
        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `addNewMediaToEditorAsync returns false when copying files to app storage fails`() = test {
        // Arrange
        val localUris = listOf<Uri>(mock(), mock())
        val copyMediaToAppStorageUseCase = createCopyMediaToAppStorageUseCase(
                createCopyMediaResult(copyingSomeMediaFailed = true)
        )
        // Act
        val result = createAddLocalMediaToPostUseCase(copyMediaToAppStorageUseCase = copyMediaToAppStorageUseCase)
                .addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock())
        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `addNewMediaToEditorAsync returns false when creating media model fails`() = test {
        // Arrange
        val localUris = listOf<Uri>(mock(), mock())
        val getMediaModelUseCase = createGetMediaModelUseCase(
                createMediaModelResult(
                        loadingSomeMediaFailed = true
                )
        )
        // Act
        val result = createAddLocalMediaToPostUseCase(getMediaModelUseCase = getMediaModelUseCase)
                .addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock())
        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `Uris of copied files are passed to optimizeMediaUseCase`() = test {
        // Arrange
        val localUris = listOf<Uri>(mock(), mock())
        val urisOfCopiedFiles = listOf<Uri>(mock())
        val copyMediaToAppStorageUseCase = createCopyMediaToAppStorageUseCase(
                createCopyMediaResult(uris = urisOfCopiedFiles)
        )

        val optimizeMediaUseCase = createOptimizeMediaUseCase()
        // Act
        createAddLocalMediaToPostUseCase(
                copyMediaToAppStorageUseCase = copyMediaToAppStorageUseCase,
                optimizeMediaUseCase = optimizeMediaUseCase
        ).addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock())
        // Assert
        verify(optimizeMediaUseCase).optimizeMediaIfSupportedAsync(
                any(),
                eq(FRESHLY_TAKEN),
                eq(urisOfCopiedFiles)
        )
    }

    @Test
    fun `Uris of optimized files are passed to getMediaUseCase`() = test {
        // Arrange
        val localUris = listOf<Uri>(mock(), mock())
        val urisOfOptimizedFiles = listOf<Uri>(mock())
        val optimizeMediaUseCase = createOptimizeMediaUseCase(createOptimizeMediaResult(uris = urisOfOptimizedFiles))

        val getMediaModelUseCase = createGetMediaModelUseCase()
        // Act
        createAddLocalMediaToPostUseCase(
                optimizeMediaUseCase = optimizeMediaUseCase,
                getMediaModelUseCase = getMediaModelUseCase
        ).addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock())
        // Assert
        verify(getMediaModelUseCase).createMediaModelFromUri(
                eq(LOCAL_SITE_ID),
                eq(urisOfOptimizedFiles)
        )
    }

    @Test
    fun `MediaModel upload status is set to QUEUED`() = test {
        // Arrange
        val localUris = listOf<Uri>(mock(), mock())
        val updateMediaModelUseCase = mock<UpdateMediaModelUseCase>()
        // Act
        createAddLocalMediaToPostUseCase(updateMediaModelUseCase = updateMediaModelUseCase)
                .addNewMediaToEditorAsync(localUris, SITE_MODEL, FRESHLY_TAKEN, mock())
        // Assert
        verify(updateMediaModelUseCase).updateMediaModel(any(), anyOrNull(), eq(QUEUED))
    }

    @Test
    fun `verify invocation order in addNewMediaToEditorAsync`() = test {
        // Arrange
        val localUris = listOf<Uri>(mock(), mock())
        val getMediaModelUseCase = createGetMediaModelUseCase()
        val updateMediaModelUseCase = mock<UpdateMediaModelUseCase>()
        val appendMediaToEditorUseCase = mock<AppendMediaToEditorUseCase>()
        val uploadMediaUseCase = mock<UploadMediaUseCase>()
        val copyMediaToAppStorageUseCase = createCopyMediaToAppStorageUseCase()
        val optimizeMediaUseCase = createOptimizeMediaUseCase()

        val inOrder = inOrder(
                getMediaModelUseCase,
                updateMediaModelUseCase,
                appendMediaToEditorUseCase,
                uploadMediaUseCase,
                copyMediaToAppStorageUseCase,
                optimizeMediaUseCase
        )
        val siteModel = SiteModel().apply { id = LOCAL_SITE_ID }
        // Act
        createAddLocalMediaToPostUseCase(
                getMediaModelUseCase = getMediaModelUseCase,
                updateMediaModelUseCase = updateMediaModelUseCase,
                appendMediaToEditorUseCase = appendMediaToEditorUseCase,
                uploadMediaUseCase = uploadMediaUseCase,
                copyMediaToAppStorageUseCase = copyMediaToAppStorageUseCase,
                optimizeMediaUseCase = optimizeMediaUseCase
        ).addNewMediaToEditorAsync(localUris, siteModel, FRESHLY_TAKEN, mock())

        // Assert
        inOrder.verify(copyMediaToAppStorageUseCase).copyFilesToAppStorageIfNecessary(localUris)
        inOrder.verify(optimizeMediaUseCase).optimizeMediaIfSupportedAsync(any(), any(), any())
        inOrder.verify(getMediaModelUseCase)
                .createMediaModelFromUri(eq(LOCAL_SITE_ID), any<List<Uri>>())
        inOrder.verify(updateMediaModelUseCase)
                .updateMediaModel(any(), anyOrNull(), any())
        inOrder.verify(appendMediaToEditorUseCase).addMediaToEditor(any(), any())
        inOrder.verify(uploadMediaUseCase).saveQueuedPostAndStartUpload(any(), any())
    }

    @Test
    fun `verify invocation order in addLocalMediaToEditorAsync`() = test {
        // Arrange
        val localIds = listOf(1, 2, 3)
        val getMediaModelUseCase = createGetMediaModelUseCase()
        val updateMediaModelUseCase = mock<UpdateMediaModelUseCase>()
        val appendMediaToEditorUseCase = mock<AppendMediaToEditorUseCase>()
        val uploadMediaUseCase = mock<UploadMediaUseCase>()

        val inOrder = inOrder(
                getMediaModelUseCase,
                updateMediaModelUseCase,
                appendMediaToEditorUseCase,
                uploadMediaUseCase
        )
        // Act
        createAddLocalMediaToPostUseCase(
                getMediaModelUseCase = getMediaModelUseCase,
                updateMediaModelUseCase = updateMediaModelUseCase,
                appendMediaToEditorUseCase = appendMediaToEditorUseCase,
                uploadMediaUseCase = uploadMediaUseCase
        ).addLocalMediaToEditorAsync(localIds, mock())
        // Assert
        inOrder.verify(getMediaModelUseCase).loadMediaByLocalId(localIds)
        inOrder.verify(updateMediaModelUseCase, times(localIds.size))
                .updateMediaModel(any(), anyOrNull(), any())
        inOrder.verify(appendMediaToEditorUseCase).addMediaToEditor(any(), any())
        inOrder.verify(uploadMediaUseCase).saveQueuedPostAndStartUpload(any(), any())
    }

    private companion object Fixtures {
        private const val LOCAL_SITE_ID = 1
        private const val FRESHLY_TAKEN = false
        private val SITE_MODEL = SiteModel().apply { id = LOCAL_SITE_ID }
        fun createAddLocalMediaToPostUseCase(
            copyMediaToAppStorageUseCase: CopyMediaToAppStorageUseCase = createCopyMediaToAppStorageUseCase(),
            optimizeMediaUseCase: OptimizeMediaUseCase = createOptimizeMediaUseCase(),
            getMediaModelUseCase: GetMediaModelUseCase = createGetMediaModelUseCase(),
            updateMediaModelUseCase: UpdateMediaModelUseCase = mock(),
            appendMediaToEditorUseCase: AppendMediaToEditorUseCase = mock(),
            uploadMediaUseCase: UploadMediaUseCase = mock()
        ): AddLocalMediaToPostUseCase {
            return AddLocalMediaToPostUseCase(
                    copyMediaToAppStorageUseCase = copyMediaToAppStorageUseCase,
                    optimizeMediaUseCase = optimizeMediaUseCase,
                    getMediaModelUseCase = getMediaModelUseCase,
                    updateMediaModelUseCase = updateMediaModelUseCase,
                    appendMediaToEditorUseCase = appendMediaToEditorUseCase,
                    uploadMediaUseCase = uploadMediaUseCase
            )
        }

        fun createCopyMediaToAppStorageUseCase(copyMediaResult: CopyMediaResult = createCopyMediaResult()) =
                mock<CopyMediaToAppStorageUseCase> {
                    onBlocking { copyFilesToAppStorageIfNecessary(any()) }.thenReturn(
                            copyMediaResult
                    )
                }

        private fun createCopyMediaResult(
            copyingSomeMediaFailed: Boolean = false,
            uris: List<Uri> = listOf(mock())
        ) = mock<CopyMediaResult> {
            on { this.permanentlyAccessibleUris }.thenReturn(uris)
            on { this.copyingSomeMediaFailed }.thenReturn(copyingSomeMediaFailed)
        }

        fun createOptimizeMediaUseCase(optimizeMediaResult: OptimizeMediaResult = createOptimizeMediaResult()) =
                mock<OptimizeMediaUseCase> {
                    onBlocking {
                        optimizeMediaIfSupportedAsync(
                                any(),
                                eq(FRESHLY_TAKEN),
                                any()
                        )
                    }.thenReturn(optimizeMediaResult)
                }

        fun createOptimizeMediaResult(
            loadingSomeMediaFailed: Boolean = false,
            uris: List<Uri> = listOf(mock())
        ): OptimizeMediaResult {
            return OptimizeMediaResult(uris, loadingSomeMediaFailed)
        }

        fun createGetMediaModelUseCase(createMediaModelResult: CreateMediaModelsResult = createMediaModelResult()) =
                mock<GetMediaModelUseCase> {
                    onBlocking { loadMediaByLocalId(any()) }.thenAnswer { invocation ->
                        val result = mutableListOf<MediaModel>()
                        (invocation.getArgument(0) as List<Int>).forEach { result.add(mock()) }
                        result
                    }
                    onBlocking {
                        createMediaModelFromUri(eq(LOCAL_SITE_ID), any<List<Uri>>())
                    }.thenReturn(createMediaModelResult)
                }

        fun createMediaModelResult(loadingSomeMediaFailed: Boolean = false): CreateMediaModelsResult {
            return CreateMediaModelsResult(listOf(mock()), loadingSomeMediaFailed)
        }
    }
}
