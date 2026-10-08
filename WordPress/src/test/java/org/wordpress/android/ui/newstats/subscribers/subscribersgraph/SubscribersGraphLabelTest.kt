package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersGraphDataPoint
import org.wordpress.android.ui.newstats.repository.SubscribersGraphResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

/**
 * How a `/stats/subscribers` period string becomes the text on the chart — the axis label, the
 * marker label, and the order the points end up in. Split out of
 * [SubscribersGraphViewModelTest], which covers loading, errors and tab selection.
 */
@ExperimentalCoroutinesApi
class SubscribersGraphLabelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    private lateinit var viewModel:
        SubscribersGraphViewModel

    private lateinit var defaultLocale: Locale

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        // The month abbreviations these tests assert ("Jan", "Feb", ...) come from
        // Locale.getDefault() inside formatLabel. Pin the locale for determinism, and restore
        // it in tearDown so this global mutation doesn't leak into other tests in the runner.
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(testSite)
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    private fun initViewModel() {
        viewModel = SubscribersGraphViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.loadData()
    }

    @Test
    fun `given weeks tab, when data loads, then labels show the week start date`() =
        test {
            stubDataPoints(
                "2026W01W26" to TEST_COUNT_1,
                "2026W02W02" to TEST_COUNT_2
            )

            loadWithTab(SubscribersGraphTab.WEEKS)

            assertThat(loadedLabels())
                .containsExactly("Jan 26", "Feb 2")
        }

    @Test
    fun `given weeks tab, when data loads, then no raw period string is shown`() =
        test {
            // Locale-independent contract lock: the raw WordPress period shape (not an
            // ISO week, which would be '2026-W05') must never reach the label, and its
            // 'W' separators are what would give it away.
            stubDataPoints(
                "2026W01W26" to TEST_COUNT_1,
                "2026W02W02" to TEST_COUNT_2
            )

            loadWithTab(SubscribersGraphTab.WEEKS)

            assertThat(loadedLabels())
                .isNotEmpty
                .noneMatch { it.contains("W") }
        }

    @Test
    fun `given days tab, when data loads, then labels show the day`() =
        test {
            stubDataPoints(
                "2026-02-25" to TEST_COUNT_1,
                "2026-02-26" to TEST_COUNT_2
            )

            initViewModel()
            advanceUntilIdle()

            assertThat(loadedLabels())
                .containsExactly("Feb 25", "Feb 26")
        }

    @Test
    fun `given months tab, when data loads, then labels show the month`() =
        test {
            stubDataPoints(
                "2026-02-01" to TEST_COUNT_1,
                "2026-03-01" to TEST_COUNT_2
            )

            loadWithTab(SubscribersGraphTab.MONTHS)

            assertThat(loadedLabels())
                .containsExactly("Feb", "Mar")
        }

    @Test
    fun `given a day-less month period, when data loads, then the label shows the month`() =
        test {
            stubDataPoints("2026-02" to TEST_COUNT_1)

            loadWithTab(SubscribersGraphTab.MONTHS)

            assertThat(loadedLabels()).containsExactly("Feb")
        }

    @Test
    fun `given years tab, when data loads, then labels show the year`() =
        test {
            stubDataPoints(
                "2025" to TEST_COUNT_1,
                "2026" to TEST_COUNT_2
            )

            loadWithTab(SubscribersGraphTab.YEARS)

            assertThat(loadedLabels())
                .containsExactly("2025", "2026")
        }

    @Test
    fun `given an unrecognised period, when data loads, then it is shown verbatim`() =
        test {
            stubDataPoints("not-a-period" to TEST_COUNT_1)

            initViewModel()
            advanceUntilIdle()

            assertThat(loadedLabels())
                .containsExactly("not-a-period")
        }

    @Test
    fun `given weeks tab, when a point is selected, then the marker names the whole week`() =
        test {
            stubDataPoints("2026W07W27" to TEST_COUNT_1)

            loadWithTab(SubscribersGraphTab.WEEKS)

            // 27 Jul 2026 is a Monday; the span must read as seven days, not one.
            assertThat(loadedMarkerLabels())
                .containsExactly("27 Jul - 2 Aug")
        }

    @Test
    fun `given a week inside one month, when selected, then the marker omits the first month`() =
        test {
            stubDataPoints("2026W07W20" to TEST_COUNT_1)

            loadWithTab(SubscribersGraphTab.WEEKS)

            assertThat(loadedMarkerLabels())
                .containsExactly("20-26 Jul")
        }

    @Test
    fun `given days tab, when a point is selected, then the marker matches the axis label`() =
        test {
            stubDataPoints("2026-02-25" to TEST_COUNT_1)

            initViewModel()
            advanceUntilIdle()

            assertThat(loadedMarkerLabels())
                .isEqualTo(loadedLabels())
        }

    @Test
    fun `given an out-of-range weekly period, when data loads, then it is shown verbatim`() =
        test {
            // Pins the breadth of the catch: the regex matches, so this fails inside
            // LocalDate.of rather than LocalDate.parse. Narrowing the caught type to
            // DateTimeParseException would crash the card here while every other test stays
            // green.
            stubDataPoints("2026W13W27" to TEST_COUNT_1)

            loadWithTab(SubscribersGraphTab.WEEKS)

            assertThat(loadedLabels())
                .containsExactly("2026W13W27")
        }

    @Test
    fun `given unordered weekly periods, when data loads, then points are in date order`() =
        test {
            stubDataPoints(
                "2026W08W03" to TEST_COUNT_3,
                "2026W07W20" to TEST_COUNT_1,
                "2026W07W27" to TEST_COUNT_2
            )

            loadWithTab(SubscribersGraphTab.WEEKS)

            assertThat(loadedLabels())
                .containsExactly("Jul 20", "Jul 27", "Aug 3")
        }

    @Test
    fun `given an unparseable period, when data loads, then it sorts last`() =
        test {
            stubDataPoints(
                "not-a-period" to TEST_COUNT_3,
                "2026-02-25" to TEST_COUNT_1
            )

            initViewModel()
            advanceUntilIdle()

            assertThat(loadedLabels())
                .containsExactly("Feb 25", "not-a-period")
        }

    private fun loadWithTab(tab: SubscribersGraphTab) {
        initViewModel()
        advanceUntilIdle()
        viewModel.onTabSelected(tab)
        advanceUntilIdle()
    }

    private suspend fun stubDataPoints(
        vararg points: Pair<String, Long>
    ) {
        whenever(
            statsRepository.fetchSubscribersGraph(
                any(), any(), any(), any()
            )
        ).thenReturn(
            SubscribersGraphResult.Success(
                dataPoints = points.map { (date, count) ->
                    SubscribersGraphDataPoint(date, count)
                }
            )
        )
    }

    private fun loadedLabels(): List<String> =
        loadedPoints().map { it.label }

    private fun loadedMarkerLabels(): List<String> =
        loadedPoints().map { it.markerLabel }

    private fun loadedPoints(): List<GraphDataPoint> =
        (
            viewModel.uiState.value
                as SubscribersGraphUiState.Loaded
            ).dataPoints

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
        private const val TEST_COUNT_1 = 100L
        private const val TEST_COUNT_2 = 150L
        private const val TEST_COUNT_3 = 200L
    }
}
