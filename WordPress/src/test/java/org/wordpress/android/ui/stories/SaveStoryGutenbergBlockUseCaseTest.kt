package org.wordpress.android.ui.stories

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.wordpress.stories.compose.story.StoryFrameItem
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.Companion.TEMPORARY_ID_PREFIX
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.StoryMediaFileData
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class SaveStoryGutenbergBlockUseCaseTest : BaseUnitTest() {
    private lateinit var saveStoryGutenbergBlockUseCase: SaveStoryGutenbergBlockUseCase
    private lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var storiesPrefs: StoriesPrefs
    @Mock lateinit var context: Context
    @Mock lateinit var postStore: PostStore

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        saveStoryGutenbergBlockUseCase = SaveStoryGutenbergBlockUseCase(storiesPrefs)
        editPostRepository = EditPostRepository(
                mock(),
                postStore,
                mock(),
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `post with empty Story block is set given an empty mediaFiles array`() {
        // Given
        val mediaFiles: ArrayList<MediaFile> = setupFluxCMediaFiles(emptyList = true)
        editPostRepository.set { PostModel() }

        // When
        saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockInPost(
                editPostRepository,
                mediaFiles
        )

        // Then
        Assertions.assertThat(editPostRepository.content).isEqualTo(BLOCK_WITH_EMPTY_MEDIA_FILES)
    }

    @Test
    fun `post with non-empty Story block is set given a non-empty mediaFiles array`() {
        // Given
        val mediaFiles: ArrayList<MediaFile> = setupFluxCMediaFiles(emptyList = false)
        editPostRepository.set { PostModel() }

        // When
        saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockInPost(
                editPostRepository,
                mediaFiles
        )

        // Then
        Assertions.assertThat(editPostRepository.content).isEqualTo(BLOCK_WITH_NON_EMPTY_MEDIA_FILES)
    }

    @Test
    fun `builds non-empty story block string from non-empty mediaFiles array`() {
        // Given
        val mediaFileDataList: ArrayList<StoryMediaFileData> = setupMediaFileDataList(emptyList = false)

        // When
        val result = saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockStringFromStoryMediaFileData(
                mediaFileDataList
        )

        // Then
        Assertions.assertThat(result).isEqualTo(BLOCK_WITH_NON_EMPTY_MEDIA_FILES)
    }

    @Test
    fun `builds empty story block string from empty mediaFiles array`() {
        // Given
        val mediaFileDataList: ArrayList<StoryMediaFileData> = setupMediaFileDataList(emptyList = true)

        // When
        val result = saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockStringFromStoryMediaFileData(
                mediaFileDataList
        )

        // Then
        Assertions.assertThat(result).isEqualTo(BLOCK_WITH_EMPTY_MEDIA_FILES)
    }

    @Test
    fun `obtain a mediaFileData from a MediaFile with a temporary id`() {
        // Given
        val mediaFile = getOneMediaFile(1)

        // When
        val mediaFileData = saveStoryGutenbergBlockUseCase.buildMediaFileDataWithTemporaryId(
                mediaFile,
                TEMPORARY_ID_PREFIX + 1
        )

        // Then
        Assertions.assertThat(mediaFileData.alt).isEqualTo("")
        Assertions.assertThat(mediaFileData.id).isEqualTo(TEMPORARY_ID_PREFIX + 1)
        Assertions.assertThat(mediaFileData.link).isEqualTo(
                "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg"
        )
        Assertions.assertThat(mediaFileData.type).isEqualTo("image")
        Assertions.assertThat(mediaFileData.mime).isEqualTo(mediaFile.mimeType)
        Assertions.assertThat(mediaFileData.caption).isEqualTo("")
        Assertions.assertThat(mediaFileData.url).isEqualTo(mediaFile.fileURL)
    }

    @Test
    fun `local media id is found and gets replaced with remote media id`() {
        // Given
        val mediaFile = getOneMediaFile(1)
        val onePost = PostModel()
        onePost.setContent(BLOCK_WITH_NON_EMPTY_MEDIA_FILES)

        // When
        saveStoryGutenbergBlockUseCase.replaceLocalMediaIdsWithRemoteMediaIdsInPost(
                onePost,
                mediaFile
        )

        // Then
        Assertions.assertThat(onePost.content).isEqualTo(BLOCK_WITH_NON_EMPTY_MEDIA_FILES_WITH_ONE_REMOTE_ID)
    }

    @Test
    fun `slides are saved locally to storiedPrefs`() {
        // Given
        val frames = ArrayList<StoryFrameItem>()
        frames.add(getOneStoryFrameItem("1"))
        frames.add(getOneStoryFrameItem("2"))
        frames.add(getOneStoryFrameItem("3"))

        // When
        saveStoryGutenbergBlockUseCase.saveNewLocalFilesToStoriesPrefsTempSlides(
                mock(),
                0,
                frames
        )

        // Then
        verify(storiesPrefs, times(3)).saveSlideWithTempId(any(), any(), any())
    }

    private fun setupFluxCMediaFiles(
        emptyList: Boolean
    ): ArrayList<MediaFile> {
        return when (emptyList) {
            true -> ArrayList()
            false -> {
                val mediaFiles = ArrayList<MediaFile>()
                for (i in 1..10) {
                    val mediaFile = MediaFile()
                    mediaFile.id = i
                    mediaFile.mediaId = (i + 1000).toString()
                    mediaFile.mimeType = "image/jpeg"
                    mediaFile.fileURL = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg"
                    mediaFiles.add(mediaFile)
                }
                mediaFiles
            }
        }
    }

    private fun getOneMediaFile(id: Int): MediaFile {
        val mediaFile = MediaFile()
        mediaFile.id = id
        mediaFile.mediaId = (id + 1000).toString()
        mediaFile.mimeType = "image/jpeg"
        mediaFile.fileURL = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg"
        return mediaFile
    }

    private fun getOneStoryFrameItem(id: String): StoryFrameItem {
        return StoryFrameItem(
                source = mock(),
                id = id
        )
    }

    private fun setupMediaFileDataList(
        emptyList: Boolean
    ): ArrayList<StoryMediaFileData> {
        when (emptyList) {
            true -> return ArrayList()
            false -> {
                val mediaFiles = ArrayList<StoryMediaFileData>()
                for (i in 1..10) {
                    val mediaFile = StoryMediaFileData(
                            id = i.toString(),
                            mime = "image/jpeg",
                            link = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg",
                            url = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg",
                            alt = "",
                            type = "image",
                            caption = ""
                    )
                    mediaFiles.add(mediaFile)
                }
                return mediaFiles
            }
        }
    }

    companion object {
        private const val BLOCK_WITH_EMPTY_MEDIA_FILES = "<!-- wp:jetpack/story {\"mediaFiles\":[]} -->\n" +
                "<div class=\"wp-story wp-block-jetpack-story\"></div>\n" +
                "<!-- /wp:jetpack/story -->"
        private const val BLOCK_WITH_NON_EMPTY_MEDIA_FILES = "<!-- wp:jetpack/story " +
                "{\"mediaFiles\":[{\"alt\":\"\",\"id\":\"1\",\"link\":\"https://testsite.files." +
                "wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\"," +
                "\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"2\"," +
                "\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\"" +
                ":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000." +
                "jpg\"},{\"alt\":\"\",\"id\":\"3\",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000" +
                ".jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.files" +
                ".wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"4\",\"link\":\"https://testsite.file" +
                "s.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\"" +
                ",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"5\"" +
                ",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime" +
                "\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000" +
                ".jpg\"},{\"alt\":\"\",\"id\":\"6\",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-000000" +
                "0.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.file" +
                "s.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"7\",\"link\":\"https://testsite.fi" +
                "les.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":" +
                "\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":" +
                "\"8\",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\"," +
                "\"mime\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-" +
                "0000000.jpg\"},{\"alt\":\"\",\"id\":\"9\",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp" +
                "-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://tests" +
                "ite.files.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"10\",\"link\":\"https://te" +
                "stsite.files.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"ca" +
                "ption\":\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\"}]} -->\n" +
                "<div class=\"wp-story wp-block-jetpack-story\"></div>\n" +
                "<!-- /wp:jetpack/story -->"
        private const val BLOCK_WITH_NON_EMPTY_MEDIA_FILES_WITH_ONE_REMOTE_ID = "<!-- wp:jetpack/story " +
                "{\"mediaFiles\":[{\"alt\":\"\",\"id\":\"1001\",\"link\":\"https://testsite.files." +
                "wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\"," +
                "\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"2\"," +
                "\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\"" +
                ":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000." +
                "jpg\"},{\"alt\":\"\",\"id\":\"3\",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000" +
                ".jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.files" +
                ".wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"4\",\"link\":\"https://testsite.file" +
                "s.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\"" +
                ",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"5\"" +
                ",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime" +
                "\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000" +
                ".jpg\"},{\"alt\":\"\",\"id\":\"6\",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-000000" +
                "0.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.file" +
                "s.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"7\",\"link\":\"https://testsite.fi" +
                "les.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":" +
                "\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":" +
                "\"8\",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\"," +
                "\"mime\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-" +
                "0000000.jpg\"},{\"alt\":\"\",\"id\":\"9\",\"link\":\"https://testsite.files.wordpress.com/2020/10/wp" +
                "-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"caption\":\"\",\"url\":\"https://tests" +
                "ite.files.wordpress.com/2020/10/wp-0000000.jpg\"},{\"alt\":\"\",\"id\":\"10\",\"link\":\"https://te" +
                "stsite.files.wordpress.com/2020/10/wp-0000000.jpg\",\"type\":\"image\",\"mime\":\"image/jpeg\",\"ca" +
                "ption\":\"\",\"url\":\"https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg\"}]} -->\n" +
                "<div class=\"wp-story wp-block-jetpack-story\"></div>\n" +
                "<!-- /wp:jetpack/story -->"
    }
}
