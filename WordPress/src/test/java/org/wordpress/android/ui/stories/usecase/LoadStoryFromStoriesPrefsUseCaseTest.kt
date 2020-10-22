package org.wordpress.android.ui.stories.usecase

import android.content.Context
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.Companion.TEMPORARY_ID_PREFIX
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.ui.stories.prefs.StoriesPrefs.TempId

@RunWith(MockitoJUnitRunner::class)
class LoadStoryFromStoriesPrefsUseCaseTest {
    private lateinit var loadStoryFromStoriesPrefsUseCase: LoadStoryFromStoriesPrefsUseCase
    @Mock lateinit var storyRepositoryWrapper: StoryRepositoryWrapper
    @Mock lateinit var mediaStore: MediaStore
    @Mock lateinit var storiesPrefs: StoriesPrefs
    @Mock lateinit var context: Context
    @Mock lateinit var siteModel: SiteModel

    @Before
    fun setUp() {
        loadStoryFromStoriesPrefsUseCase = LoadStoryFromStoriesPrefsUseCase(
                storyRepositoryWrapper,
                storiesPrefs,
                mediaStore
        )
    }

    @Test
    fun `obtain empty media ids list from empty mediaFiles array`() {
        // Given
        val mediaFiles: ArrayList<HashMap<String, Any>> = setupMediaFiles(emptyList = true)

        // When
        val mediaIds = loadStoryFromStoriesPrefsUseCase.getMediaIdsFromStoryBlockBridgeMediaFiles(
                mediaFiles as ArrayList<Any>
        )

        // Then
        Assertions.assertThat(mediaIds).isEmpty()
    }

    @Test
    fun `obtain media ids list from non empty mediaFiles array`() {
        // Given
        val mediaFiles: ArrayList<HashMap<String, Any>> = setupMediaFiles(emptyList = false)

        // When
        val mediaIds = loadStoryFromStoriesPrefsUseCase.getMediaIdsFromStoryBlockBridgeMediaFiles(
                mediaFiles as ArrayList<Any>
        )

        // Then
        Assertions.assertThat(mediaIds).isNotEmpty
        Assertions.assertThat(mediaIds).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    }

    @Test
    fun `verify all story slides are editable with temporary ids`() {
        // Given
        val mediaIdsTemp = setupTestSildes(sayValid = true, useTempPrefix = true, useRemoteId = false)

        // When
        val result = loadStoryFromStoriesPrefsUseCase.areAllStorySlidesEditable(siteModel, mediaIdsTemp)

        // Then
        Assertions.assertThat(result).isTrue()
    }

    @Test
    fun `verify all story slides are editable with local ids`() {
        // Given
        val mediaIdsLocal = setupTestSildes(sayValid = true, useTempPrefix = false, useRemoteId = false)

        // When
        val result = loadStoryFromStoriesPrefsUseCase.areAllStorySlidesEditable(siteModel, mediaIdsLocal)

        // Then
        Assertions.assertThat(result).isTrue()
    }

    @Test
    fun `verify all story slides are editable with remote ids`() {
        // Given
        val mediaIdsLocal = setupTestSildes(sayValid = true, useTempPrefix = false, useRemoteId = true)

        // When
        val result = loadStoryFromStoriesPrefsUseCase.areAllStorySlidesEditable(siteModel, mediaIdsLocal)

        // Then
        Assertions.assertThat(result).isTrue()
    }

    @Test
    fun `verify not all story slides are editable with temporary ids`() {
        // Given
        val mediaIdsLocal = setupTestSildes(sayValid = false, useTempPrefix = true, useRemoteId = false)

        // When
        val result = loadStoryFromStoriesPrefsUseCase.areAllStorySlidesEditable(siteModel, mediaIdsLocal)

        // Then
        Assertions.assertThat(result).isFalse()
    }

    @Test
    fun `verify not all story slides are editable with remote ids`() {
        // Given
        val mediaIdsLocal = setupTestSildes(sayValid = false, useTempPrefix = false, useRemoteId = true)

        // When
        val result = loadStoryFromStoriesPrefsUseCase.areAllStorySlidesEditable(siteModel, mediaIdsLocal)

        // Then
        Assertions.assertThat(result).isFalse()
    }

    @Test
    fun `verify not all story slides are editable with local ids`() {
        // Given
        val mediaIdsLocal = setupTestSildes(sayValid = false, useTempPrefix = false, useRemoteId = false)

        // When
        val result = loadStoryFromStoriesPrefsUseCase.areAllStorySlidesEditable(siteModel, mediaIdsLocal)

        // Then
        Assertions.assertThat(result).isFalse()
    }

    private fun setupMediaFiles(
        emptyList: Boolean
    ): ArrayList<HashMap<String, Any>> {
        when (emptyList) {
            true -> return ArrayList<HashMap<String, Any>>()
            false -> {
                val mediaFiles = ArrayList<HashMap<String, Any>>()
                for (i in 1..10) {
                    val oneMediaFile = HashMap<String, Any>()
                    oneMediaFile.put("mime", "image/jpeg")
                    oneMediaFile.put("link", "https://testsite.files.wordpress.com/2020/10/wp-0000000.jpg")
                    oneMediaFile.put("type", "image")
                    oneMediaFile.put("id", i.toString())
                    mediaFiles.add(oneMediaFile)
                }
                return mediaFiles
            }
        }
    }

    private fun setupTestSildes(
        sayValid: Boolean,
        useTempPrefix: Boolean,
        useRemoteId: Boolean
    ): ArrayList<String> {
        val mediaIds = ArrayList<String>()

        for (i in 1..10) {
            val mediaId = (if (useTempPrefix) TEMPORARY_ID_PREFIX else "") + i.toString()
            mediaIds.add(mediaId)
            if (useTempPrefix) {
                whenever(storiesPrefs.isValidSlide(siteModel.id.toLong(), TempId(mediaId))).thenReturn(sayValid)
            } else if (useRemoteId) {
                whenever(
                        storiesPrefs.isValidSlide(
                                siteModel.id.toLong(),
                                RemoteId(mediaId.toLong())
                        )
                ).thenReturn(sayValid)
            } else {
                whenever(storiesPrefs.isValidSlide(
                            siteModel.id.toLong(),
                            LocalId(mediaId.toInt())
                        )
                ).thenReturn(sayValid)
            }
        }

        return mediaIds
    }
}
