package org.wordpress.android.ui.stories

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.TestContent
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class SaveStoryGutenbergBlockUseCaseTest {
    private lateinit var saveStoryGutenbergBlockUseCase: SaveStoryGutenbergBlockUseCase
    @Mock lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var mediaFile: MediaFile
    @Mock lateinit var mediaFile2: MediaFile
    lateinit var postModel: PostModel

    @Before
    fun setUp() {
        saveStoryGutenbergBlockUseCase = SaveStoryGutenbergBlockUseCase()
    }

    @Test
    fun `replaceLocalMediaIdsWithRemoteMediaIdsInPost replaces local id and url for given mediaFile in Story block`() {
        // arrange
        whenever(mediaFile.id).thenReturn(TestContent.localMediaId.toInt())
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        postModel = PostModel()
        postModel.setContent(TestContent.storyBlockWithLocalIdsAndUrls)

        // act
        saveStoryGutenbergBlockUseCase.replaceLocalMediaIdsWithRemoteMediaIdsInPost(postModel, mediaFile)

        // assert
        Assertions.assertThat(postModel.content).isEqualTo(TestContent.storyBlockWithFirstRemoteIdsAndUrlsReplaced)
    }

    @Test
    fun `buildJetpackStoryBlockInPost sets the Post content to a Story block with local ids and urls`() {
        // arrange
        postModel = PostModel()
        whenever(editPostRepository.update<PostModel>(any())).then {
            val action: (PostModel) -> Boolean = it.getArgument(0)
            action(postModel)
            null
        }
        whenever(editPostRepository.getPost()).thenReturn(postModel)

        whenever(mediaFile.id).thenReturn(TestContent.localMediaId.toInt())
        whenever(mediaFile.mediaId).thenReturn(TestContent.localMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.localImageUrl)
        whenever(mediaFile.mimeType).thenReturn(TestContent.storyMediaFileMimeTypeImage)

        whenever(mediaFile2.id).thenReturn(TestContent.localMediaId2.toInt())
        whenever(mediaFile2.mediaId).thenReturn(TestContent.localMediaId2)
        whenever(mediaFile2.fileURL).thenReturn(TestContent.localImageUrl2)
        whenever(mediaFile2.mimeType).thenReturn(TestContent.storyMediaFileMimeTypeImage)

        val mediaFiles = HashMap<String, MediaFile>()
        mediaFiles.put(TestContent.localMediaId, mediaFile)
        mediaFiles.put(TestContent.localMediaId2, mediaFile2)

        // act
        saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockInPost(editPostRepository, mediaFiles)

        // assert
        Assertions.assertThat(postModel.content).isEqualTo(TestContent.storyBlockWithLocalIdsAndUrls)
    }
}
