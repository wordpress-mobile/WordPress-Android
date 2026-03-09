package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
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

            assertThat(config.visibleCards).containsExactly(
                InsightsCardType.YEAR_IN_REVIEW
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
    fun `when saveConfiguration is called, then json is saved to prefs`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(TEST_SITE_ID)
            ).thenReturn(null)
            val config = InsightsCardsConfiguration(
                visibleCards = listOf(InsightsCardType.YEAR_IN_REVIEW)
            )

            repository.saveConfiguration(TEST_SITE_ID, config)

            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), any()
                )
        }

    @Test
    fun `when removeCard is called, then card is removed from visible cards`() =
        test {
            val initialJson = """
                {
                    "visibleCards": ["YEAR_IN_REVIEW"]
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(TEST_SITE_ID)
            ).thenReturn(initialJson)

            repository.removeCard(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), jsonCaptor.capture()
                )
            assertThat(jsonCaptor.firstValue)
                .doesNotContain("YEAR_IN_REVIEW")
        }

    @Test
    fun `when addCard is called, then card is added to visible cards`() =
        test {
            val initialJson = """
                {
                    "visibleCards": []
                }
            """.trimIndent()
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(TEST_SITE_ID)
            ).thenReturn(initialJson)

            repository.addCard(
                TEST_SITE_ID,
                InsightsCardType.YEAR_IN_REVIEW
            )

            val jsonCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper)
                .setStatsInsightsCardsConfigurationJson(
                    eq(TEST_SITE_ID), jsonCaptor.capture()
                )
            assertThat(jsonCaptor.firstValue)
                .contains("YEAR_IN_REVIEW")
        }

    @Test
    fun `when configurationFlow emits, then it contains site id and configuration`() =
        test {
            whenever(
                appPrefsWrapper
                    .getStatsInsightsCardsConfigurationJson(TEST_SITE_ID)
            ).thenReturn(null)
            val config = InsightsCardsConfiguration(
                visibleCards = listOf(InsightsCardType.YEAR_IN_REVIEW)
            )

            repository.saveConfiguration(TEST_SITE_ID, config)

            val flowValue = repository.configurationFlow.value
            assertThat(flowValue).isNotNull
            assertThat(flowValue?.first).isEqualTo(TEST_SITE_ID)
            assertThat(flowValue?.second?.visibleCards)
                .containsExactly(InsightsCardType.YEAR_IN_REVIEW)
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

    companion object {
        private const val TEST_SITE_ID = 123L
    }
}
