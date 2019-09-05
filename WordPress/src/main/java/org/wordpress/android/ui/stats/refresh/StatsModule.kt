package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.UiModelMapper
import org.wordpress.android.ui.stats.refresh.lists.detail.PostAverageViewsPerDayUseCase.PostAverageViewsPerDayUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.detail.PostDayViewsUseCase
import org.wordpress.android.ui.stats.refresh.lists.detail.PostHeaderUseCase
import org.wordpress.android.ui.stats.refresh.lists.detail.PostMonthsAndYearsUseCase.PostMonthsAndYearsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.detail.PostRecentWeeksUseCase.PostRecentWeeksUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.AuthorsUseCase.AuthorsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ClicksUseCase.ClicksUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.CountryViewsUseCase.CountryViewsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.FileDownloadsUseCase.FileDownloadsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.OverviewUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.PostsAndPagesUseCase.PostsAndPagesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ReferrersUseCase.ReferrersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.SearchTermsUseCase.SearchTermsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.VideoPlaysUseCase.VideoPlaysUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AllTimeStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AnnualSiteStatsUseCase.AnnualSiteStatsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.CommentsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTotalsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase.FollowersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ManagementControlUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ManagementNewsCardUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.MostPopularInsightsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.PostingActivityUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.PublicizeUseCase.PublicizeUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase.TagsAndCategoriesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TodayStatsUseCase
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider.SelectedSiteStorage
import javax.inject.Named
import javax.inject.Singleton

const val INSIGHTS_USE_CASE = "InsightsUseCase"
const val DAY_STATS_USE_CASE = "DayStatsUseCase"
const val WEEK_STATS_USE_CASE = "WeekStatsUseCase"
const val MONTH_STATS_USE_CASE = "MonthStatsUseCase"
const val YEAR_STATS_USE_CASE = "YearStatsUseCase"
const val LIST_STATS_USE_CASES = "ListStatsUseCases"
const val BLOCK_INSIGHTS_USE_CASES = "BlockInsightsUseCases"
const val VIEW_ALL_INSIGHTS_USE_CASES = "ViewAllInsightsUseCases"
const val GRANULAR_USE_CASE_FACTORIES = "GranularUseCaseFactories"
const val BLOCK_DETAIL_USE_CASE = "BlockDetailUseCase"
// These are injected only internally
private const val BLOCK_DETAIL_USE_CASES = "BlockDetailUseCases"

/**
 * Module that provides use cases for Stats.
 */
@Module
class StatsModule {
    /**
     * Provides a list of use cases for the Insights screen in Stats. Modify this method when you want to add more
     * blocks to the Insights screen.
     */
    @Provides
    @Singleton
    @Named(BLOCK_INSIGHTS_USE_CASES)
    fun provideBlockInsightsUseCases(
        allTimeStatsUseCase: AllTimeStatsUseCase,
        latestPostSummaryUseCase: LatestPostSummaryUseCase,
        todayStatsUseCase: TodayStatsUseCase,
        followersUseCaseFactory: FollowersUseCaseFactory,
        commentsUseCase: CommentsUseCase,
        mostPopularInsightsUseCase: MostPopularInsightsUseCase,
        tagsAndCategoriesUseCaseFactory: TagsAndCategoriesUseCaseFactory,
        publicizeUseCaseFactory: PublicizeUseCaseFactory,
        postingActivityUseCase: PostingActivityUseCase,
        followerTotalsUseCase: FollowerTotalsUseCase,
        annualSiteStatsUseCaseFactory: AnnualSiteStatsUseCaseFactory,
        managementControlUseCase: ManagementControlUseCase,
        managementNewsCardUseCase: ManagementNewsCardUseCase
    ): List<@JvmSuppressWildcards BaseStatsUseCase<*, *>> {
        return listOf(
                allTimeStatsUseCase,
                latestPostSummaryUseCase,
                todayStatsUseCase,
                followersUseCaseFactory.build(BLOCK),
                commentsUseCase,
                mostPopularInsightsUseCase,
                tagsAndCategoriesUseCaseFactory.build(BLOCK),
                publicizeUseCaseFactory.build(BLOCK),
                postingActivityUseCase,
                followerTotalsUseCase,
                annualSiteStatsUseCaseFactory.build(BLOCK),
                managementControlUseCase,
                managementNewsCardUseCase
        )
    }

    /**
     * Provides a list of use cases for the View all screen in Stats. Modify this method when you want to add more
     * blocks to the Insights screen.
     */
    @Provides
    @Singleton
    @Named(VIEW_ALL_INSIGHTS_USE_CASES)
    fun provideViewAllInsightsUseCases(
        followersUseCaseFactory: FollowersUseCaseFactory,
        tagsAndCategoriesUseCaseFactory: TagsAndCategoriesUseCaseFactory,
        publicizeUseCaseFactory: PublicizeUseCaseFactory,
        postMonthsAndYearsUseCaseFactory: PostMonthsAndYearsUseCaseFactory,
        postAverageViewsPerDayUseCaseFactory: PostAverageViewsPerDayUseCaseFactory,
        postRecentWeeksUseCaseFactory: PostRecentWeeksUseCaseFactory,
        annualSiteStatsUseCaseFactory: AnnualSiteStatsUseCaseFactory
    ): List<@JvmSuppressWildcards BaseStatsUseCase<*, *>> {
        return listOf(
                followersUseCaseFactory.build(VIEW_ALL),
                tagsAndCategoriesUseCaseFactory.build(VIEW_ALL),
                publicizeUseCaseFactory.build(VIEW_ALL),
                postMonthsAndYearsUseCaseFactory.build(VIEW_ALL),
                postAverageViewsPerDayUseCaseFactory.build(VIEW_ALL),
                postRecentWeeksUseCaseFactory.build(VIEW_ALL),
                annualSiteStatsUseCaseFactory.build(VIEW_ALL)
        )
    }

    /**
     * Provides a list of use case factories that build use cases for the Time stats screen based on the given
     * granularity (Day, Week, Month, Year).
     */
    @Provides
    @Singleton
    @Named(GRANULAR_USE_CASE_FACTORIES)
    fun provideGranularUseCaseFactories(
        postsAndPagesUseCaseFactory: PostsAndPagesUseCaseFactory,
        referrersUseCaseFactory: ReferrersUseCaseFactory,
        clicksUseCaseFactory: ClicksUseCaseFactory,
        countryViewsUseCaseFactory: CountryViewsUseCaseFactory,
        videoPlaysUseCaseFactory: VideoPlaysUseCaseFactory,
        searchTermsUseCaseFactory: SearchTermsUseCaseFactory,
        authorsUseCaseFactory: AuthorsUseCaseFactory,
        overviewUseCaseFactory: OverviewUseCaseFactory,
        fileDownloadsUseCaseFactory: FileDownloadsUseCaseFactory
    ): List<@JvmSuppressWildcards GranularUseCaseFactory> {
        return listOf(
                postsAndPagesUseCaseFactory,
                referrersUseCaseFactory,
                clicksUseCaseFactory,
                countryViewsUseCaseFactory,
                videoPlaysUseCaseFactory,
                searchTermsUseCaseFactory,
                authorsUseCaseFactory,
                overviewUseCaseFactory,
                fileDownloadsUseCaseFactory
        )
    }

    /**
     * Provides a list of use cases for the Post detail screen in Stats. Modify this method when you want to add more
     * blocks to the post detail screen.
     */
    @Provides
    @Singleton
    @Named(BLOCK_DETAIL_USE_CASES)
    fun provideDetailUseCases(
        postHeaderUseCase: PostHeaderUseCase,
        postDayViewsUseCase: PostDayViewsUseCase,
        postMonthsAndYearsUseCaseFactory: PostMonthsAndYearsUseCaseFactory,
        postAverageViewsPerDayUseCaseFactory: PostAverageViewsPerDayUseCaseFactory,
        postRecentWeeksUseCaseFactory: PostRecentWeeksUseCaseFactory
    ): List<@JvmSuppressWildcards BaseStatsUseCase<*, *>> {
        return listOf(
                postHeaderUseCase,
                postDayViewsUseCase,
                postMonthsAndYearsUseCaseFactory.build(BLOCK),
                postAverageViewsPerDayUseCaseFactory.build(BLOCK),
                postRecentWeeksUseCaseFactory.build(BLOCK)
        )
    }

    /**
     * Provides a singleton usecase that represents the Insights screen. It consists of list of use cases that build
     * the insights blocks.
     */
    @Provides
    @Singleton
    @Named(INSIGHTS_USE_CASE)
    fun provideInsightsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(BLOCK_INSIGHTS_USE_CASES) useCases: List<@JvmSuppressWildcards BaseStatsUseCase<*, *>>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                statsSiteProvider,
                useCases,
                { statsStore.getInsightTypes(it) },
                uiModelMapper::mapInsights
        )
    }

    /**
     * Provides a singleton usecase that represents the Day stats screen.
     * @param useCasesFactories build the use cases for the DAYS granularity
     */
    @Provides
    @Singleton
    @Named(DAY_STATS_USE_CASE)
    fun provideDayStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(GRANULAR_USE_CASE_FACTORIES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                statsSiteProvider,
                useCasesFactories.map { it.build(DAYS, BLOCK) },
                { statsStore.getTimeStatsTypes(it) },
                uiModelMapper::mapTimeStats
        )
    }

    /**
     * Provides a singleton usecase that represents the Week stats screen.
     * @param useCasesFactories build the use cases for the WEEKS granularity
     */
    @Provides
    @Singleton
    @Named(WEEK_STATS_USE_CASE)
    fun provideWeekStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(GRANULAR_USE_CASE_FACTORIES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                statsSiteProvider,
                useCasesFactories.map { it.build(WEEKS, BLOCK) },
                { statsStore.getTimeStatsTypes(it) },
                uiModelMapper::mapTimeStats
        )
    }

    /**
     * Provides a singleton usecase that represents the Month stats screen.
     * @param useCasesFactories build the use cases for the MONTHS granularity
     */
    @Provides
    @Singleton
    @Named(MONTH_STATS_USE_CASE)
    fun provideMonthStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(GRANULAR_USE_CASE_FACTORIES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher, mainDispatcher,
                statsSiteProvider,
                useCasesFactories.map { it.build(MONTHS, BLOCK) },
                { statsStore.getTimeStatsTypes(it) },
                uiModelMapper::mapTimeStats
        )
    }

    /**
     * Provides a singleton usecase that represents the Year stats screen.
     * @param useCasesFactories build the use cases for the YEARS granularity
     */
    @Provides
    @Singleton
    @Named(YEAR_STATS_USE_CASE)
    fun provideYearStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(GRANULAR_USE_CASE_FACTORIES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                statsSiteProvider,
                useCasesFactories.map { it.build(YEARS, BLOCK) },
                { statsStore.getTimeStatsTypes(it) },
                uiModelMapper::mapTimeStats
        )
    }

    /**
     * Provides all list stats use cases
     */
    @Provides
    @Singleton
    @Named(LIST_STATS_USE_CASES)
    fun provideListStatsUseCases(
        @Named(INSIGHTS_USE_CASE) insightsUseCase: BaseListUseCase,
        @Named(DAY_STATS_USE_CASE) dayStatsUseCase: BaseListUseCase,
        @Named(WEEK_STATS_USE_CASE) weekStatsUseCase: BaseListUseCase,
        @Named(MONTH_STATS_USE_CASE) monthStatsUseCase: BaseListUseCase,
        @Named(YEAR_STATS_USE_CASE) yearStatsUseCase: BaseListUseCase
    ): Map<StatsSection, BaseListUseCase> {
        return mapOf(
                StatsSection.INSIGHTS to insightsUseCase,
                StatsSection.DAYS to dayStatsUseCase,
                StatsSection.WEEKS to weekStatsUseCase,
                StatsSection.MONTHS to monthStatsUseCase,
                StatsSection.YEARS to yearStatsUseCase
        )
    }

    /**
     * Provides a singleton usecase that represents the Year stats screen.
     * @param useCases build the use cases for the YEARS granularity
     */
    @Provides
    @Singleton
    @Named(BLOCK_DETAIL_USE_CASE)
    fun provideDetailStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(BLOCK_DETAIL_USE_CASES) useCases: List<@JvmSuppressWildcards BaseStatsUseCase<*, *>>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                statsSiteProvider,
                useCases,
                { statsStore.getPostDetailTypes() },
                uiModelMapper::mapDetailStats
        )
    }

    @Provides
    @Singleton
    fun provideSharedPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSelectedSiteStorage(): SelectedSiteStorage {
        return SelectedSiteStorage()
    }
}
