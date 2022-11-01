package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType
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
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK_DETAIL
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
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.TotalLikesDetailUseCase.TotalLikesGranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.VideoPlaysUseCase.VideoPlaysUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ViewsAndVisitorsDetailUseCase.ViewsAndVisitorsGranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ActionCardGrowUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ActionCardReminderUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ActionCardScheduleUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AllTimeStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AnnualSiteStatsUseCase.AnnualSiteStatsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AuthorsCommentsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.CommentsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTotalsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTypesUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase.FollowersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ManagementControlUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ManagementNewsCardUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.MostPopularInsightsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.PostingActivityUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.PostsCommentsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.PublicizeUseCase.PublicizeUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase.TagsAndCategoriesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TodayStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalCommentsUseCase.TotalCommentsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalFollowersUseCase.TotalFollowersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TotalLikesUseCase.TotalLikesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsUseCase.ViewsAndVisitorsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import javax.inject.Named
import javax.inject.Singleton

const val INSIGHTS_USE_CASE = "InsightsUseCase"
const val DAY_STATS_USE_CASE = "DayStatsUseCase"
const val WEEK_STATS_USE_CASE = "WeekStatsUseCase"
const val MONTH_STATS_USE_CASE = "MonthStatsUseCase"
const val YEAR_STATS_USE_CASE = "YearStatsUseCase"
const val BLOCK_DETAIL_USE_CASE = "BlockDetailUseCase"
const val VIEWS_AND_VISITORS_USE_CASE = "ViewsAndVisitorsUseCase"
const val TOTAL_LIKES_DETAIL_USE_CASE = "LikesDetailUseCase"
const val TOTAL_COMMENTS_DETAIL_USE_CASE = "CommentsDetailUseCase"
const val TOTAL_FOLLOWERS_DETAIL_USE_CASE = "FollowersDetailUseCase"

const val LIST_STATS_USE_CASES = "ListStatsUseCases"
const val BLOCK_INSIGHTS_USE_CASES = "BlockInsightsUseCases"
const val VIEW_ALL_INSIGHTS_USE_CASES = "ViewAllInsightsUseCases"
const val GRANULAR_USE_CASE_FACTORIES = "GranularUseCaseFactories"

// These are injected only internally
private const val BLOCK_DETAIL_USE_CASES = "BlockDetailUseCases"
private const val BLOCK_VIEWS_AND_VISITORS_USE_CASES = "BlockViewsAndVisitorsUseCases"
private const val TOTAL_LIKES_DETAIL_USE_CASES = "LikesDetailUseCases"
private const val TOTAL_COMMENTS_DETAIL_USE_CASES = "CommentsDetailUseCases"
private const val TOTAL_FOLLOWERS_DETAIL_USE_CASES = "FollowersDetailUseCases"

/**
 * Module that provides use cases for Stats.
 */
@InstallIn(SingletonComponent::class)
@Module
class StatsModule {
    /**
     * Provides a list of use cases for the Insights screen in Stats. Modify this method when you want to add more
     * blocks to the Insights screen.
     */
    @Provides
    @Singleton
    @Named(BLOCK_INSIGHTS_USE_CASES)
    @Suppress("LongParameterList")
    fun provideBlockInsightsUseCases(
        viewsAndVisitorsUseCaseFactory: ViewsAndVisitorsUseCaseFactory,
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
        totalLikesUseCaseFactory: TotalLikesUseCaseFactory,
        totalCommentsUseCaseFactory: TotalCommentsUseCaseFactory,
        totalFollowersUseCaseFactory: TotalFollowersUseCaseFactory,
        annualSiteStatsUseCaseFactory: AnnualSiteStatsUseCaseFactory,
        managementControlUseCase: ManagementControlUseCase,
        managementNewsCardUseCase: ManagementNewsCardUseCase,
        actionCardGrowUseCase: ActionCardGrowUseCase,
        actionCardReminderUseCase: ActionCardReminderUseCase,
        actionCardScheduleUseCase: ActionCardScheduleUseCase
    ): List<@JvmSuppressWildcards BaseStatsUseCase<*, *>> {
        val useCases = mutableListOf<BaseStatsUseCase<*, *>>()
        if (BuildConfig.IS_JETPACK_APP) {
            useCases.add(viewsAndVisitorsUseCaseFactory.build(BLOCK))
            useCases.add(totalLikesUseCaseFactory.build(BLOCK))
            useCases.add(totalCommentsUseCaseFactory.build(BLOCK))
            useCases.add(totalFollowersUseCaseFactory.build(BLOCK))
            useCases.add(actionCardGrowUseCase)
            useCases.add(actionCardReminderUseCase)
            useCases.add(actionCardScheduleUseCase)
        } else {
            useCases.add(followerTotalsUseCase)
        }
        useCases.addAll(
                listOf(
                    allTimeStatsUseCase,
                    latestPostSummaryUseCase,
                    todayStatsUseCase,
                    followersUseCaseFactory.build(BLOCK),
                    commentsUseCase,
                    mostPopularInsightsUseCase,
                    tagsAndCategoriesUseCaseFactory.build(BLOCK),
                    publicizeUseCaseFactory.build(BLOCK),
                    postingActivityUseCase,
                    annualSiteStatsUseCaseFactory.build(BLOCK),
                    managementControlUseCase,
                    managementNewsCardUseCase
                )
        )
        return useCases
    }

    /**
     * Provides a list of use cases for the View all screen in Stats. Modify this method when you want to add more
     * blocks to the Insights screen.
     */
    @Provides
    @Singleton
    @Named(VIEW_ALL_INSIGHTS_USE_CASES)
    @Suppress("LongParameterList")
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
    @Suppress("LongParameterList")
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
    @Suppress("LongParameterList")
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
    @Suppress("LongParameterList")
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
    @Suppress("LongParameterList")
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
    @Suppress("LongParameterList")
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
    @Suppress("LongParameterList")
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
    @Suppress("LongParameterList")
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

    /**
     * Provides a singleton usecase that represents Views And Visitors Details screen.
     * Update this method when you want to add more blocks to this detail screen.
     * @param useCases build the use cases for the YEARS granularity
     */
    @Provides
    @Singleton
    @Named(BLOCK_VIEWS_AND_VISITORS_USE_CASES)
    fun provideViewsAndVisitorsDetailUseCases(
        viewsAndVisitorsGranularUseCaseFactory: ViewsAndVisitorsGranularUseCaseFactory,
        referrersUseCaseFactory: ReferrersUseCaseFactory,
        countryViewsUseCaseFactory: CountryViewsUseCaseFactory
    ): List<@JvmSuppressWildcards GranularUseCaseFactory> {
        return listOf(
                viewsAndVisitorsGranularUseCaseFactory,
                referrersUseCaseFactory,
                countryViewsUseCaseFactory
        )
    }

    /**
     * Provides a singleton usecase that represents the Year stats screen.
     * @param useCases build the use cases for the YEARS granularity
     */
    @Provides
    @Singleton
    @Named(VIEWS_AND_VISITORS_USE_CASE)
    fun provideViewsAndVisitorsDetailUseCase(
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(BLOCK_VIEWS_AND_VISITORS_USE_CASES)
        useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                statsSiteProvider,
                useCasesFactories.map { it.build(WEEKS, BLOCK_DETAIL) },
                {
                    listOf(InsightType.VIEWS_AND_VISITORS, TimeStatsType.REFERRERS, TimeStatsType.COUNTRIES)
                },
                uiModelMapper::mapTimeStats
        )
    }

    /**
     * Provides a list of use cases for the Total Likes detail screen in Stats. Modify this method when you want to
     * add more blocks to the likes detail screen.
     */
    @Provides
    @Singleton
    @Named(TOTAL_LIKES_DETAIL_USE_CASES)
    fun provideLikesDetailUseCases(
        totalLikesUseCaseFactory: TotalLikesGranularUseCaseFactory,
        postsAndPagesUseCaseFactory: PostsAndPagesUseCaseFactory
    ): List<@JvmSuppressWildcards GranularUseCaseFactory> {
        return listOf(
                totalLikesUseCaseFactory,
                postsAndPagesUseCaseFactory
        )
    }

    /**
     * Provides a singleton usecase that represents the Likes detail screen.
     * @param useCases build the use cases for the DAYS granularity
     */
    @Provides
    @Singleton
    @Named(TOTAL_LIKES_DETAIL_USE_CASE)
    fun provideLikesDetailStatsUseCase(
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(TOTAL_LIKES_DETAIL_USE_CASES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                statsSiteProvider,
                useCasesFactories.map { it.build(WEEKS, BLOCK_DETAIL) },
                { listOf(InsightType.TOTAL_LIKES, TimeStatsType.POSTS_AND_PAGES) },
                uiModelMapper::mapTimeStats
        )
    }

    /**
     * Provides a list of use cases for the Total Comments detail screen in Stats. Modify this method when you want to
     * add more blocks to the comments detail screen.
     */
    @Provides
    @Singleton
    @Named(TOTAL_COMMENTS_DETAIL_USE_CASES)
    fun provideCommentsDetailUseCases(
        totalCommentsUseCaseFactory: TotalCommentsUseCaseFactory,
        authorsCommentsUseCase: AuthorsCommentsUseCase,
        postsCommentsUseCase: PostsCommentsUseCase
    ): List<@JvmSuppressWildcards BaseStatsUseCase<*, *>> {
        return listOf(
                totalCommentsUseCaseFactory.build(BLOCK_DETAIL),
                authorsCommentsUseCase,
                postsCommentsUseCase
        )
    }

    /**
     * Provides a singleton usecase that represents the Comments detail screen.
     * @param useCases build the use cases for the DAYS granularity
     */
    @Provides
    @Singleton
    @Named(TOTAL_COMMENTS_DETAIL_USE_CASE)
    fun provideCommentsDetailStatsUseCase(
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(TOTAL_COMMENTS_DETAIL_USE_CASES) useCases: List<@JvmSuppressWildcards BaseStatsUseCase<*, *>>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                statsSiteProvider,
                useCases,
                { listOf(InsightType.TOTAL_COMMENTS, InsightType.AUTHORS_COMMENTS, InsightType.POSTS_COMMENTS) },
                uiModelMapper::mapInsights
        )
    }

    /**
     * Provides a list of use cases for the Total Followers detail screen in Stats. Modify this method when you want to
     * add more blocks to the followers detail screen.
     */
    @Provides
    @Singleton
    @Named(TOTAL_FOLLOWERS_DETAIL_USE_CASES)
    fun provideFollowersDetailUseCases(
        totalFollowersUseCaseFactory: TotalFollowersUseCaseFactory,
        followerTypesUseCase: FollowerTypesUseCase,
        followersUseCaseFactory: FollowersUseCaseFactory
    ): List<@JvmSuppressWildcards BaseStatsUseCase<*, *>> = listOf(
            totalFollowersUseCaseFactory.build(VIEW_ALL),
            followerTypesUseCase,
            followersUseCaseFactory.build(BLOCK_DETAIL)
    )

    /**
     * Provides a singleton usecase that represents the Followers detail screen.
     * @param useCases build the use cases
     */
    @Provides
    @Singleton
    @Named(TOTAL_FOLLOWERS_DETAIL_USE_CASE)
    fun provideFollowersDetailStatsUseCase(
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        statsSiteProvider: StatsSiteProvider,
        @Named(TOTAL_FOLLOWERS_DETAIL_USE_CASES) useCases: List<@JvmSuppressWildcards BaseStatsUseCase<*, *>>,
        uiModelMapper: UiModelMapper
    ): BaseListUseCase = BaseListUseCase(
            bgDispatcher,
            mainDispatcher,
            statsSiteProvider,
            useCases,
            { listOf(InsightType.TOTAL_FOLLOWERS, InsightType.FOLLOWER_TYPES, InsightType.FOLLOWERS) },
            uiModelMapper::mapInsights
    )

    @Provides
    @Singleton
    fun provideSharedPrefs(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
