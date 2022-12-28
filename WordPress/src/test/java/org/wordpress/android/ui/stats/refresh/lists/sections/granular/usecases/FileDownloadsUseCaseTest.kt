package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.FileDownloadsModel
import org.wordpress.android.fluxc.model.stats.time.FileDownloadsModel.FileDownloads
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.FILE_DOWNLOADS
import org.wordpress.android.fluxc.store.stats.time.FileDownloadsStore
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewFileDownloads
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val ITEMS_TO_LOAD = 6
private val statsGranularity = DAYS

@ExperimentalCoroutinesApi
class FileDownloadsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var store: FileDownloadsStore
    @Mock
    lateinit var siteModelProvider: StatsSiteProvider
    @Mock
    lateinit var site: SiteModel
    @Mock
    lateinit var selectedDateProvider: SelectedDateProvider
    @Mock
    lateinit var tracker: AnalyticsTrackerWrapper
    @Mock
    lateinit var contentDescriptionHelper: ContentDescriptionHelper
    @Mock
    lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock
    lateinit var statsUtils: StatsUtils
    private lateinit var useCase: FileDownloadsUseCase
    private lateinit var selectedDate: Date
    private val contentDescription = "file, downloads"

    @Before
    fun setUp() {
        useCase = FileDownloadsUseCase(
            statsGranularity,
            testDispatcher(),
            testDispatcher(),
            store,
            siteModelProvider,
            selectedDateProvider,
            tracker,
            contentDescriptionHelper,
            localeManagerWrapper,
            statsUtils,
            BLOCK
        )
        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.set(2019, 6, 1)
        selectedDate = selectedCalendar.time
        whenever(siteModelProvider.siteModel).thenReturn(site)
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(selectedDate)
        whenever((selectedDateProvider.getSelectedDateState(statsGranularity))).thenReturn(
            SelectedDate(
                selectedDate,
                listOf(selectedDate)
            )
        )
        whenever(
            contentDescriptionHelper.buildContentDescription(
                any(),
                any<String>(),
                any()
            )
        ).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `returns failed item when store fails`() = test {
        val forced = false
        val refresh = true
        val message = "error"
        whenever(
            store.fetchFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(ERROR)
        assertThat(result.type).isEqualTo(FILE_DOWNLOADS)
    }

    @Test
    fun `result contains only empty item when response is empty`() = test {
        val forced = false
        val refresh = true
        val emptyModel = FileDownloadsModel(listOf(), false)
        whenever(
            store.fetchFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(emptyModel))
        val calendar = Calendar.getInstance(Locale.US)
        calendar.set(2019, 8, 10)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(calendar)

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(EMPTY)
        assertThat(result.type).isEqualTo(FILE_DOWNLOADS)
        val items = result.stateData!!
        assertThat(items.size).isEqualTo(2)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).textResource).isEqualTo(R.string.stats_file_downloads)
        assertThat(items[1] is Empty).isTrue()
        assertThat((items[1] as Empty).textResource).isEqualTo(R.string.stats_no_data_for_period)
    }

    @Test
    fun `shows correct empty message when date before July 29, 2019`() = test {
        val forced = false
        val refresh = true
        val emptyModel = FileDownloadsModel(listOf(), false)
        val calendar = Calendar.getInstance(Locale.US)
        calendar.set(2019, 5, 28)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(calendar)
        val pastSelectedDate = calendar.time
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(pastSelectedDate)
        whenever((selectedDateProvider.getSelectedDateState(statsGranularity))).thenReturn(
            SelectedDate(
                pastSelectedDate,
                listOf(pastSelectedDate)
            )
        )
        whenever(
            store.fetchFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                pastSelectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(emptyModel))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(EMPTY)
        assertThat(result.type).isEqualTo(FILE_DOWNLOADS)
        val items = result.stateData!!
        assertThat(items.size).isEqualTo(2)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).textResource).isEqualTo(R.string.stats_file_downloads)
        assertThat(items[1] is Empty).isTrue()
        assertThat((items[1] as Empty).textResource).isEqualTo(R.string.stats_data_not_recorded_for_period)
    }

    @Test
    fun `converts file downloads to UI model`() = test {
        val forced = false
        val refresh = true
        val file = FileDownloads("file.txt", 10)
        val model = FileDownloadsModel(listOf(file), false)
        whenever(
            store.getFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(SUCCESS)
        assertThat(result.type).isEqualTo(FILE_DOWNLOADS)
        val items = result.data!!
        assertThat(items.size).isEqualTo(3)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).textResource).isEqualTo(R.string.stats_file_downloads)
        assertHeader(items[1])
        assertThat(items[2] is ListItemWithIcon).isTrue()
        val item = items[2] as ListItemWithIcon
        assertThat(item.text).isEqualTo(file.filename)
        assertThat(item.value).isEqualTo("10")
        assertThat(item.contentDescription).isEqualTo(contentDescription)
    }

    @Test
    fun `shows divider between items`() = test {
        val forced = false
        val refresh = true
        val page = FileDownloads("file1.txt", 10)
        val homePage = FileDownloads("file2.txt", 5)
        val model = FileDownloadsModel(listOf(page, homePage), false)
        whenever(
            store.getFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(SUCCESS)
        assertThat(result.type).isEqualTo(FILE_DOWNLOADS)
        val items = result.data!!
        assertThat(items.size).isEqualTo(4)
        assertHeader(items[1])
        assertThat(items[2] is ListItemWithIcon).isTrue()
        assertThat(items[3] is ListItemWithIcon).isTrue()
        assertThat((items[2] as ListItemWithIcon).showDivider).isEqualTo(true)
        assertThat((items[3] as ListItemWithIcon).showDivider).isEqualTo(false)
    }

    @Test
    fun `shows view more button when hasMore is true`() = test {
        val forced = false
        val refresh = true
        val page = FileDownloads("file.txt", 10)
        val hasMore = true
        val model = FileDownloadsModel(listOf(page), hasMore)
        whenever(
            store.getFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                selectedDate
            )
        ).thenReturn(model)
        whenever(
            store.fetchFileDownloads(
                site,
                statsGranularity,
                Top(ITEMS_TO_LOAD),
                selectedDate,
                forced
            )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result.state).isEqualTo(SUCCESS)
        assertThat(result.type).isEqualTo(FILE_DOWNLOADS)
        val items = result.data!!
        assertThat(items.size).isEqualTo(4)
        assertThat(items[2] is ListItemWithIcon).isTrue()
        assertThat(items[3] is Link).isTrue()

        var navigationTarget: NavigationTarget? = null
        useCase.navigationTarget.observeForever { navigationTarget = it?.getContentIfNotHandled() }

        (items[3] as Link).navigateAction.click()

        assertThat(navigationTarget).isNotNull
        val viewPost = navigationTarget as ViewFileDownloads
        assertThat(viewPost.statsGranularity).isEqualTo(statsGranularity)
    }

    private fun assertHeader(item: BlockListItem) {
        assertThat(item.type).isEqualTo(HEADER)
        assertThat((item as Header).startLabel).isEqualTo(R.string.stats_file_downloads_title_label)
        assertThat(item.endLabel).isEqualTo(R.string.stats_file_downloads_value_label)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
