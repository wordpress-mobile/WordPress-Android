package org.wordpress.android.ui.stats.refresh

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.dwmy.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.dwmy.usecases.ClicksUseCase.ClicksUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.dwmy.usecases.PostsAndPagesUseCase.PostsAndPagesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.dwmy.usecases.ReferrersUseCase.ReferrersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AllTimeStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.CommentsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.MostPopularInsightsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.PublicizeUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TodayStatsUseCase
import javax.inject.Named
import javax.inject.Singleton

const val INSIGHTS_USE_CASE = "InsightsUseCase"
const val DAY_STATS_USE_CASE = "DayStatsUseCase"
const val WEEK_STATS_USE_CASE = "WeekStatsUseCase"
const val MONTH_STATS_USE_CASE = "MonthStatsUseCase"
const val YEAR_STATS_USE_CASE = "YearStatsUseCase"
// These are injected only internally
private const val INSIGHTS_USE_CASES = "InsightsUseCases"
private const val DWMY_USE_CASE_FACTORIES = "DWMYUseCaseFactories"

@Module
class StatsModule {
    @Provides
    @Singleton
    @Named(INSIGHTS_USE_CASES)
    fun provideInsightsUseCases(
        allTimeStatsUseCase: AllTimeStatsUseCase,
        latestPostSummaryUseCase: LatestPostSummaryUseCase,
        todayStatsUseCase: TodayStatsUseCase,
        followersUseCase: FollowersUseCase,
        commentsUseCase: CommentsUseCase,
        mostPopularInsightsUseCase: MostPopularInsightsUseCase,
        tagsAndCategoriesUseCase: TagsAndCategoriesUseCase,
        publicizeUseCase: PublicizeUseCase
    ): List<@JvmSuppressWildcards BaseStatsUseCase<*, *>> {
        return listOf(
                allTimeStatsUseCase,
                latestPostSummaryUseCase,
                todayStatsUseCase,
                followersUseCase,
                commentsUseCase,
                mostPopularInsightsUseCase,
                tagsAndCategoriesUseCase,
                publicizeUseCase
        )
    }

    @Provides
    @Singleton
    @Named(DWMY_USE_CASE_FACTORIES)
    fun provideDayStatsUseCases(
        postsAndPagesUseCaseFactory: PostsAndPagesUseCaseFactory,
        referrersUseCaseFactory: ReferrersUseCaseFactory,
        clicksUseCaseFactory: ClicksUseCaseFactory
    ): List<@JvmSuppressWildcards UseCaseFactory> {
        return listOf(postsAndPagesUseCaseFactory, referrersUseCaseFactory, clicksUseCaseFactory)
    }

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

    @Provides
    @Singleton
    @Named(DAY_STATS_USE_CASE)
    fun provideDayStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        @Named(DWMY_USE_CASE_FACTORIES) useCases: List<@JvmSuppressWildcards UseCaseFactory>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCases.map { it.build(DAYS) }) {
            statsStore.getTimeStatsTypes()
        }
    }

    @Provides
    @Singleton
    @Named(WEEK_STATS_USE_CASE)
    fun provideWeekStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        @Named(DWMY_USE_CASE_FACTORIES) useCases: List<@JvmSuppressWildcards UseCaseFactory>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCases.map { it.build(WEEKS) }) {
            statsStore.getTimeStatsTypes()
        }
    }

    @Provides
    @Singleton
    @Named(MONTH_STATS_USE_CASE)
    fun provideMonthStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        @Named(DWMY_USE_CASE_FACTORIES) useCases: List<@JvmSuppressWildcards UseCaseFactory>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCases.map { it.build(MONTHS) }) {
            statsStore.getTimeStatsTypes()
        }
    }

    @Provides
    @Singleton
    @Named(YEAR_STATS_USE_CASE)
    fun provideYearStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        @Named(DWMY_USE_CASE_FACTORIES) useCases: List<@JvmSuppressWildcards UseCaseFactory>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, useCases.map { it.build(YEARS) }) {
            statsStore.getTimeStatsTypes()
        }
    }
}
