package org.wordpress.android.ui.stats.refresh

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.StatsViewType.ANNUAL_STATS
import org.wordpress.android.ui.stats.StatsViewType.AUTHORS
import org.wordpress.android.ui.stats.StatsViewType.CLICKS
import org.wordpress.android.ui.stats.StatsViewType.DETAIL_AVERAGE_VIEWS_PER_DAY
import org.wordpress.android.ui.stats.StatsViewType.DETAIL_MONTHS_AND_YEARS
import org.wordpress.android.ui.stats.StatsViewType.DETAIL_RECENT_WEEKS
import org.wordpress.android.ui.stats.StatsViewType.FILE_DOWNLOADS
import org.wordpress.android.ui.stats.StatsViewType.FOLLOWERS
import org.wordpress.android.ui.stats.StatsViewType.GEOVIEWS
import org.wordpress.android.ui.stats.StatsViewType.INSIGHTS_ALL_TIME
import org.wordpress.android.ui.stats.StatsViewType.INSIGHTS_LATEST_POST_SUMMARY
import org.wordpress.android.ui.stats.StatsViewType.INSIGHTS_MOST_POPULAR
import org.wordpress.android.ui.stats.StatsViewType.INSIGHTS_TODAY
import org.wordpress.android.ui.stats.StatsViewType.INSIGHTS_VIEWS_AND_VISITORS
import org.wordpress.android.ui.stats.StatsViewType.PUBLICIZE
import org.wordpress.android.ui.stats.StatsViewType.REFERRERS
import org.wordpress.android.ui.stats.StatsViewType.SEARCH_TERMS
import org.wordpress.android.ui.stats.StatsViewType.TAGS_AND_CATEGORIES
import org.wordpress.android.ui.stats.StatsViewType.TOP_POSTS_AND_PAGES
import org.wordpress.android.ui.stats.StatsViewType.VIDEO_PLAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.detail.PostAverageViewsPerDayUseCase
import org.wordpress.android.ui.stats.refresh.lists.detail.PostMonthsAndYearsUseCase
import org.wordpress.android.ui.stats.refresh.lists.detail.PostRecentWeeksUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.AuthorsUseCase.AuthorsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ClicksUseCase.ClicksUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.CountryViewsUseCase.CountryViewsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.FileDownloadsUseCase.FileDownloadsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.PostsAndPagesUseCase.PostsAndPagesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ReferrersUseCase.ReferrersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.SearchTermsUseCase.SearchTermsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.VideoPlaysUseCase.VideoPlaysUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AllTimeStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AnnualSiteStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.MostPopularInsightsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.PublicizeUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TodayStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsUseCase
import org.wordpress.android.ui.stats.refresh.utils.StatsDateSelector
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toStatsSection
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Named

class StatsViewAllViewModelFactory(
    private val mainDispatcher: CoroutineDispatcher,
    private val bgDispatcher: CoroutineDispatcher,
    private val useCase: BaseStatsUseCase<*, *>,
    private val statsSiteProvider: StatsSiteProvider,
    private val dateSelector: StatsDateSelector,
    @StringRes private val titleResource: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewAllViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewAllViewModel(
                    mainDispatcher,
                    bgDispatcher,
                    useCase,
                    statsSiteProvider,
                    dateSelector,
                    titleResource
            ) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }

    class Builder @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        @Named(GRANULAR_USE_CASE_FACTORIES)
        private val granularFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>,
        @Named(VIEW_ALL_INSIGHTS_USE_CASES)
        private val insightsUseCases: List<@JvmSuppressWildcards BaseStatsUseCase<*, *>>,
        private val statsSiteProvider: StatsSiteProvider,
        private val dateSelectorFactory: StatsDateSelector.Factory
    ) {
        fun build(type: StatsViewType, granularity: StatsGranularity?): StatsViewAllViewModelFactory {
            return when {
                type == ANNUAL_STATS -> buildAnnualStatsFactory()
                granularity == null -> buildFactory(type)
                else -> buildFactory(type, granularity)
            }
        }

        private fun buildFactory(type: StatsViewType, granularity: StatsGranularity): StatsViewAllViewModelFactory {
            val (useCase, title) = getGranularUseCase(type, granularity, granularFactories)
            return StatsViewAllViewModelFactory(
                    mainDispatcher,
                    bgDispatcher,
                    useCase,
                    statsSiteProvider,
                    dateSelectorFactory.build(granularity.toStatsSection()),
                    title
            )
        }

        private fun buildFactory(type: StatsViewType): StatsViewAllViewModelFactory {
            val (useCase, title) = getInsightsUseCase(type, insightsUseCases)
            return StatsViewAllViewModelFactory(
                    mainDispatcher,
                    bgDispatcher,
                    useCase,
                    statsSiteProvider,
                    dateSelectorFactory.build(INSIGHTS),
                    title
            )
        }

        private fun buildAnnualStatsFactory(): StatsViewAllViewModelFactory {
            val useCase = insightsUseCases.find {
                it is AnnualSiteStatsUseCase
            } as AnnualSiteStatsUseCase
            return StatsViewAllViewModelFactory(
                    mainDispatcher,
                    bgDispatcher,
                    useCase,
                    statsSiteProvider,
                    dateSelectorFactory.build(StatsSection.ANNUAL_STATS),
                    R.string.stats_insights_annual_site_stats
            )
        }

        private fun getGranularUseCase(
            type: StatsViewType,
            granularity: StatsGranularity,
            granularFactories: List<GranularUseCaseFactory>
        ): Pair<BaseStatsUseCase<*, *>, Int> {
            return when (type) {
                TOP_POSTS_AND_PAGES -> Pair(granularFactories.first { it is PostsAndPagesUseCaseFactory }
                        .build(granularity, VIEW_ALL), R.string.stats_view_top_posts_and_pages)
                REFERRERS -> Pair(granularFactories.first { it is ReferrersUseCaseFactory }
                        .build(granularity, VIEW_ALL), R.string.stats_view_referrers)
                CLICKS -> Pair(granularFactories.first { it is ClicksUseCaseFactory }
                        .build(granularity, VIEW_ALL), R.string.stats_view_clicks)
                AUTHORS -> Pair(granularFactories.first { it is AuthorsUseCaseFactory }
                        .build(granularity, VIEW_ALL), R.string.stats_view_authors)
                GEOVIEWS -> Pair(granularFactories.first { it is CountryViewsUseCaseFactory }
                        .build(granularity, VIEW_ALL), R.string.stats_view_countries)
                SEARCH_TERMS -> Pair(granularFactories.first { it is SearchTermsUseCaseFactory }
                        .build(granularity, VIEW_ALL), R.string.stats_view_search_terms)
                VIDEO_PLAYS -> Pair(granularFactories.first { it is VideoPlaysUseCaseFactory }
                        .build(granularity, VIEW_ALL), R.string.stats_view_videos)
                FILE_DOWNLOADS -> Pair(granularFactories.first { it is FileDownloadsUseCaseFactory }
                        .build(granularity, VIEW_ALL), R.string.stats_file_downloads)
                else -> throw InvalidParameterException("Invalid granular stats type: ${type.name}")
            }
        }

        private fun getInsightsUseCase(
            type: StatsViewType,
            insightsUseCases: List<BaseStatsUseCase<*, *>>
        ): Pair<BaseStatsUseCase<*, *>, Int> {
            return when (type) {
                INSIGHTS_VIEWS_AND_VISITORS -> Pair(
                        insightsUseCases.first { it is ViewsAndVisitorsUseCase },
                        R.string.stats_insights_views_and_visitors
                )
                FOLLOWERS -> Pair(
                        insightsUseCases.first { it is FollowersUseCase },
                        R.string.stats_view_followers
                )
                TAGS_AND_CATEGORIES -> Pair(
                        insightsUseCases.first { it is TagsAndCategoriesUseCase },
                        R.string.stats_view_tags_and_categories
                )
                INSIGHTS_ALL_TIME -> Pair(
                        insightsUseCases.first { it is AllTimeStatsUseCase },
                        R.string.stats_insights_all_time_stats
                )
                INSIGHTS_LATEST_POST_SUMMARY -> Pair(
                        insightsUseCases
                                .first { it is LatestPostSummaryUseCase }, R.string.stats_insights_latest_post_summary
                )
                INSIGHTS_MOST_POPULAR -> Pair(
                        insightsUseCases.first { it is MostPopularInsightsUseCase },
                        R.string.stats_insights_popular
                )
                INSIGHTS_TODAY -> Pair(
                        insightsUseCases.first { it is TodayStatsUseCase },
                        R.string.stats_insights_today
                )
                PUBLICIZE -> Pair(
                        insightsUseCases.first { it is PublicizeUseCase },
                        R.string.stats_view_publicize
                )
                DETAIL_MONTHS_AND_YEARS ->
                    insightsUseCases.first {
                        it is PostMonthsAndYearsUseCase
                    } to R.string.stats_detail_months_and_years
                DETAIL_AVERAGE_VIEWS_PER_DAY ->
                    insightsUseCases.first {
                        it is PostAverageViewsPerDayUseCase
                    } to R.string.stats_detail_average_views_per_day
                DETAIL_RECENT_WEEKS ->
                    insightsUseCases.first {
                        it is PostRecentWeeksUseCase
                    } to R.string.stats_detail_recent_weeks
                else -> throw InvalidParameterException("Invalid insights stats type: ${type.name}")
            }
        }
    }
}
