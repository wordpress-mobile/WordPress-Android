package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.google.gson.Gson
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.newstats.InsightsCardType
import org.wordpress.android.ui.newstats.InsightsCardsConfiguration
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class InsightsCardsConfigurationRepositoryTest : BaseUnitTest() {
    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var repository: InsightsCardsConfigurationRepository

    @Before
    fun setUp() {
        repository = InsightsCardsConfigurationRepository(
            appPrefsWrapper,
            UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `when no saved configuration, then default configuration is returned`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(TEST_SITE_ID)
            ).thenReturn(null)

            val config = repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards)
                .isEqualTo(InsightsCardType.defaultCards())
        }

    @Test
    fun `when valid json is saved, then configuration is parsed correctly`() =
        test {
            val json = """
                {
                    "visibleCards": ["YEAR_IN_REVIEW"]
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(TEST_SITE_ID)
            ).thenReturn(json)

            val config = repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards).contains(
                InsightsCardType.YEAR_IN_REVIEW
            )
        }

    @Test
    fun `when saved config missing new card types, then new types are added`() =
        test {
            val json = """
                {
                    "visibleCards": ["YEAR_IN_REVIEW"]
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(json)

            val config =
                repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards)
                .containsExactly(
                    InsightsCardType.YEAR_IN_REVIEW,
                    InsightsCardType.ALL_TIME_STATS,
                    InsightsCardType.MOST_POPULAR_DAY,
                    InsightsCardType.MOST_POPULAR_TIME,
                    InsightsCardType.TAGS_AND_CATEGORIES
                )
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), any()
                )
        }

    @Test
    fun `when saved config has all card types, then no update is saved`() =
        test {
            val json = """
                {
                    "visibleCards": [
                        "YEAR_IN_REVIEW",
                        "ALL_TIME_STATS",
                        "MOST_POPULAR_DAY",
                        "MOST_POPULAR_TIME",
                        "TAGS_AND_CATEGORIES"
                    ]
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(json)

            val config =
                repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards)
                .containsExactly(
                    InsightsCardType.YEAR_IN_REVIEW,
                    InsightsCardType.ALL_TIME_STATS,
                    InsightsCardType.MOST_POPULAR_DAY,
                    InsightsCardType.MOST_POPULAR_TIME,
                    InsightsCardType.TAGS_AND_CATEGORIES
                )
            verify(
                appPrefsWrapper,
                never()
            ).setStatsInsightsCardsConfigurationJson(
                any(), any()
            )
        }

    @Test
    fun `when invalid json is saved, then default configuration is returned`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(TEST_SITE_ID)
            ).thenReturn("invalid json")

            val config = repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards)
                .isEqualTo(InsightsCardType.defaultCards())
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), any()
                )
        }

    @Test
    fun `when addCard is called on empty config, then json is saved to prefs`() =
        test {
            val emptyJson = """
                {
                    "visibleCards": [],
                    "hiddenCards": [
                        "YEAR_IN_REVIEW",
                        "ALL_TIME_STATS",
                        "MOST_POPULAR_DAY",
                        "MOST_POPULAR_TIME",
                        "TAGS_AND_CATEGORIES"
                    ]
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(emptyJson)

            repository.addCard(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), any()
                )
        }

    @Test
    fun `when removeCard is called, then card is removed from visible cards`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.removeCard(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), jsonCaptor.capture()
                )
            val savedConfig = Gson()
                .fromJson(
                    jsonCaptor.firstValue,
                    InsightsCardsConfiguration::class.java
                )
            assertThat(savedConfig.visibleCards)
                .doesNotContain(
                    InsightsCardType.YEAR_IN_REVIEW
                )
            assertThat(savedConfig.hiddenCards)
                .contains(
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when card is removed, then it stays removed on next getConfiguration`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.removeCard(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), jsonCaptor.capture()
                )
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(jsonCaptor.firstValue)

            val config =
                repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards)
                .doesNotContain(
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when addCard is called, then card is added to visible cards`() =
        test {
            val initialJson = """
                {
                    "visibleCards": [],
                    "hiddenCards": [
                        "YEAR_IN_REVIEW",
                        "ALL_TIME_STATS",
                        "MOST_POPULAR_DAY",
                        "MOST_POPULAR_TIME",
                        "TAGS_AND_CATEGORIES"
                    ]
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(initialJson)

            repository.addCard(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID),
                    jsonCaptor.capture()
                )
            assertThat(jsonCaptor.firstValue)
                .contains("YEAR_IN_REVIEW")
        }

    @Test
    fun `when mutation occurs, then configurationFlow emits site id and configuration`() =
        test {
            val json = """
                {
                    "visibleCards": [],
                    "hiddenCards": [
                        "YEAR_IN_REVIEW",
                        "ALL_TIME_STATS",
                        "MOST_POPULAR_DAY",
                        "MOST_POPULAR_TIME",
                        "TAGS_AND_CATEGORIES"
                    ]
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(json)

            repository.addCard(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            val flowValue =
                repository.configurationFlow.value
            assertThat(flowValue).isNotNull
            assertThat(flowValue?.first)
                .isEqualTo(TEST_SITE_ID)
            assertThat(flowValue?.second?.visibleCards)
                .contains(
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when config contains invalid card type, then default configuration is returned`() =
        test {
            val jsonWithInvalidCardType = """
                {
                    "visibleCards": ["INVALID_CARD"]
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(TEST_SITE_ID)
            ).thenReturn(jsonWithInvalidCardType)

            val config = repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards)
                .isEqualTo(InsightsCardType.defaultCards())
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), any()
                )
        }

    @Test
    fun `when addCard is called with existing card, then card is not duplicated`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.addCard(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            verify(
                appPrefsWrapper,
                never()
            ).setStatsInsightsCardsConfigurationJson(
                any(), any()
            )
        }

    @Test
    fun `when moveCardUp on first card, then order unchanged`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.moveCardUp(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            verify(
                appPrefsWrapper,
                never()
            ).setStatsInsightsCardsConfigurationJson(
                any(), any()
            )
        }

    @Test
    fun `when moveCardDown on last card, then order unchanged`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.moveCardDown(
                TEST_SITE_ID,
                InsightsCardType.TAGS_AND_CATEGORIES
            )

            verify(
                appPrefsWrapper,
                never()
            ).setStatsInsightsCardsConfigurationJson(
                any(), any()
            )
        }

    @Test
    fun `when moveCardToTop on first card, then order unchanged`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.moveCardToTop(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            verify(
                appPrefsWrapper,
                never()
            ).setStatsInsightsCardsConfigurationJson(
                any(), any()
            )
        }

    @Test
    fun `when moveCardToBottom on last card, then order unchanged`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.moveCardToBottom(
                TEST_SITE_ID,
                InsightsCardType.TAGS_AND_CATEGORIES
            )

            verify(
                appPrefsWrapper,
                never()
            ).setStatsInsightsCardsConfigurationJson(
                any(), any()
            )
        }

    @Test
    fun `when moveCardUp on middle card, then card swaps with previous`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.moveCardUp(
                TEST_SITE_ID,
                InsightsCardType.ALL_TIME_STATS
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), jsonCaptor.capture()
                )
            val saved = Gson()
                .fromJson(
                    jsonCaptor.firstValue,
                    InsightsCardsConfiguration::class.java
                )
            assertThat(saved.visibleCards[0])
                .isEqualTo(InsightsCardType.ALL_TIME_STATS)
            assertThat(saved.visibleCards[1])
                .isEqualTo(InsightsCardType.YEAR_IN_REVIEW)
        }

    @Test
    fun `when moveCardDown on middle card, then card swaps with next`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.moveCardDown(
                TEST_SITE_ID,
                InsightsCardType.ALL_TIME_STATS
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), jsonCaptor.capture()
                )
            val saved = Gson()
                .fromJson(
                    jsonCaptor.firstValue,
                    InsightsCardsConfiguration::class.java
                )
            assertThat(saved.visibleCards[1])
                .isEqualTo(
                    InsightsCardType.MOST_POPULAR_DAY
                )
            assertThat(saved.visibleCards[2])
                .isEqualTo(InsightsCardType.ALL_TIME_STATS)
        }

    @Test
    fun `when moveCardToTop on middle card, then card becomes first`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.moveCardToTop(
                TEST_SITE_ID,
                InsightsCardType.MOST_POPULAR_DAY
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), jsonCaptor.capture()
                )
            val saved = Gson()
                .fromJson(
                    jsonCaptor.firstValue,
                    InsightsCardsConfiguration::class.java
                )
            assertThat(saved.visibleCards[0])
                .isEqualTo(
                    InsightsCardType.MOST_POPULAR_DAY
                )
        }

    @Test
    fun `when moveCardToBottom on middle card, then card becomes last`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(
                        TEST_SITE_ID
                    )
            ).thenReturn(ALL_CARDS_JSON)

            repository.moveCardToBottom(
                TEST_SITE_ID,
                InsightsCardType.MOST_POPULAR_DAY
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), jsonCaptor.capture()
                )
            val saved = Gson()
                .fromJson(
                    jsonCaptor.firstValue,
                    InsightsCardsConfiguration::class.java
                )
            assertThat(saved.visibleCards.last())
                .isEqualTo(
                    InsightsCardType.MOST_POPULAR_DAY
                )
        }

    companion object {
        private const val TEST_SITE_ID = 123L
        private val ALL_CARDS_JSON = """
            {
                "visibleCards": [
                    "YEAR_IN_REVIEW",
                    "ALL_TIME_STATS",
                    "MOST_POPULAR_DAY",
                    "MOST_POPULAR_TIME",
                    "TAGS_AND_CATEGORIES"
                ]
            }
        """.trimIndent()
    }
}
