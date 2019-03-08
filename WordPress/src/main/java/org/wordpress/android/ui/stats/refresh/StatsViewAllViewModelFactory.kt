package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.PostsAndPagesUseCase.PostsAndPagesUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.CommentsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowersUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TagsAndCategoriesUseCase
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Named

class StatsViewAllViewModelFactory(
    private val mainDispatcher: CoroutineDispatcher,
    private val bgDispatcher: CoroutineDispatcher,
    private val useCase: BaseStatsUseCase<*, *>,
    @StringRes private val titleResource: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewAllViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewAllViewModel(mainDispatcher, bgDispatcher, useCase, titleResource) as T
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
            private val insightsUseCases: List<@JvmSuppressWildcards BaseStatsUseCase<*, *>>
    ) {
        fun build(type: StatsViewType, granularity: StatsGranularity?): StatsViewAllViewModelFactory {
            return if (granularity == null) {
                buildFactory(type)
            } else {
                buildFactory(type, granularity)
            }
        }

        private fun buildFactory(type: StatsViewType, granularity: StatsGranularity): StatsViewAllViewModelFactory {
            val (useCase, title) = getGranularUseCase(type, granularity, granularFactories)
            return StatsViewAllViewModelFactory(mainDispatcher, bgDispatcher, useCase, title)
        }

        private fun buildFactory(type: StatsViewType): StatsViewAllViewModelFactory {
            val (useCase, title) = getInsightsUseCase(type, insightsUseCases)
            return StatsViewAllViewModelFactory(mainDispatcher, bgDispatcher, useCase, title)
        }

        private fun getGranularUseCase(
            type: StatsViewType,
            granularity: StatsGranularity,
            granularFactories: List<GranularUseCaseFactory>
        ): Pair<BaseStatsUseCase<*, *>, Int> {
            return when (type) {
                StatsViewType.TOP_POSTS_AND_PAGES -> {
                    Pair(granularFactories.first { it is PostsAndPagesUseCaseFactory }.build(granularity, VIEW_ALL),
                            R.string.stats_view_top_posts_and_pages)
                }
                StatsViewType.REFERRERS -> {
                    Pair(granularFactories.first { it is PostsAndPagesUseCaseFactory }.build(granularity, VIEW_ALL),
                            R.string.stats_view_referrers)
                }
                StatsViewType.CLICKS -> TODO()
                StatsViewType.AUTHORS -> TODO()
                StatsViewType.GEOVIEWS -> TODO()
                StatsViewType.SEARCH_TERMS -> TODO()
                StatsViewType.VIDEO_PLAYS -> TODO()
                else -> throw InvalidParameterException("Invalid granular stats type: ${type.name}")
            }
        }

        private fun getInsightsUseCase(
            type: StatsViewType,
            insightsUseCases: List<BaseStatsUseCase<*, *>>
        ): Pair<BaseStatsUseCase<*, *>, Int> {
            return when (type) {
                StatsViewType.FOLLOWERS -> Pair(insightsUseCases.first { it is FollowersUseCase },
                        R.string.stats_view_followers)
                StatsViewType.COMMENTS -> Pair(insightsUseCases.first { it is CommentsUseCase },
                        R.string.stats_view_comments)
                StatsViewType.TAGS_AND_CATEGORIES -> Pair(insightsUseCases.first { it is TagsAndCategoriesUseCase },
                        R.string.stats_view_comments)
                StatsViewType.INSIGHTS_ALL_TIME -> TODO()
                StatsViewType.INSIGHTS_LATEST_POST_SUMMARY -> TODO()
                StatsViewType.INSIGHTS_MOST_POPULAR -> TODO()
                StatsViewType.INSIGHTS_TODAY -> TODO()
                StatsViewType.PUBLICIZE -> TODO()
                else -> throw InvalidParameterException("Invalid insights stats type: ${type.name}")
            }
        }
    }
}
