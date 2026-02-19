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
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsCardsConfiguration
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class StatsCardsConfigurationRepositoryTest : BaseUnitTest() {
    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var repository: StatsCardsConfigurationRepository

    @Before
    fun setUp() {
        repository = StatsCardsConfigurationRepository(
            appPrefsWrapper,
            UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `when no saved configuration, then default configuration is returned`() = test {
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(null)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).isEqualTo(StatsCardType.defaultCards())
    }

    @Test
    fun `when valid json is saved, then configuration is parsed correctly`() = test {
        val json = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(json)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).containsExactly(StatsCardType.TODAYS_STATS, StatsCardType.VIEWS_STATS)
    }

    @Test
    fun `when invalid json is saved, then default configuration is returned and json is reset`() = test {
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn("invalid json")

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).isEqualTo(StatsCardType.defaultCards())
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when saveConfiguration is called, then json is saved to prefs`() = test {
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(null)
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS)
        )

        repository.saveConfiguration(TEST_SITE_ID, config)

        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when removeCard is called, then card is removed from visible cards`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.removeCard(TEST_SITE_ID, StatsCardType.VIEWS_STATS)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        assertThat(jsonCaptor.firstValue).contains("TODAYS_STATS")
        assertThat(jsonCaptor.firstValue).contains("LOCATIONS")
        assertThat(jsonCaptor.firstValue).doesNotContain("VIEWS_STATS")
    }

    @Test
    fun `when addCard is called, then card is added to visible cards`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.addCard(TEST_SITE_ID, StatsCardType.LOCATIONS)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        assertThat(jsonCaptor.firstValue).contains("TODAYS_STATS")
        assertThat(jsonCaptor.firstValue).contains("LOCATIONS")
    }

    @Test
    fun `when configurationFlow emits, then it contains site id and configuration`() = test {
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(null)
        val config = StatsCardsConfiguration(visibleCards = listOf(StatsCardType.TODAYS_STATS))

        repository.saveConfiguration(TEST_SITE_ID, config)

        val flowValue = repository.configurationFlow.value
        assertThat(flowValue).isNotNull
        assertThat(flowValue?.first).isEqualTo(TEST_SITE_ID)
        assertThat(flowValue?.second?.visibleCards).containsExactly(StatsCardType.TODAYS_STATS)
    }

    @Test
    fun `when config contains invalid card type, then default configuration is returned and json is reset`() = test {
        // JSON with old "MOST_VIEWED" card type that no longer exists
        val jsonWithInvalidCardType = """
            {
                "visibleCards": ["TODAYS_STATS", "MOST_VIEWED", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(jsonWithInvalidCardType)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).isEqualTo(StatsCardType.defaultCards())
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), any())
    }

    // region Move card tests
    @Test
    fun `when moveCardUp is called, then card is moved up one position`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.moveCardUp(TEST_SITE_ID, StatsCardType.VIEWS_STATS)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        // VIEWS_STATS should now be at position 0
        assertThat(jsonCaptor.firstValue).contains("VIEWS_STATS")
        assertThat(jsonCaptor.firstValue.indexOf("VIEWS_STATS"))
            .isLessThan(jsonCaptor.firstValue.indexOf("TODAYS_STATS"))
    }

    @Test
    fun `when moveCardUp is called on first card, then nothing changes`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.moveCardUp(TEST_SITE_ID, StatsCardType.TODAYS_STATS)

        // setStatsCardsConfigurationJson should not be called since card is already first
        verify(appPrefsWrapper, org.mockito.kotlin.never()).setStatsCardsConfigurationJson(any(), any())
    }

    @Test
    fun `when moveCardToTop is called, then card is moved to first position`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.moveCardToTop(TEST_SITE_ID, StatsCardType.LOCATIONS)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        // LOCATIONS should now be at position 0
        assertThat(jsonCaptor.firstValue.indexOf("LOCATIONS"))
            .isLessThan(jsonCaptor.firstValue.indexOf("TODAYS_STATS"))
        assertThat(jsonCaptor.firstValue.indexOf("LOCATIONS"))
            .isLessThan(jsonCaptor.firstValue.indexOf("VIEWS_STATS"))
    }

    @Test
    fun `when moveCardToTop is called on first card, then nothing changes`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.moveCardToTop(TEST_SITE_ID, StatsCardType.TODAYS_STATS)

        verify(appPrefsWrapper, org.mockito.kotlin.never()).setStatsCardsConfigurationJson(any(), any())
    }

    @Test
    fun `when moveCardDown is called, then card is moved down one position`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.moveCardDown(TEST_SITE_ID, StatsCardType.VIEWS_STATS)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        // VIEWS_STATS should now be after LOCATIONS
        assertThat(jsonCaptor.firstValue.indexOf("VIEWS_STATS"))
            .isGreaterThan(jsonCaptor.firstValue.indexOf("LOCATIONS"))
    }

    @Test
    fun `when moveCardDown is called on last card, then nothing changes`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.moveCardDown(TEST_SITE_ID, StatsCardType.LOCATIONS)

        verify(appPrefsWrapper, org.mockito.kotlin.never()).setStatsCardsConfigurationJson(any(), any())
    }

    @Test
    fun `when moveCardToBottom is called, then card is moved to last position`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.moveCardToBottom(TEST_SITE_ID, StatsCardType.TODAYS_STATS)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        // TODAYS_STATS should now be at the end
        assertThat(jsonCaptor.firstValue.indexOf("TODAYS_STATS"))
            .isGreaterThan(jsonCaptor.firstValue.indexOf("VIEWS_STATS"))
        assertThat(jsonCaptor.firstValue.indexOf("TODAYS_STATS"))
            .isGreaterThan(jsonCaptor.firstValue.indexOf("LOCATIONS"))
    }

    @Test
    fun `when moveCardToBottom is called on last card, then nothing changes`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(initialJson)

        repository.moveCardToBottom(TEST_SITE_ID, StatsCardType.LOCATIONS)

        verify(appPrefsWrapper, org.mockito.kotlin.never()).setStatsCardsConfigurationJson(any(), any())
    }
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
    }
}
