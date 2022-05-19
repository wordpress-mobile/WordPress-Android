package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWER_TYPES
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.fluxc.store.stats.insights.PublicizeStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTypesUseCase.FollowerType
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTypesUseCase.FollowerType.EMAIL
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTypesUseCase.FollowerType.SOCIAL
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTypesUseCase.FollowerType.WP_COM
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class FollowerTypesUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val followersStore: FollowersStore,
    private val publicizeStore: PublicizeStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils,
    private val resourceProvider: ResourceProvider
) : StatelessUseCase<Map<FollowerType, Int>>(FOLLOWER_TYPES, mainDispatcher, bgDispatcher) {
    override fun buildLoadingItem() = listOf(Empty())

    override fun buildEmptyItem() = listOf(Empty(), Empty())

    override suspend fun loadCachedData(): Map<FollowerType, Int>? {
        val wpComFollowers = followersStore.getWpComFollowers(statsSiteProvider.siteModel, LimitMode.Top(0))
        val emailFollowers = followersStore.getEmailFollowers(statsSiteProvider.siteModel, LimitMode.Top(0))
        val publicizeServices = publicizeStore.getPublicizeData(statsSiteProvider.siteModel, LimitMode.All)
        if (wpComFollowers != null && emailFollowers != null && publicizeServices != null) {
            val socialFollowers = publicizeServices.services.sumOf { it.followers }
            return buildDataModel(wpComFollowers.totalCount, emailFollowers.totalCount, socialFollowers)
        }
        return null
    }

    private fun buildDataModel(wpComTotals: Int?, emailTotals: Int?, socialTotals: Int?): Map<FollowerType, Int> {
        val map = HashMap<FollowerType, Int>()
        wpComTotals?.let {
            if (it > 0) {
                map[WP_COM] = it
            }
        }
        emailTotals?.let {
            if (it > 0) {
                map[EMAIL] = it
            }
        }
        socialTotals?.let {
            if (it > 0) {
                map[SOCIAL] = it
            }
        }
        return map
    }

    override suspend fun fetchRemoteData(forced: Boolean) = fetchData(forced, PagedMode(0, false))

    private suspend fun fetchData(forced: Boolean, fetchMode: PagedMode): State<Map<FollowerType, Int>> {
        val deferredWpComResponse = async(bgDispatcher) {
            followersStore.fetchWpComFollowers(statsSiteProvider.siteModel, fetchMode, forced)
        }
        val deferredEmailResponse = async(bgDispatcher) {
            followersStore.fetchEmailFollowers(statsSiteProvider.siteModel, fetchMode, forced)
        }
        val deferredPublicizeResponse = async(bgDispatcher) {
            publicizeStore.fetchPublicizeData(statsSiteProvider.siteModel, LimitMode.All, forced)
        }

        val wpComResponse = deferredWpComResponse.await()
        val emailResponse = deferredEmailResponse.await()
        val publicizeResponse = deferredPublicizeResponse.await()

        val wpComModel = wpComResponse.model
        val emailModel = emailResponse.model
        val socialTotals = publicizeResponse.model?.services?.sumOf { it.followers }

        val error = wpComResponse.error ?: emailResponse.error ?: publicizeResponse.error
        val data = buildDataModel(wpComModel?.totalCount, emailModel?.totalCount, socialTotals)
        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            data.isNotEmpty() -> State.Data(data)
            else -> State.Empty()
        }
    }

    private fun getTitle(type: FollowerType) = when (type) {
        WP_COM -> R.string.stats_followers_wordpress_com
        EMAIL -> R.string.email
        SOCIAL -> R.string.stats_insights_social
    }

    override fun buildUiModel(domainModel: Map<FollowerType, Int>): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.isNotEmpty()) {
            val total = domainModel.entries.sumOf { it.value }

            domainModel.entries.forEach {
                val title = getTitle(it.key)
                val value = buildValuePercent(it.value, total)
                items.add(
                        ListItem(
                                text = resourceProvider.getString(title),
                                value = value,
                                showDivider = domainModel.entries.indexOf(it) < domainModel.size - 1,
                                contentDescription = contentDescriptionHelper.buildContentDescription(title, it.value)
                        )
                )
            }
        } else {
            items.add(Empty())
        }
        return items
    }

    private fun buildValuePercent(value: Int, total: Int): String {
        val percentage = if (total == 0 || value == 0) {
            0.0
        } else {
            value * PERCENT_HUNDRED / total
        }

        val formattedPercentage = if (percentage == 0.0 || percentage >= 1.0) {
            statsUtils.toFormattedString(percentage.toInt())
        } else {
            statsUtils.toFormattedString(percentage)
        }

        return resourceProvider.getString(
                R.string.stats_value_percent,
                statsUtils.toFormattedString(value),
                formattedPercentage
        )
    }

    enum class FollowerType { WP_COM, EMAIL, SOCIAL }

    companion object {
        private const val PERCENT_HUNDRED = 100.0
    }
}
