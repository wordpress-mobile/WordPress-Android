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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.newstats.subscribers.SubscribersCardType
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class SubscribersCardsConfigurationRepositoryTest : BaseUnitTest() {
    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var repository: SubscribersCardsConfigurationRepository

    @Before
    fun setUp() {
        repository = SubscribersCardsConfigurationRepository(
            appPrefsWrapper,
            UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `when no saved configuration, then default configuration is returned`() = test {
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(null)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).isEqualTo(SubscribersCardType.defaultCards())
    }

    @Test
    fun `when valid json is saved, then configuration is parsed correctly`() = test {
        val json = """
            {
                "visibleCards": ["ALL_TIME_SUBSCRIBERS", "EMAILS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(json)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).containsExactly(
            SubscribersCardType.ALL_TIME_SUBSCRIBERS,
            SubscribersCardType.EMAILS
        )
    }

    @Test
    fun `when invalid json is saved, then default config is returned and json is reset`() =
        test {
            whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
                .thenReturn("invalid json")

            val config = repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards).isEqualTo(SubscribersCardType.defaultCards())
            verify(appPrefsWrapper)
                .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), any())
        }

    @Test
    fun `when addCard is called, then json is saved to prefs`() = test {
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn("""{"visibleCards":["ALL_TIME_SUBSCRIBERS"]}""")

        repository.addCard(TEST_SITE_ID, SubscribersCardType.EMAILS)

        verify(appPrefsWrapper)
            .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when removeCard is called, then card is removed from visible cards`() = test {
        val initialJson = """
            {
                "visibleCards": [
                    "ALL_TIME_SUBSCRIBERS",
                    "SUBSCRIBERS_GRAPH",
                    "EMAILS"
                ]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(initialJson)

        repository.removeCard(TEST_SITE_ID, SubscribersCardType.SUBSCRIBERS_GRAPH)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper)
            .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        assertThat(jsonCaptor.firstValue).contains("ALL_TIME_SUBSCRIBERS")
        assertThat(jsonCaptor.firstValue).contains("EMAILS")
        assertThat(jsonCaptor.firstValue).doesNotContain("SUBSCRIBERS_GRAPH")
    }

    @Test
    fun `when addCard is called, then card is added to visible cards`() = test {
        val initialJson = """
            {
                "visibleCards": ["ALL_TIME_SUBSCRIBERS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(initialJson)

        repository.addCard(TEST_SITE_ID, SubscribersCardType.EMAILS)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper)
            .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        assertThat(jsonCaptor.firstValue).contains("ALL_TIME_SUBSCRIBERS")
        assertThat(jsonCaptor.firstValue).contains("EMAILS")
    }

    @Test
    fun `when moveCardUp is called, then card is moved up one position`() = test {
        val initialJson = """
            {
                "visibleCards": [
                    "ALL_TIME_SUBSCRIBERS",
                    "SUBSCRIBERS_GRAPH",
                    "EMAILS"
                ]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(initialJson)

        repository.moveCardUp(TEST_SITE_ID, SubscribersCardType.SUBSCRIBERS_GRAPH)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper)
            .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        assertThat(jsonCaptor.firstValue.indexOf("SUBSCRIBERS_GRAPH"))
            .isLessThan(jsonCaptor.firstValue.indexOf("ALL_TIME_SUBSCRIBERS"))
    }

    @Test
    fun `when moveCardUp is called on first card, then nothing changes`() = test {
        val initialJson = """
            {
                "visibleCards": [
                    "ALL_TIME_SUBSCRIBERS",
                    "SUBSCRIBERS_GRAPH",
                    "EMAILS"
                ]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(initialJson)

        repository.moveCardUp(TEST_SITE_ID, SubscribersCardType.ALL_TIME_SUBSCRIBERS)

        verify(appPrefsWrapper, never())
            .setSubscribersCardsConfigurationJson(any(), any())
    }

    @Test
    fun `when moveCardDown is called, then card is moved down one position`() = test {
        val initialJson = """
            {
                "visibleCards": [
                    "ALL_TIME_SUBSCRIBERS",
                    "SUBSCRIBERS_GRAPH",
                    "EMAILS"
                ]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(initialJson)

        repository.moveCardDown(TEST_SITE_ID, SubscribersCardType.SUBSCRIBERS_GRAPH)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper)
            .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        assertThat(jsonCaptor.firstValue.indexOf("SUBSCRIBERS_GRAPH"))
            .isGreaterThan(jsonCaptor.firstValue.indexOf("EMAILS"))
    }

    @Test
    fun `when moveCardDown is called on last card, then nothing changes`() = test {
        val initialJson = """
            {
                "visibleCards": [
                    "ALL_TIME_SUBSCRIBERS",
                    "SUBSCRIBERS_GRAPH",
                    "EMAILS"
                ]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(initialJson)

        repository.moveCardDown(TEST_SITE_ID, SubscribersCardType.EMAILS)

        verify(appPrefsWrapper, never())
            .setSubscribersCardsConfigurationJson(any(), any())
    }

    @Test
    fun `when moveCardToTop is called, then card is moved to first position`() = test {
        val initialJson = """
            {
                "visibleCards": [
                    "ALL_TIME_SUBSCRIBERS",
                    "SUBSCRIBERS_GRAPH",
                    "EMAILS"
                ]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(initialJson)

        repository.moveCardToTop(TEST_SITE_ID, SubscribersCardType.EMAILS)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper)
            .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        assertThat(jsonCaptor.firstValue.indexOf("EMAILS"))
            .isLessThan(jsonCaptor.firstValue.indexOf("ALL_TIME_SUBSCRIBERS"))
        assertThat(jsonCaptor.firstValue.indexOf("EMAILS"))
            .isLessThan(jsonCaptor.firstValue.indexOf("SUBSCRIBERS_GRAPH"))
    }

    @Test
    fun `when moveCardToBottom is called, then card is moved to last position`() = test {
        val initialJson = """
            {
                "visibleCards": [
                    "ALL_TIME_SUBSCRIBERS",
                    "SUBSCRIBERS_GRAPH",
                    "EMAILS"
                ]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn(initialJson)

        repository.moveCardToBottom(
            TEST_SITE_ID,
            SubscribersCardType.ALL_TIME_SUBSCRIBERS
        )

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper)
            .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        assertThat(jsonCaptor.firstValue.indexOf("ALL_TIME_SUBSCRIBERS"))
            .isGreaterThan(jsonCaptor.firstValue.indexOf("SUBSCRIBERS_GRAPH"))
        assertThat(jsonCaptor.firstValue.indexOf("ALL_TIME_SUBSCRIBERS"))
            .isGreaterThan(jsonCaptor.firstValue.indexOf("EMAILS"))
    }

    @Test
    fun `when config contains invalid card type, then default config is returned and json is reset`() =
        test {
            val jsonWithInvalidType = """
                {
                    "visibleCards": [
                        "ALL_TIME_SUBSCRIBERS",
                        "INVALID_TYPE",
                        "EMAILS"
                    ]
                }
            """.trimIndent()
            whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
                .thenReturn(jsonWithInvalidType)

            val config = repository.getConfiguration(TEST_SITE_ID)

            assertThat(config.visibleCards).isEqualTo(SubscribersCardType.defaultCards())
            verify(appPrefsWrapper)
                .setSubscribersCardsConfigurationJson(eq(TEST_SITE_ID), any())
        }

    @Test
    fun `when configurationFlow emits, then it contains site id and configuration`() = test {
        whenever(appPrefsWrapper.getSubscribersCardsConfigurationJson(TEST_SITE_ID))
            .thenReturn("""{"visibleCards":[]}""")

        repository.addCard(
            TEST_SITE_ID,
            SubscribersCardType.ALL_TIME_SUBSCRIBERS
        )

        val flowValue = repository.configurationFlow.value
        assertThat(flowValue).isNotNull
        assertThat(flowValue?.first).isEqualTo(TEST_SITE_ID)
        assertThat(flowValue?.second?.visibleCards)
            .containsExactly(
                SubscribersCardType.ALL_TIME_SUBSCRIBERS
            )
    }

    companion object {
        private const val TEST_SITE_ID = 123L
    }
}
