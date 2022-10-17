package org.wordpress.android.ui.stories

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.wordpress.stories.compose.story.StoryFrameItem
import kotlinx.coroutines.InternalCoroutinesApi
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.TestContent
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.Companion.TEMPORARY_ID_PREFIX
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.DoWithMediaFilesListener
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.StoryMediaFileData
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class SaveStoryGutenbergBlockUseCaseTest : BaseUnitTest() {
    private lateinit var saveStoryGutenbergBlockUseCase: SaveStoryGutenbergBlockUseCase
    private lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var storiesPrefs: StoriesPrefs
    @Mock lateinit var crashLogging: CrashLogging
    @Mock lateinit var context: Context
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var mediaFile: MediaFile
    @Mock lateinit var mediaFile2: MediaFile

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        saveStoryGutenbergBlockUseCase = SaveStoryGutenbergBlockUseCase(storiesPrefs, crashLogging)
        editPostRepository = EditPostRepository(
                mock(),
                postStore,
                mock(),
                mock(),
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `post with empty Story block is given an empty mediaFiles array`() {
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
    fun `verify all properties of mediaFileData that are created from buildMediaFileDataWithTemporaryId are correct`() {
        // Given
        val mediaFileId = 1
        val mediaFile = getMediaFile(mediaFileId)

        // When
        val mediaFileData = saveStoryGutenbergBlockUseCase.buildMediaFileDataWithTemporaryId(
                mediaFile,
                TEMPORARY_ID_PREFIX + mediaFileId
        )

        // Then
        Assertions.assertThat(mediaFileData.alt).isEqualTo("")
        Assertions.assertThat(mediaFileData.id).isEqualTo(TEMPORARY_ID_PREFIX + mediaFileId)
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
        val mediaFile = getMediaFile(1)
        val postModel = PostModel()
        postModel.setContent(BLOCK_WITH_NON_EMPTY_MEDIA_FILES)
        val siteModel = SiteModel()

        // When
        saveStoryGutenbergBlockUseCase.replaceLocalMediaIdsWithRemoteMediaIdsInPost(
                postModel,
                siteModel,
                mediaFile
        )

        // Then
        Assertions.assertThat(postModel.content).isEqualTo(BLOCK_WITH_NON_EMPTY_MEDIA_FILES_WITH_ONE_REMOTE_ID)
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

    @Test
    fun `replaceLocalMediaIdsWithRemoteMediaIdsInPost replaces local id and url for given mediaFile in Story block`() {
        // arrange
        whenever(mediaFile.id).thenReturn(TestContent.localMediaId.toInt())
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        val postModel = PostModel()
        postModel.setContent(TestContent.storyBlockWithLocalIdsAndUrls)
        val siteModel = SiteModel()

        // act
        saveStoryGutenbergBlockUseCase.replaceLocalMediaIdsWithRemoteMediaIdsInPost(postModel, siteModel, mediaFile)

        // assert
        Assertions.assertThat(postModel.content).isEqualTo(TestContent.storyBlockWithFirstRemoteIdsAndUrlsReplaced)
    }

    @Test
    fun `buildJetpackStoryBlockInPost sets the Post content to a Story block with local ids and urls`() {
        // arrange
        val postModel = PostModel()
        editPostRepository.set { postModel }

        whenever(mediaFile.id).thenReturn(TestContent.localMediaId.toInt())
        whenever(mediaFile.fileURL).thenReturn(TestContent.localImageUrl)
        whenever(mediaFile.mimeType).thenReturn(TestContent.storyMediaFileMimeTypeImage)
        whenever(mediaFile.alt).thenReturn("")

        whenever(mediaFile2.id).thenReturn(TestContent.localMediaId2.toInt())
        whenever(mediaFile2.fileURL).thenReturn(TestContent.localImageUrl2)
        whenever(mediaFile2.mimeType).thenReturn(TestContent.storyMediaFileMimeTypeImage)
        whenever(mediaFile2.alt).thenReturn("")

        val mediaFiles = ArrayList<MediaFile>()
        mediaFiles.add(mediaFile)
        mediaFiles.add(mediaFile2)

        // act
        saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockInPost(editPostRepository, mediaFiles)

        // assert
        Assertions.assertThat(postModel.content).isEqualTo(TestContent.storyBlockWithLocalIdsAndUrls)
    }

    @Test
    fun `post with a Story block with no mediaFiles is not taken into account for processing`() {
        // Given
        val siteModel = SiteModel()
        val postModel = PostModel()
        val listener: DoWithMediaFilesListener = mock()
        postModel.setContent(BLOCK_LACKING_MEDIA_FILES_ARRAY)

        // When
        saveStoryGutenbergBlockUseCase.findAllStoryBlocksInPostAndPerformOnEachMediaFilesJson(
                postModel,
                siteModel,
                mock()
        )

        // Then
        verify(listener, times(0)).doWithMediaFilesJson(any(), any())
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
                    mediaFile.alt = ""
                    mediaFile.fileURL = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg"
                    mediaFiles.add(mediaFile)
                }
                mediaFiles
            }
        }
    }

    private fun getMediaFile(id: Int): MediaFile {
        val mediaFile = MediaFile()
        mediaFile.id = id
        mediaFile.mediaId = (id + 1000).toString()
        mediaFile.mimeType = "image/jpeg"
        mediaFile.alt = ""
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
        private const val BLOCK_LACKING_MEDIA_FILES_ARRAY = "<!-- wp:jetpack/story -->\n" +
                "<div class=\"wp-story wp-block-jetpack-story\"></div>\n" +
                "<!-- /wp:jetpack/story -->"
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
