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
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TAGS_AND_CATEGORIES
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.AuthorsUseCase.AuthorsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ClicksUseCase.ClicksUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.CountryViewsUseCase.CountryViewsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.OverviewUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.PostsAndPagesUseCase.PostsAndPagesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ReferrersUseCase.ReferrersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.SearchTermsUseCase.SearchTermsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.VideoPlaysUseCase.VideoPlaysUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AllTimeStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.CommentsUseCase.CommentsUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase.FollowersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.MostPopularInsightsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.PublicizeUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase.TagsAndCategoriesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TodayStatsUseCase
import javax.inject.Named
import javax.inject.Singleton

const val INSIGHTS_USE_CASE = "InsightsUseCase"
const val DAY_STATS_USE_CASE = "DayStatsUseCase"
const val WEEK_STATS_USE_CASE = "WeekStatsUseCase"
const val MONTH_STATS_USE_CASE = "MonthStatsUseCase"
const val YEAR_STATS_USE_CASE = "YearStatsUseCase"
const val VIEW_ALL_FOLLOWERS_USE_CASE = "ViewAllFollowersUseCase"
const val VIEW_ALL_COMMENTS_USE_CASE = "ViewAllCommentsUseCase"
const val VIEW_ALL_TAGS_AND_CATEGORIES_USE_CASE = "ViewAllTagsAndCategoriesUseCase"

// These are injected only internally
private const val INSIGHTS_USE_CASES = "InsightsUseCases"
private const val GRANULAR_USE_CASE_FACTORIES = "GranularUseCaseFactories"

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
    @Named(INSIGHTS_USE_CASES)
    fun provideInsightsUseCases(
        allTimeStatsUseCase: AllTimeStatsUseCase,
        latestPostSummaryUseCase: LatestPostSummaryUseCase,
        todayStatsUseCase: TodayStatsUseCase,
        followersUseCaseFactory: FollowersUseCaseFactory,
        commentsUseCaseFactory: CommentsUseCaseFactory,
        mostPopularInsightsUseCase: MostPopularInsightsUseCase,
        tagsAndCategoriesUseCaseFactory: TagsAndCategoriesUseCaseFactory,
        publicizeUseCase: PublicizeUseCase
    ): List<@JvmSuppressWildcards BaseStatsUseCase<*, *>> {
        return listOf(
                allTimeStatsUseCase,
                latestPostSummaryUseCase,
                todayStatsUseCase,
                followersUseCaseFactory.build(UseCaseMode.BLOCK),
                commentsUseCaseFactory.build(UseCaseMode.BLOCK),
                mostPopularInsightsUseCase,
                tagsAndCategoriesUseCaseFactory.build(UseCaseMode.BLOCK),
                publicizeUseCase
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
        overviewUseCaseFactory: OverviewUseCaseFactory
    ): List<@JvmSuppressWildcards GranularUseCaseFactory> {
        return listOf(
                postsAndPagesUseCaseFactory,
                referrersUseCaseFactory,
                clicksUseCaseFactory,
                countryViewsUseCaseFactory,
                videoPlaysUseCaseFactory,
                searchTermsUseCaseFactory,
                authorsUseCaseFactory,
                overviewUseCaseFactory
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
        @Named(INSIGHTS_USE_CASES) useCases: List<@JvmSuppressWildcards BaseStatsUseCase<*, *>>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCases) {
            statsStore.getInsights()
        }
    }

    /**
     * Provides a singleton FollowersUseCase that represents the Followers View all screen
     * @param followersUseCaseFactory build the use cases for the Followers
     */
    @Provides
    @Singleton
    @Named(VIEW_ALL_FOLLOWERS_USE_CASE)
    fun provideViewAllFollowersUseCase(
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        followersUseCaseFactory: FollowersUseCaseFactory
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                listOf(followersUseCaseFactory.build(UseCaseMode.VIEW_ALL))
        ) {
            listOf(FOLLOWERS)
        }
    }

    /**
     * Provides a singleton CommentsUseCase that represents the Followers View all screen
     * @param commentsUseCaseFactory build the use cases for the Followers
     */
    @Provides
    @Singleton
    @Named(VIEW_ALL_COMMENTS_USE_CASE)
    fun provideViewAllCommentsUseCase(
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        commentsUseCaseFactory: CommentsUseCaseFactory
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                listOf(commentsUseCaseFactory.build(UseCaseMode.VIEW_ALL))
        ) {
            listOf(COMMENTS)
        }
    }

    /**
     * Provides a singleton TagsAndCategoriesUseCase that represents the Followers View all screen
     * @param tagsAndCategoriesUseCaseFactory build the use cases for the Followers
     */
    @Provides
    @Singleton
    @Named(VIEW_ALL_TAGS_AND_CATEGORIES_USE_CASE)
    fun provideViewAllTagsAndCategoriesUseCase(
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        tagsAndCategoriesUseCaseFactory: TagsAndCategoriesUseCaseFactory
    ): BaseListUseCase {
        return BaseListUseCase(
                bgDispatcher,
                mainDispatcher,
                listOf(tagsAndCategoriesUseCaseFactory.build(UseCaseMode.VIEW_ALL))
        ) {
            listOf(TAGS_AND_CATEGORIES)
        }
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
        @Named(GRANULAR_USE_CASE_FACTORIES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCasesFactories.map { it.build(WEEKS) }) {
            statsStore.getTimeStatsTypes()
        }
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
        @Named(GRANULAR_USE_CASE_FACTORIES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCasesFactories.map { it.build(MONTHS) }) {
            statsStore.getTimeStatsTypes()
        }
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
        @Named(GRANULAR_USE_CASE_FACTORIES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCasesFactories.map { it.build(YEARS) }) {
            statsStore.getTimeStatsTypes()
        }
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
        @Named(GRANULAR_USE_CASE_FACTORIES) useCasesFactories: List<@JvmSuppressWildcards GranularUseCaseFactory>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCasesFactories.map { it.build(DAYS) }) {
            statsStore.getTimeStatsTypes()
        }
    }

    @Provides
    @Singleton
    fun provideSharedPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
