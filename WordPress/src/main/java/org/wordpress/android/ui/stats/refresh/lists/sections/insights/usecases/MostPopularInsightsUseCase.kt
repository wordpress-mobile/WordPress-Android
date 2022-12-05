package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsDateUtils
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.text.PercentFormatter
import org.wordpress.android.viewmodel.ResourceProvider
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.roundToInt

class MostPopularInsightsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val mostPopularStore: MostPopularInsightsStore,
    private val postStore: PostStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsDateUtils: StatsDateUtils,
    private val resourceProvider: ResourceProvider,
    private val popupMenuHandler: ItemPopupMenuHandler,
    private val actionCardHandler: ActionCardHandler,
    private val percentFormatter: PercentFormatter
) : StatelessUseCase<InsightsMostPopularModel>(MOST_POPULAR_DAY_AND_HOUR, mainDispatcher, backgroundDispatcher) {
    override suspend fun loadCachedData(): InsightsMostPopularModel? {
        return mostPopularStore.getMostPopularInsights(statsSiteProvider.siteModel)
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<InsightsMostPopularModel> {
        val response = mostPopularStore.fetchMostPopularInsights(statsSiteProvider.siteModel, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_popular))

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override fun buildUiModel(domainModel: InsightsMostPopularModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()

        items.add(buildTitle())

        val noActivity = domainModel.highestDayPercent == 0.0 && domainModel.highestHourPercent == 0.0

        if (BuildConfig.IS_JETPACK_APP && noActivity) {
            items.add(Empty(R.string.stats_most_popular_percent_views_empty))
        } else {
            val highestDayPercent = resourceProvider.getString(
                    R.string.stats_most_popular_percent_views,
                    percentFormatter.format(
                            value = domainModel.highestDayPercent.roundToInt(),
                            rounding = RoundingMode.HALF_UP
                    )
            )
            val highestHourPercent = resourceProvider.getString(
                    R.string.stats_most_popular_percent_views,
                    percentFormatter.format(
                            value = domainModel.highestHourPercent.roundToInt(),
                            rounding = RoundingMode.HALF_UP
                    )
            )
            items.add(
                    QuickScanItem(
                            Column(
                                    R.string.stats_insights_best_day,
                                    statsDateUtils.getWeekDay(domainModel.highestDayOfWeek),
                                    if (BuildConfig.IS_JETPACK_APP) {
                                        highestDayPercent
                                    } else {
                                        null
                                    },
                                    highestDayPercent
                            ),
                            Column(
                                    R.string.stats_insights_best_hour,
                                    statsDateUtils.getHour(domainModel.highestHour),
                                    if (BuildConfig.IS_JETPACK_APP) {
                                        highestHourPercent
                                    } else {
                                        null
                                    },
                                    highestHourPercent
                            )
                    )
            )
        }

        if (BuildConfig.IS_JETPACK_APP) {
            addActionCards(domainModel)
        }
        return items
    }

    private fun addActionCards(domainModel: InsightsMostPopularModel) {
        val popular = domainModel.highestDayOfWeek > 0 || domainModel.highestHour > 0
        if (popular) actionCardHandler.display(InsightType.ACTION_REMINDER)

        val drafts = postStore.getPostsForSite(statsSiteProvider.siteModel).firstOrNull {
            PostStatus.fromPost(it) == PostStatus.DRAFT
        }
        if (drafts != null) actionCardHandler.display(InsightType.ACTION_SCHEDULE)
    }

    private fun buildTitle() = Title(
            textResource = if (BuildConfig.IS_JETPACK_APP) {
                R.string.stats_insights_popular_title
            } else {
                R.string.stats_insights_popular
            },
            menuAction = if (BuildConfig.IS_JETPACK_APP) {
                null
            } else {
                this::onMenuClick
            })

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }
}
