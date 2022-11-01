package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VideoPlaysModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.VIDEOS
import org.wordpress.android.fluxc.store.stats.time.VideoPlaysStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewVideoPlays
import org.wordpress.android.ui.stats.refresh.lists.BLOCK_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.VIEW_ALL_ITEM_COUNT
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
class VideoPlaysUseCase constructor(
    statsGranularity: StatsGranularity,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val store: VideoPlaysStore,
    statsSiteProvider: StatsSiteProvider,
    selectedDateProvider: SelectedDateProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils,
    private val useCaseMode: UseCaseMode
) : GranularStatelessUseCase<VideoPlaysModel>(
        VIDEOS,
        mainDispatcher,
        backgroundDispatcher,
        selectedDateProvider,
        statsSiteProvider,
        statsGranularity
) {
    private val itemsToLoad = if (useCaseMode == VIEW_ALL) VIEW_ALL_ITEM_COUNT else BLOCK_ITEM_COUNT

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_videos))

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): VideoPlaysModel? {
        return store.getVideoPlays(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate
        )
    }

    override suspend fun fetchRemoteData(selectedDate: Date, site: SiteModel, forced: Boolean): State<VideoPlaysModel> {
        val response = store.fetchVideoPlays(
                site,
                statsGranularity,
                LimitMode.Top(itemsToLoad),
                selectedDate,
                forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.plays.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: VideoPlaysModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        if (useCaseMode == BLOCK) {
            items.add(Title(R.string.stats_videos))
        }

        if (domainModel.plays.isEmpty()) {
            items.add(Empty(R.string.stats_no_data_for_period))
        } else {
            val header = Header(R.string.stats_videos_title_label, R.string.stats_videos_views_label)
            items.add(header)
            items.addAll(domainModel.plays.mapIndexed { index, videoPlays ->
                ListItemWithIcon(
                        text = videoPlays.title,
                        value = statsUtils.toFormattedString(videoPlays.plays),
                        showDivider = index < domainModel.plays.size - 1,
                        navigationAction = videoPlays.url?.let { ListItemInteraction.create(it, this::onItemClick) },
                        contentDescription = contentDescriptionHelper.buildContentDescription(
                                header,
                                videoPlays.title,
                                videoPlays.plays
                        )
                )
            })

            if (useCaseMode == BLOCK && domainModel.hasMore) {
                items.add(
                        Link(
                                text = R.string.stats_insights_view_more,
                                navigateAction = ListItemInteraction.create(statsGranularity, this::onViewMoreClick)
                        )
                )
            }
        }
        return items
    }

    private fun onViewMoreClick(statsGranularity: StatsGranularity) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_VIDEO_PLAYS_VIEW_MORE_TAPPED, statsGranularity)
        navigateTo(
                ViewVideoPlays(
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity) ?: Date()
                )
        )
    }

    private fun onItemClick(url: String) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_VIDEO_PLAYS_VIDEO_TAPPED, statsGranularity)
        navigateTo(ViewUrl(url))
    }

    class VideoPlaysUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val store: VideoPlaysStore,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsSiteProvider: StatsSiteProvider,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val statsUtils: StatsUtils,
        private val contentDescriptionHelper: ContentDescriptionHelper
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                VideoPlaysUseCase(
                        granularity,
                        mainDispatcher,
                        backgroundDispatcher,
                        store,
                        statsSiteProvider,
                        selectedDateProvider,
                        analyticsTracker,
                        contentDescriptionHelper,
                        statsUtils,
                        useCaseMode
                )
    }
}
