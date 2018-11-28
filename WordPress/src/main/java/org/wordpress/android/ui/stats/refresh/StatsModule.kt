package org.wordpress.android.ui.stats.refresh

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.dwmy.usecases.PostsAndPagesUseCase.PostsAndPagesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.AllTimeStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.CommentsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.MostPopularInsightsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TodayStatsUseCase
import javax.inject.Named
import javax.inject.Singleton

const val INSIGHTS_USE_CASES = "InsightsUseCases"
const val INSIGHTS_USE_CASE = "InsightsUseCase"
const val DAY_STATS_USE_CASES = "DayStatsUseCases"
const val DAY_STATS_USE_CASE = "DayStatsUseCase"

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
        tagsAndCategoriesUseCase: TagsAndCategoriesUseCase
    ): List<@JvmSuppressWildcards BaseStatsUseCase> {
        return listOf(
                allTimeStatsUseCase,
                latestPostSummaryUseCase,
                todayStatsUseCase,
                followersUseCase,
                commentsUseCase,
                mostPopularInsightsUseCase,
                tagsAndCategoriesUseCase
        )
    }

    @Provides
    @Singleton
    @Named(DAY_STATS_USE_CASES)
    fun provideDayStatsUseCases(
        postsAndPagesUseCaseFactory: PostsAndPagesUseCaseFactory
    ): List<@JvmSuppressWildcards BaseStatsUseCase> {
        return listOf(postsAndPagesUseCaseFactory.build(DAYS))
    }

    @Provides
    @Singleton
    @Named(INSIGHTS_USE_CASE)
    fun provideInsightsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        @Named(INSIGHTS_USE_CASES) useCases: List<@JvmSuppressWildcards BaseStatsUseCase>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, {
            statsStore.getInsights()
        }, useCases)
    }

    @Provides
    @Singleton
    @Named(DAY_STATS_USE_CASE)
    fun provideDayStatsUseCase(
        statsStore: StatsStore,
        @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
        @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
        @Named(DAY_STATS_USE_CASES) useCases: List<@JvmSuppressWildcards BaseStatsUseCase>
    ): BaseListUseCase {
        return BaseListUseCase(bgDispatcher, mainDispatcher, {
            statsStore.getTimeStatsTypes()
        }, useCases)
    }
}
