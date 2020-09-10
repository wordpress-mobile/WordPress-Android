package org.wordpress.android.ui.mediapicker

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
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
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaLoader.DomainModel
import org.wordpress.android.ui.mediapicker.MediaLoader.LoadAction
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import org.wordpress.android.util.LocaleManagerWrapper

class MediaLoaderTest : BaseUnitTest() {
    @Mock lateinit var mediaSource: MediaSource
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var identifier1: Identifier
    @Mock lateinit var identifier2: Identifier
    private lateinit var mediaLoader: MediaLoader
    private val mediaTypes = setOf(IMAGE, VIDEO)
    private lateinit var firstMediaItem: MediaItem
    private lateinit var secondMediaItem: MediaItem

    @Before
    fun setUp() {
        mediaLoader = MediaLoader(mediaSource, localeManagerWrapper, mediaTypes)
        firstMediaItem = MediaItem(identifier1, "url://first_item", "first item", IMAGE, "image/jpeg", 1)
        secondMediaItem = MediaItem(identifier2, "url://second_item", "second item", VIDEO, "video/mpeg", 2)
    }

    @Test
    fun `loads media items on start`() = withMediaLoader { resultModel, performAction ->
        val mediaItems = listOf(firstMediaItem)
        whenever(
                mediaSource.load(
                        mediaTypes,
                        forced = false,
                        loadMore = false
                )
        ).thenReturn(MediaLoadingResult.Success())
        whenever(mediaSource.get(mediaTypes)).thenReturn(mediaItems)

        performAction(LoadAction.Start(), true)

        resultModel.assertModel(mediaItems)
    }

    @Test
    fun `shows an error when loading fails`() = withMediaLoader { resultModel, performAction ->
        val errorMessage = "error"
        whenever(mediaSource.load(mediaTypes, forced = false, loadMore = false)).thenReturn(
                MediaLoadingResult.Failure(
                        errorMessage
                )
        )

        performAction(LoadAction.Start(), true)

        resultModel.assertModel(errorMessage = errorMessage)
    }

    @Test
    fun `loads next page`() = withMediaLoader { resultModel, performAction ->
        val firstPage = MediaLoadingResult.Success(hasMore = true)
        val secondPage = MediaLoadingResult.Success()
        whenever(mediaSource.load(mediaTypes, forced = false, loadMore = false)).thenReturn(firstPage)
        whenever(mediaSource.load(mediaTypes, forced = false, loadMore = true)).thenReturn(secondPage)
        whenever(mediaSource.get(mediaTypes)).thenReturn(
                listOf(firstMediaItem),
                listOf(firstMediaItem, secondMediaItem)
        )

        performAction(LoadAction.Start(), true)

        resultModel.assertModel(listOf(firstMediaItem), hasMore = true)

        performAction(LoadAction.NextPage, true)

        resultModel.assertModel(listOf(firstMediaItem, secondMediaItem), hasMore = false)
    }

    @Test
    fun `shows an error when loading next page fails`() = withMediaLoader { resultModel, performAction ->
        val firstPage = MediaLoadingResult.Success(hasMore = true)
        whenever(mediaSource.get(mediaTypes)).thenReturn(listOf(firstMediaItem))
        val message = "error"
        val secondPage = MediaLoadingResult.Failure(message)
        whenever(mediaSource.load(mediaTypes, forced = false, loadMore = false)).thenReturn(firstPage)
        whenever(mediaSource.load(mediaTypes, forced = false, loadMore = true)).thenReturn(secondPage)

        performAction(LoadAction.Start(), true)

        resultModel.assertModel(listOf(firstMediaItem), hasMore = true)

        performAction(LoadAction.NextPage, true)

        resultModel.assertModel(listOf(firstMediaItem), errorMessage = message, hasMore = true)
    }

    @Test
    fun `refresh overrides data`() = withMediaLoader { resultModel, performAction ->
        val firstResult = MediaLoadingResult.Success()
        whenever(mediaSource.load(eq(mediaTypes), any(), any())).thenReturn(firstResult)
        whenever(mediaSource.get(mediaTypes)).thenReturn(listOf(firstMediaItem), listOf(secondMediaItem))

        performAction(LoadAction.Start(), true)

        resultModel.assertModel(listOf(firstMediaItem))

        performAction(LoadAction.Refresh(true), true)

        resultModel.assertModel(listOf(secondMediaItem))
    }

    @Test
    fun `filters out media item`() = withMediaLoader { resultModel, performAction ->
        val mediaItems = listOf(firstMediaItem, secondMediaItem)
        whenever(
                mediaSource.load(
                        mediaTypes,
                        forced = false,
                        loadMore = false
                )
        ).thenReturn(MediaLoadingResult.Success())
        whenever(mediaSource.get(mediaTypes)).thenReturn(mediaItems)
        val filter = "second"
        whenever(mediaSource.get(mediaTypes, filter)).thenReturn(listOf(secondMediaItem))

        performAction(LoadAction.Start(), true)

        performAction(LoadAction.Filter(filter), true)

        resultModel.assertModel(listOf(secondMediaItem))

        performAction(LoadAction.ClearFilter, true)

        resultModel.assertModel(mediaItems)
    }

    @Test
    fun `clears filter`() = withMediaLoader { resultModel, performAction ->
        val mediaItems = listOf(firstMediaItem, secondMediaItem)
        whenever(
                mediaSource.load(
                        mediaTypes,
                        forced = false,
                        loadMore = false
                )
        ).thenReturn(MediaLoadingResult.Success())
        val filter = "second"
        whenever(mediaSource.get(eq(mediaTypes), eq(filter))).thenReturn(listOf(secondMediaItem))
        whenever(mediaSource.get(eq(mediaTypes), isNull())).thenReturn(mediaItems)

        performAction(LoadAction.Start(), true)
        performAction(LoadAction.Filter(filter), true)

        performAction(LoadAction.ClearFilter, true)

        resultModel.assertModel(mediaItems)
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
                action: LoadAction,
                awaitResult: Boolean
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
    }
}
