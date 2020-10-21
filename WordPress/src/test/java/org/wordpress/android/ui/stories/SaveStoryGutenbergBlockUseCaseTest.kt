package org.wordpress.android.ui.stories

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.Companion.TEMPORARY_ID_PREFIX
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.StoryMediaFileData
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class SaveStoryGutenbergBlockUseCaseTest: BaseUnitTest()  {
    private lateinit var saveStoryGutenbergBlockUseCase: SaveStoryGutenbergBlockUseCase
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var siteModel: SiteModel
    @Mock lateinit var storiesPrefs: StoriesPrefs
    @Mock lateinit var context: Context
    @Mock lateinit var postStore: PostStore

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        saveStoryGutenbergBlockUseCase = SaveStoryGutenbergBlockUseCase(storiesPrefs)
        siteModel = SiteModel()
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
        Assertions.assertThat(editPostRepository.content).isEqualTo(blockWithEmptyMediaFiles)
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
        Assertions.assertThat(editPostRepository.content).isEqualTo(blockWithNonEmptyMediaFiles)
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
        Assertions.assertThat(result).isEqualTo(blockWithNonEmptyMediaFiles)
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
        Assertions.assertThat(result).isEqualTo(blockWithEmptyMediaFiles)
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
        Assertions.assertThat(mediaFileData.link).isEqualTo("https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg")
        Assertions.assertThat(mediaFileData.type).isEqualTo("image")
        Assertions.assertThat(mediaFileData.mime).isEqualTo(mediaFile.mimeType)
        Assertions.assertThat(mediaFileData.caption).isEqualTo("")
        Assertions.assertThat(mediaFileData.url).isEqualTo(mediaFile.fileURL)
    }

    private fun setupFluxCMediaFiles(
        emptyList: Boolean
    ): ArrayList<MediaFile> {
        when (emptyList) {
            true -> return ArrayList()
            false -> {
                val mediaFiles = ArrayList<MediaFile>()
                for (i in 1..10) {
                    val oneMediaFile = MediaFile()
                    oneMediaFile.id = i
                    oneMediaFile.mediaId = (i + 1000).toString()
                    oneMediaFile.mimeType = "image/jpeg"
                    oneMediaFile.fileURL = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg"
                    mediaFiles.add(oneMediaFile)
                }
                return mediaFiles
            }
        }
    }

    private fun getOneMediaFile(id: Int): MediaFile {
        val oneMediaFile = MediaFile()
        oneMediaFile.id = id
        oneMediaFile.mediaId = (id + 1000).toString()
        oneMediaFile.mimeType = "image/jpeg"
        oneMediaFile.fileURL = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg"
        return oneMediaFile
    }

    private fun setupMediaFileDataList(
        emptyList: Boolean
    ): ArrayList<StoryMediaFileData> {
        when (emptyList) {
            true -> return ArrayList()
            false -> {
                val mediaFiles = ArrayList<StoryMediaFileData>()
                for (i in 1..10) {
                    val oneMediaFile = StoryMediaFileData(
                            id = i.toString(),
                            mime = "image/jpeg",
                            link = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg",
                            url = "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg",
                            alt = "",
                            type = "image",
                            caption = ""
                    )
                    mediaFiles.add(oneMediaFile)
                }
                return mediaFiles
            }
        }
    }

    companion object {
        private const val blockWithEmptyMediaFiles = "<!-- wp:jetpack/story {\"mediaFiles\":[]} -->\n" +
                "<div class=\"wp-story wp-block-jetpack-story\"></div>\n" +
                "<!-- /wp:jetpack/story -->"
        private const val blockWithNonEmptyMediaFiles = "<!-- wp:jetpack/story " +
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
    }
}
