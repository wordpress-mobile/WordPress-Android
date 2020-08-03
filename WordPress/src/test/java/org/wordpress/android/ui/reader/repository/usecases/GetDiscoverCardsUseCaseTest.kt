package org.wordpress.android.ui.reader.repository.usecases

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.datasets.ReaderDiscoverCardsTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.test
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.repository.ReaderTagRepository

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetDiscoverCardsUseCaseTest {
    private lateinit var useCase: GetDiscoverCardsUseCase
    private val readerDiscoverCardsTableWrapper: ReaderDiscoverCardsTableWrapper = mock()
    private val parseDiscoverCardsJsonUseCase: ParseDiscoverCardsJsonUseCase = mock()
    private val mockedJsonArray: JSONArray = mock()
    private val mockedPostCardJson: JSONObject = mock()
    private val mockedInterestsCardJson: JSONObject = mock()
    private val readerPostTableWrapper: ReaderPostTableWrapper = mock()
    private val appLogWrapper: AppLogWrapper = mock()
    private val readerTagRepository: ReaderTagRepository = mock()

    @Before
    fun setUp() {
        useCase = GetDiscoverCardsUseCase(
                parseDiscoverCardsJsonUseCase,
                readerDiscoverCardsTableWrapper,
                readerPostTableWrapper,
                appLogWrapper,
                readerTagRepository,
                TEST_DISPATCHER
        )
        whenever(parseDiscoverCardsJsonUseCase.convertListOfJsonArraysIntoSingleJsonArray(anyOrNull()))
                .thenReturn(mockedJsonArray)
        whenever(mockedJsonArray.length()).thenReturn(2)
        whenever(mockedJsonArray.getJSONObject(0)).thenReturn(mockedPostCardJson)
        whenever(mockedJsonArray.getJSONObject(1)).thenReturn(mockedInterestsCardJson)
        whenever(readerDiscoverCardsTableWrapper.loadDiscoverCardsJsons()).thenReturn(listOf(""))
        whenever(parseDiscoverCardsJsonUseCase.parseInterestCard(anyOrNull())).thenReturn(mock())
        whenever(parseDiscoverCardsJsonUseCase.parseSimplifiedPostCard(anyOrNull())).thenReturn(Pair(101, 102))
        whenever(readerPostTableWrapper.getBlogPost(anyLong(), anyLong(), anyBoolean())).thenReturn(mock())
        whenever(mockedPostCardJson.getString(ReaderConstants.JSON_CARD_TYPE))
                .thenReturn(ReaderConstants.JSON_CARD_POST)
        whenever(mockedInterestsCardJson.getString(ReaderConstants.JSON_CARD_TYPE))
                .thenReturn(ReaderConstants.JSON_CARD_INTERESTS_YOU_MAY_LIKE)

        // TODO jd_alexander remove the associated mocks and tested behavior of the readerTagRepository
        runBlocking {
            whenever(readerTagRepository.getUserTags(anyBoolean())).thenReturn(null)
        }
    }

    @Test
    fun `interest you might like card json is transformed into InterestsYouMayLikeCard object`() = test {
        // Arrange
        whenever(mockedPostCardJson.getString(ReaderConstants.JSON_CARD_TYPE))
                .thenReturn(ReaderConstants.JSON_CARD_INTERESTS_YOU_MAY_LIKE)
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards[0]).isInstanceOf(InterestsYouMayLikeCard::class.java)
    }

    @Test
    fun `post card json is transformed into ReaderPostCard object`() = test {
        // Arrange
        whenever(mockedPostCardJson.getString(ReaderConstants.JSON_CARD_TYPE))
                .thenReturn(ReaderConstants.JSON_CARD_POST)
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards[0]).isInstanceOf(ReaderPostCard::class.java)
    }

    @Test
    fun `if post not found in local db the remaining items are still transformed`() = test {
        // Arrange
        whenever(readerPostTableWrapper.getBlogPost(anyLong(), anyLong(), anyBoolean())).thenReturn(null)
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards.size).isEqualTo(1)
    }

    @Test
    fun `all items from the json are transformed into cards`() = test {
        // Arrange
        whenever(readerPostTableWrapper.getBlogPost(anyLong(), anyLong(), anyBoolean())).thenReturn(mock())
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards.size).isEqualTo(2)
    }

    @Test
    fun `when cards json is empty an empty ReaderDiscoverCards is returned`() = test {
        // Arrange
        val emptyList = listOf<String>()
        whenever(readerDiscoverCardsTableWrapper.loadDiscoverCardsJsons()).thenReturn(emptyList)
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards).isEmpty()
    }
}
