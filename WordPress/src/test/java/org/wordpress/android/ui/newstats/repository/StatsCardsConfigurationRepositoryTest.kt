package org.wordpress.android.ui.newstats.repository

import com.google.gson.Gson
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "MOST_VIEWED", "LOCATIONS"],
                "defaultCardsVersion": 1
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(jsonWithInvalidCardType)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).isEqualTo(StatsCardType.defaultCards())
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), any())
    }

    // region Default cards migration tests
    @Test
    fun `when configuration predates the referrers default, then referrers is shown under posts and pages`() = test {
        val legacyJson = """
            {
                "visibleCards": ["TODAYS_STATS", "MOST_VIEWED_POSTS_AND_PAGES", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(legacyJson)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).containsExactly(
            StatsCardType.TODAYS_STATS,
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            StatsCardType.MOST_VIEWED_REFERRERS,
            StatsCardType.LOCATIONS
        )
    }

    @Test
    fun `when configuration predates the referrers default, then the migrated configuration is persisted`() = test {
        val legacyJson = """
            {
                "visibleCards": ["TODAYS_STATS", "MOST_VIEWED_POSTS_AND_PAGES"],
                "selectedPeriodType": "week",
                "selectedMetric": "visitors"
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(legacyJson)

        val returned = repository.getConfiguration(TEST_SITE_ID)

        val jsonCaptor = argumentCaptor<String>()
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), jsonCaptor.capture())
        // What is written has to be the configuration that was returned. Asserting only that the
        // JSON mentions referrers would also pass if a fresh default configuration were persisted,
        // which would silently discard the card order, period and metric the user had saved.
        val persisted = Gson().fromJson(jsonCaptor.firstValue, StatsCardsConfiguration::class.java)
        assertThat(persisted).isEqualTo(returned)
        assertThat(persisted.visibleCards).containsExactly(
            StatsCardType.TODAYS_STATS,
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            StatsCardType.MOST_VIEWED_REFERRERS
        )
        assertThat(persisted.selectedPeriodType).isEqualTo("week")
        assertThat(persisted.selectedMetric).isEqualTo("visitors")
        assertThat(persisted.defaultCardsVersion)
            .isEqualTo(StatsCardsConfiguration.CURRENT_DEFAULT_CARDS_VERSION)
    }

    @Test
    fun `when posts and pages is not visible, then the referrers default is added last`() = test {
        val legacyJson = """
            {
                "visibleCards": ["TODAYS_STATS", "LOCATIONS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(legacyJson)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).containsExactly(
            StatsCardType.TODAYS_STATS,
            StatsCardType.LOCATIONS,
            StatsCardType.MOST_VIEWED_REFERRERS
        )
    }

    @Test
    fun `when a configuration predating the default already shows referrers, then it is not duplicated`() = test {
        val legacyJson = """
            {
                "visibleCards": ["MOST_VIEWED_REFERRERS", "TODAYS_STATS"]
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(legacyJson)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).containsExactly(
            StatsCardType.MOST_VIEWED_REFERRERS,
            StatsCardType.TODAYS_STATS
        )
        // The version still has to be stamped and written, even though no card was added. Without
        // it the migration runs again on every load and would undo a later removal of the card.
        assertThat(config.defaultCardsVersion)
            .isEqualTo(StatsCardsConfiguration.CURRENT_DEFAULT_CARDS_VERSION)
        verify(appPrefsWrapper).setStatsCardsConfigurationJson(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when referrers was removed after the migration ran, then it is not added back`() = test {
        val migratedJson = """
            {
                "visibleCards": ["TODAYS_STATS", "MOST_VIEWED_POSTS_AND_PAGES"],
                "defaultCardsVersion": 1
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(migratedJson)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).containsExactly(
            StatsCardType.TODAYS_STATS,
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES
        )
        verify(appPrefsWrapper, org.mockito.kotlin.never()).setStatsCardsConfigurationJson(any(), any())
    }

    @Test
    fun `when no saved configuration, then nothing is persisted`() = test {
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(null)

        repository.getConfiguration(TEST_SITE_ID)

        verify(appPrefsWrapper, org.mockito.kotlin.never()).setStatsCardsConfigurationJson(any(), any())
    }

    @Test
    fun `when the period selection predates the referrers default, then migrating keeps it`() = test {
        val legacyJson = """
            {
                "visibleCards": ["MOST_VIEWED_POSTS_AND_PAGES"],
                "selectedPeriodType": "week",
                "selectedMetric": "visitors"
            }
        """.trimIndent()
        whenever(appPrefsWrapper.getStatsCardsConfigurationJson(TEST_SITE_ID)).thenReturn(legacyJson)

        val config = repository.getConfiguration(TEST_SITE_ID)

        assertThat(config.visibleCards).containsExactly(
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            StatsCardType.MOST_VIEWED_REFERRERS
        )
        assertThat(config.selectedPeriodType).isEqualTo("week")
        assertThat(config.selectedMetric).isEqualTo("visitors")
    }
    // endregion

    // region Move card tests
    @Test
    fun `when moveCardUp is called, then card is moved up one position`() = test {
        val initialJson = """
            {
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
                "visibleCards": ["TODAYS_STATS", "VIEWS_STATS", "LOCATIONS"],
                "defaultCardsVersion": 1
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
