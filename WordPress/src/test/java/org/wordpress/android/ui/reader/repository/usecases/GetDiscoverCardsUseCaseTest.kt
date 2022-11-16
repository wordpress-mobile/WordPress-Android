package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.ReaderDiscoverCardsTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderRecommendedBlogsCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.WelcomeBannerCard
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderConstants

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetDiscoverCardsUseCaseTest {
    private lateinit var useCase: GetDiscoverCardsUseCase
    private val readerDiscoverCardsTableWrapper: ReaderDiscoverCardsTableWrapper = mock()
    private val parseDiscoverCardsJsonUseCase: ParseDiscoverCardsJsonUseCase = mock()
    private val mockedJsonArray: JSONArray = mock()
    private val mockedPostCardJson: JSONObject = mock()
    private val mockedInterestsCardJson: JSONObject = mock()
    private val mockedRecommendedBlogsCardJson: JSONObject = mock()
    private val readerPostTableWrapper: ReaderPostTableWrapper = mock()
    private val readerBlogTableWrapper: ReaderBlogTableWrapper = mock()
    private val appLogWrapper: AppLogWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    @Before
    fun setUp() {
        useCase = GetDiscoverCardsUseCase(
                parseDiscoverCardsJsonUseCase,
                readerDiscoverCardsTableWrapper,
                readerPostTableWrapper,
                readerBlogTableWrapper,
                appLogWrapper,
                appPrefsWrapper,
                TEST_DISPATCHER
        )
        whenever(parseDiscoverCardsJsonUseCase.convertListOfJsonArraysIntoSingleJsonArray(anyOrNull()))
                .thenReturn(mockedJsonArray)
        whenever(mockedJsonArray.length()).thenReturn(3)
        whenever(mockedJsonArray.getJSONObject(0)).thenReturn(mockedPostCardJson)
        whenever(mockedJsonArray.getJSONObject(1)).thenReturn(mockedInterestsCardJson)
        whenever(mockedJsonArray.getJSONObject(2)).thenReturn(mockedRecommendedBlogsCardJson)
        whenever(readerDiscoverCardsTableWrapper.loadDiscoverCardsJsons()).thenReturn(listOf(""))
        whenever(parseDiscoverCardsJsonUseCase.parseInterestCard(anyOrNull())).thenReturn(mock())
        whenever(parseDiscoverCardsJsonUseCase.parseSimplifiedRecommendedBlogsCard(anyOrNull()))
                .thenReturn(listOf(Pair(1L, 0L), Pair(2L, 0L)))
        whenever(parseDiscoverCardsJsonUseCase.parseSimplifiedPostCard(anyOrNull())).thenReturn(Pair(101, 102))
        whenever(readerPostTableWrapper.getBlogPost(anyLong(), anyLong(), anyBoolean())).thenReturn(mock())
        whenever(mockedPostCardJson.getString(ReaderConstants.JSON_CARD_TYPE))
                .thenReturn(ReaderConstants.JSON_CARD_POST)
        whenever(mockedInterestsCardJson.getString(ReaderConstants.JSON_CARD_TYPE))
                .thenReturn(ReaderConstants.JSON_CARD_INTERESTS_YOU_MAY_LIKE)
        whenever(mockedRecommendedBlogsCardJson.getString(ReaderConstants.JSON_CARD_TYPE))
                .thenReturn(ReaderConstants.JSON_CARD_RECOMMENDED_BLOGS)
        whenever(appPrefsWrapper.readerDiscoverWelcomeBannerShown)
                .thenReturn(true)
    }

    @Test
    fun `welcome card is added as first card if it has not been shown yet`() = test {
        // Arrange
        whenever(appPrefsWrapper.readerDiscoverWelcomeBannerShown)
                .thenReturn(false)
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards[0]).isInstanceOf(WelcomeBannerCard::class.java)
    }

    @Test
    fun `welcome card is not added to list of cards if was already shown once`() = test {
        // Arrange
        whenever(appPrefsWrapper.readerDiscoverWelcomeBannerShown)
                .thenReturn(true)
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards.filterIsInstance<WelcomeBannerCard>()).size().isEqualTo(0)
    }

    @Test
    fun `welcome card is not added to the list of cards when there are no other cards`() = test {
        // Arrange
        whenever(mockedJsonArray.length()).thenReturn(0)
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards.filterIsInstance<WelcomeBannerCard>()).size().isEqualTo(0)
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
    fun `recommended blogs card json is transformed into ReaderRecommendedBlogsCard object`() = test {
        // Arrange
        whenever(mockedPostCardJson.getString(ReaderConstants.JSON_CARD_TYPE))
                .thenReturn(ReaderConstants.JSON_CARD_RECOMMENDED_BLOGS)
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards[0]).isInstanceOf(ReaderRecommendedBlogsCard::class.java)
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
        assertThat(result.cards.size).isEqualTo(2)
    }

    @Test
    fun `all items from the json are transformed into cards`() = test {
        // Arrange
        whenever(readerPostTableWrapper.getBlogPost(anyLong(), anyLong(), anyBoolean())).thenReturn(mock())
        // Act
        val result = useCase.get()
        // Assert
        assertThat(result.cards.size).isEqualTo(3)
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

    @Test
    fun `recommended blog is retrieved from local db and added to the card`() = test {
        // Arrange
        val localReaderBlog = createReaderBlog()
        whenever(readerBlogTableWrapper.getReaderBlog(1L, 0L)).thenReturn(localReaderBlog)
        // Act
        val result = useCase.get()

        // Assert
        assertThat((result.cards[2] as ReaderRecommendedBlogsCard).blogs.first()).isEqualTo(localReaderBlog)
    }

    @Test
    fun `if recommended blog retrieved from local db is null it's not added to the card`() = test {
        // Arrange
        val localReaderBlog = createReaderBlog()
        whenever(readerBlogTableWrapper.getReaderBlog(1L, 0L)).thenReturn(localReaderBlog)
        whenever(readerBlogTableWrapper.getReaderBlog(2L, 0L)).thenReturn(null)
        // Act
        val result = useCase.get()

        // Assert
        assertThat((result.cards[2] as ReaderRecommendedBlogsCard).blogs.size).isEqualTo(1)
    }

    private fun createReaderBlog() = ReaderBlog().apply {
        blogId = 1L
        description = "description"
        url = "url"
        name = "name"
        imageUrl = null
        feedId = 0L
    }
}
