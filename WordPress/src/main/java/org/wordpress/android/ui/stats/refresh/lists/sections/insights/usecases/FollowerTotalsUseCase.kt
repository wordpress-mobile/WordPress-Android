package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWER_TOTALS
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.fluxc.store.stats.insights.PublicizeStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTotalsUseCase.FollowerType
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTotalsUseCase.FollowerType.EMAIL
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTotalsUseCase.FollowerType.SOCIAL
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.FollowerTotalsUseCase.FollowerType.WP_COM
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject
import javax.inject.Named

class FollowerTotalsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val followersStore: FollowersStore,
    private val publicizeStore: PublicizeStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val contentDescriptionHelper: ContentDescriptionHelper,
    private val statsUtils: StatsUtils,
    private val popupMenuHandler: ItemPopupMenuHandler
) : StatelessUseCase<Map<FollowerType, Int>>(FOLLOWER_TOTALS, mainDispatcher, bgDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_follower_totals))

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override suspend fun loadCachedData(): Map<FollowerType, Int>? {
        val wpComFollowers = followersStore.getWpComFollowers(statsSiteProvider.siteModel, LimitMode.Top(0))
        val emailFollowers = followersStore.getEmailFollowers(statsSiteProvider.siteModel, LimitMode.Top(0))
        val publicizeServices = publicizeStore.getPublicizeData(statsSiteProvider.siteModel, LimitMode.All)
        if (wpComFollowers != null && emailFollowers != null && publicizeServices != null) {
            val socialFollowers = publicizeServices.services.sumBy { it.followers }
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

    override suspend fun fetchRemoteData(
        forced: Boolean
    ): State<Map<FollowerType, Int>> {
        return fetchData(forced, PagedMode(0, false))
    }

    private suspend fun fetchData(
        forced: Boolean,
        fetchMode: PagedMode
    ): State<Map<FollowerType, Int>> {
        val deferredWpComResponse = async(bgDispatcher) {
            followersStore.fetchWpComFollowers(
                    statsSiteProvider.siteModel,
                    fetchMode,
                    forced
            )
        }
        val deferredEmailResponse = async(bgDispatcher) {
            followersStore.fetchEmailFollowers(
                    statsSiteProvider.siteModel,
                    fetchMode,
                    forced
            )
        }
        val deferredPublicizeResponse = async(bgDispatcher) {
            publicizeStore.fetchPublicizeData(
                    statsSiteProvider.siteModel,
                    LimitMode.All,
                    forced
            )
        }

        val wpComResponse = deferredWpComResponse.await()
        val emailResponse = deferredEmailResponse.await()
        val publicizeResponse = deferredPublicizeResponse.await()

        val wpComModel = wpComResponse.model
        val emailModel = emailResponse.model
        val socialTotals = publicizeResponse.model?.services?.sumBy { it.followers }

        val error = wpComResponse.error ?: emailResponse.error ?: publicizeResponse.error
        val data = buildDataModel(wpComModel?.totalCount, emailModel?.totalCount, socialTotals)
        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            data.isNotEmpty() -> State.Data(data)
            else -> State.Empty()
        }
    }

    private fun getIcon(type: FollowerType): Int {
        return when (type) {
            WP_COM -> R.drawable.ic_my_sites_white_24dp
            EMAIL -> R.drawable.ic_mail_white_24dp
            SOCIAL -> R.drawable.ic_share_white_24dp
        }
    }

    private fun getTitle(type: FollowerType): Int {
        return when (type) {
            WP_COM -> R.string.stats_followers_wordpress_com
            EMAIL -> R.string.email
            SOCIAL -> R.string.stats_insights_social
        }
    }

    override fun buildUiModel(domainModel: Map<FollowerType, Int>): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(buildTitle())

        if (domainModel.isNotEmpty()) {
            domainModel.entries.forEach {
                val title = getTitle(it.key)
                items.add(
                        ListItemWithIcon(
                                icon = getIcon(it.key),
                                textResource = title,
                                value = statsUtils.toFormattedString(it.value),
                                showDivider = domainModel.entries.indexOf(it) < domainModel.size - 1,
                                contentDescription = contentDescriptionHelper.buildContentDescription(
                                        title,
                                        it.value
                                )
                        )
                )
            }
        } else {
            items.add(Empty())
        }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_view_follower_totals, menuAction = this::onMenuClick)

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }

    enum class FollowerType { WP_COM, EMAIL, SOCIAL }
}
