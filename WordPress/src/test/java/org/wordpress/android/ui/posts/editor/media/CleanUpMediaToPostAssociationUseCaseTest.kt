package org.wordpress.android.ui.posts.editor.media

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.any
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload
import org.wordpress.android.test
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.posts.editor.AztecEditorFragmentStaticWrapper
import org.wordpress.android.ui.posts.editor.media.CleanUpMediaToPostAssociationUseCaseTest.Fixtures.createAppPrefsWrapper
import org.wordpress.android.ui.posts.editor.media.CleanUpMediaToPostAssociationUseCaseTest.Fixtures.createAztecEditorWrapper
import org.wordpress.android.ui.posts.editor.media.CleanUpMediaToPostAssociationUseCaseTest.Fixtures.createMediaList
import org.wordpress.android.ui.posts.editor.media.CleanUpMediaToPostAssociationUseCaseTest.Fixtures.createPostUtilsWrapper
import org.wordpress.android.ui.posts.editor.media.CleanUpMediaToPostAssociationUseCaseTest.Fixtures.createUploadStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@InternalCoroutinesApi
@RunWith(Parameterized::class)
class CleanUpMediaToPostAssociationUseCaseTest(
    private val aztecEnabled: Boolean,
    private val gutenbergEnabled: Boolean
) : BaseUnitTest() {
    @Test
    fun `media which are NOT in post are cleared`() = test {
        // Arrange
        val mediaList = createMediaList()
        val uploadStore = createUploadStore(
                failedMedia = setOf(mediaList[0]),
                uploadedMedia = setOf(mediaList[1]),
                uploadingMedia = setOf(mediaList[2])
        )
        val mediaInPost = mediaList.slice(3..9).toSet()
        val dispatcher = mock<Dispatcher>()
        // Act
        createUseCase(
                dispatcher,
                uploadStore,
                mediaInPost
        ).purgeMediaToPostAssociationsIfNotInPostAnymore(mock())

        // Assert
        val captor = argumentCaptor<Action<ClearMediaPayload>>()
        verify(dispatcher).dispatch(captor.capture())
        assertThat(captor.firstValue.payload.media)
                .isEqualTo(setOf(mediaList[0], mediaList[1], mediaList[2]))
    }

    @Test
    fun `media which are in post are NOT cleared`() = test {
        // Arrange
        val mediaList = createMediaList()

        val dispatcher = mock<Dispatcher>()
        val uploadStore = createUploadStore(
                failedMedia = setOf(mediaList[0]),
                uploadedMedia = setOf(mediaList[1]),
                uploadingMedia = setOf(mediaList[2])
        )
        // Act
        createUseCase(
                dispatcher,
                uploadStore,
                mediaInPost = mediaList.toSet()
        ).purgeMediaToPostAssociationsIfNotInPostAnymore(mock())

        // Assert
        verify(dispatcher, never()).dispatch(any<Action<ClearMediaPayload>>())
    }

    @Test
    fun `featured images are NOT cleared even though they are NOT in post`() = test {
        // Arrange
        val mediaList = createMediaList()
        val dispatcher = mock<Dispatcher>()
        val uploadStore = createUploadStore(
                failedMedia = setOf(mediaList[0]),
                uploadedMedia = setOf(mediaList[1]),
                uploadingMedia = setOf(mediaList[2])
        )
        mediaList[0].markedLocallyAsFeatured = true
        val mediaInPost = mediaList.slice(3..9).toSet()
        // Act
        createUseCase(
                dispatcher,
                uploadStore,
                mediaInPost
        ).purgeMediaToPostAssociationsIfNotInPostAnymore(mock())

        // Assert
        val captor = argumentCaptor<Action<ClearMediaPayload>>()
        verify(dispatcher).dispatch(captor.capture())
        assertThat(captor.firstValue.payload.media)
                .isEqualTo(setOf(mediaList[1], mediaList[2]))
    }

    private fun createUseCase(
        dispatcher: Dispatcher = mock(),
        uploadStore: UploadStore = mock(),
        mediaInPost: Set<MediaModel>
    ) = CleanUpMediaToPostAssociationUseCase(
            dispatcher,
            uploadStore,
            createAppPrefsWrapper(aztecEnabled, gutenbergEnabled),
            createAztecEditorWrapper(mediaInPost),
            createPostUtilsWrapper(mediaInPost),
            TEST_DISPATCHER
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = listOf(
                arrayOf(true, true), // aztec enabled, gutenberg enabled
                arrayOf(true, false), // aztec enabled, gutenberg disabled
                arrayOf(false, true) // aztec disabled, gutenberg enabled
                // arrayOf(false, false) is an invalid option, one of the editors must be enabled
        )
    }

    private object Fixtures {
        fun createUploadStore(
            failedMedia: Set<MediaModel> = setOf(),
            uploadedMedia: Set<MediaModel> = setOf(),
            uploadingMedia: Set<MediaModel> = setOf()
        ) = mock<UploadStore> {
            on { getFailedMediaForPost(any()) }.thenReturn(failedMedia)
            on { getCompletedMediaForPost(any()) }.thenReturn(uploadedMedia)
            on { getUploadingMediaForPost(any()) }.thenReturn(uploadingMedia)
        }

        fun createAppPrefsWrapper(aztecEnabled: Boolean, gutenbergEnabled: Boolean) =
                mock<AppPrefsWrapper> {
                    on { isAztecEditorEnabled }.thenReturn(aztecEnabled)
                    on { isGutenbergEditorEnabled() }.thenReturn(gutenbergEnabled)
                }

        fun createPostUtilsWrapper(mediaInPost: Set<MediaModel>) =
                mock<PostUtilsWrapper> {
                    on { isMediaInGutenbergPostBody(anyOrNull(), anyOrNull()) }
                            .doAnswer { invocation ->
                                mediaInPost.map { it.id }
                                        .contains((invocation.arguments[1] as String).toInt())
                            }
                }

        fun createAztecEditorWrapper(mediaInPost: Set<MediaModel>) =
                mock<AztecEditorFragmentStaticWrapper> {
                    on { isMediaInPostBody(anyOrNull(), anyOrNull()) }
                            .doAnswer { invocation ->
                                mediaInPost.map { it.id }
                                        .contains((invocation.arguments[1] as String).toInt())
                            }
                }

        fun createMediaList(): List<MediaModel> {
            val list = mutableListOf<MediaModel>()
            for (i in 1..10) {
                list.add(MediaModel().apply { id = i })
            }
            return list
        }
    }
}
