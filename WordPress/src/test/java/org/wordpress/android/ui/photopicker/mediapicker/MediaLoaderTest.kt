package org.wordpress.android.ui.photopicker.mediapicker

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.test
import org.wordpress.android.ui.photopicker.mediapicker.MediaLoader.DomainModel
import org.wordpress.android.ui.photopicker.mediapicker.MediaLoader.LoadAction
import org.wordpress.android.ui.photopicker.mediapicker.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.photopicker.mediapicker.MediaType.VIDEO
import org.wordpress.android.util.UriWrapper

class MediaLoaderTest : BaseUnitTest() {
    @Mock lateinit var mediaSource: MediaSource
    @Mock lateinit var uri1: UriWrapper
    @Mock lateinit var uri2: UriWrapper
    private lateinit var mediaLoader: MediaLoader
    private val mediaTypes = setOf(MediaType.IMAGE, VIDEO)
    private lateinit var firstMediaItem: MediaItem
    private lateinit var secondMediaItem: MediaItem

    @Before
    fun setUp() {
        mediaLoader = MediaLoader(mediaSource)
        firstMediaItem = MediaItem(1, uri1, "first item", false)
        secondMediaItem = MediaItem(2, uri2, "second item", true)
    }

    @Test
    fun `loads media items on start`() = withMediaLoader { resultModel, performAction ->
        val mediaItems = listOf(firstMediaItem)
        whenever(mediaSource.load(mediaTypes, 0, null)).thenReturn(MediaLoadingResult.Success(mediaItems))

        performAction(LoadAction.Start(mediaTypes), true)

        resultModel.assertModel(mediaItems)
    }

    @Test
    fun `shows an error when loading fails`() = withMediaLoader { resultModel, performAction ->
        val errorMessage = "error"
        whenever(mediaSource.load(mediaTypes, 0, null)).thenReturn(MediaLoadingResult.Failure(errorMessage))

        performAction(LoadAction.Start(mediaTypes), true)

        resultModel.assertModel(errorMessage = errorMessage)
    }

    @Test
    fun `does not load media without start`() = withMediaLoader { resultModel, performAction ->
        performAction(LoadAction.Refresh, false)

        assertThat(resultModel).isEmpty()
    }

    @Test
    fun `loads next page`() = withMediaLoader { resultModel, performAction ->
        val firstPage = MediaLoadingResult.Success(listOf(firstMediaItem), hasMore = true)
        val secondPage = MediaLoadingResult.Success(listOf(secondMediaItem))
        whenever(mediaSource.load(mediaTypes, 0, null)).thenReturn(firstPage)
        whenever(mediaSource.load(mediaTypes, 1, null)).thenReturn(secondPage)

        performAction(LoadAction.Start(mediaTypes), true)

        resultModel.assertModel(listOf(firstMediaItem), hasMore = true)

        performAction(LoadAction.NextPage, true)

        resultModel.assertModel(listOf(firstMediaItem, secondMediaItem), hasMore = false)
    }

    @Test
    fun `shows an error when loading next page fails`() = withMediaLoader { resultModel, performAction ->
        val firstPage = MediaLoadingResult.Success(listOf(firstMediaItem), hasMore = true)
        val message = "error"
        val secondPage = MediaLoadingResult.Failure(message)
        whenever(mediaSource.load(mediaTypes, 0, null)).thenReturn(firstPage)
        whenever(mediaSource.load(mediaTypes, 1, null)).thenReturn(secondPage)

        performAction(LoadAction.Start(mediaTypes), true)

        resultModel.assertModel(listOf(firstMediaItem), hasMore = true)

        performAction(LoadAction.NextPage, true)

        resultModel.assertModel(listOf(firstMediaItem), errorMessage = message, hasMore = true)
    }

    @Test
    fun `refresh overrides data`() = withMediaLoader { resultModel, performAction ->
        val firstResult = MediaLoadingResult.Success(listOf(firstMediaItem))
        val refreshedResult = MediaLoadingResult.Success(listOf(secondMediaItem))
        whenever(mediaSource.load(mediaTypes, 0, null)).thenReturn(firstResult, refreshedResult)

        performAction(LoadAction.Start(mediaTypes), true)

        resultModel.assertModel(listOf(firstMediaItem))

        performAction(LoadAction.Refresh, true)

        resultModel.assertModel(listOf(secondMediaItem))
    }

    private fun List<DomainModel>.assertModel(
        mediaItems: List<MediaItem> = listOf(),
        errorMessage: String? = null,
        hasMore: Boolean = false
    ) {
        this.last().apply {
            assertThat(this.domainItems).isEqualTo(mediaItems)
            assertThat(this.error).isEqualTo(errorMessage)
            assertThat(this.hasMore).isEqualTo(hasMore)
        }
    }

    private fun withMediaLoader(
        assertFunction: suspend (
            domainModels: List<DomainModel>,
            performAction: suspend (
                action: LoadAction, awaitResult: Boolean
            ) -> Unit
        ) -> Unit
    ) =
            test {
                val loadActions: Channel<LoadAction> = Channel()
                val domainModels: MutableList<DomainModel> = mutableListOf()
                val job = launch {
                    mediaLoader.loadMedia(loadActions).collect {
                        domainModels.add(it)
                    }
                }
                assertFunction(domainModels) { action, awaitResult ->
                    val currentCount = domainModels.size
                    loadActions.send(action)
                    if (awaitResult) {
                        domainModels.awaitResult(currentCount + 1)
                    }
                }
                job.cancel()
            }

    private suspend fun List<DomainModel>.awaitResult(count: Int) {
        val limit = 10
        var counter = 0
        while (counter < limit && this.size < count) {
            counter++
            delay(1)
        }
        assertThat(this).hasSize(count)
    }
}
