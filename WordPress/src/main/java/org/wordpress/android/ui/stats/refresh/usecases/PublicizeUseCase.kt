package org.wordpress.android.ui.stats.refresh.usecases

import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.PUBLICIZE
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.BlockListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPublicizeStats
import org.wordpress.android.ui.stats.refresh.toFormattedString
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.FACEBOOK
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.GOOGLE_PLUS
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.LINKED_IN
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.PATH
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.TUMBLR
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.TWITTER
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val FACEBOOK_ICON = "https://secure.gravatar.com/blavatar/2343ec78a04c6ea9d80806345d31fd78?s="
private const val TWITTER_ICON = "https://secure.gravatar.com/blavatar/7905d1c4e12c54933a44d19fcd5f9356?s="
private const val TUMBLR_ICON = "https://secure.gravatar.com/blavatar/84314f01e87cb656ba5f382d22d85134?s="
private const val GOOGLE_PLUS_ICON = "https://secure.gravatar.com/blavatar/4a4788c1dfc396b1f86355b274cc26b3?s="
private const val LINKED_IN_ICON = "https://secure.gravatar.com/blavatar/f54db463750940e0e7f7630fe327845e?s="
private const val PATH_ICON = "https://secure.gravatar.com/blavatar/3a03c8ce5bf1271fb3760bb6e79b02c1?s="
private const val PAGE_SIZE = 5

class PublicizeUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore,
    private val resourceProvider: ResourceProvider
) : BaseInsightsUseCase(PUBLICIZE, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel): InsightsItem? {
        return insightsStore.getPublicizeData(site)?.let { buildPublicizeUiModel(site, it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): InsightsItem? {
        val response = insightsStore.fetchPublicizeData(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> failedItem(
                    string.stats_view_publicize,
                    error.message ?: error.type.name
            )
            else -> model?.let { buildPublicizeUiModel(site, model) }
        }
    }

    private fun buildPublicizeUiModel(site: SiteModel, model: PublicizeModel): InsightsItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_view_publicize))
        val dimension = resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)
        items.add(Label(R.string.stats_publicize_service_label, R.string.stats_publicize_followers_label))
        if (model.services.isEmpty()) {
            items.add(Empty)
        } else {
            items.addAll(model.services.take(PAGE_SIZE).mapIndexed { index, service ->
                val mappedService = getService(service.name)
                Item(
                        iconUrl = mappedService?.iconUrl?.let { it + dimension },
                        text = service.name,
                        textResource = mappedService?.nameResource,
                        value = service.followers.toFormattedString(),
                        showDivider = index < model.services.size - 1
                )
            })
            if (model.services.size >= PAGE_SIZE) {
                items.add(Link(text = string.stats_insights_view_more) {
                    navigateTo(ViewPublicizeStats(site.siteId))
                })
            }
        }
        return dataItem(items)
    }

    enum class Service(
        val iconUrl: String,
        @StringRes val nameResource: Int
    ) {
        FACEBOOK(FACEBOOK_ICON, nameResource = R.string.stats_insights_facebook),
        TWITTER(TWITTER_ICON, nameResource = R.string.stats_insights_twitter),
        TUMBLR(TUMBLR_ICON, nameResource = R.string.stats_insights_tumblr),
        GOOGLE_PLUS(GOOGLE_PLUS_ICON, nameResource = R.string.stats_insights_google_plus),
        LINKED_IN(LINKED_IN_ICON, nameResource = R.string.stats_insights_linkedin),
        PATH(PATH_ICON, nameResource = R.string.stats_insights_path)
    }

    private fun getService(service: String): Service? {
        return when (service) {
            "facebook" -> FACEBOOK
            "twitter" -> TWITTER
            "google_plus" -> GOOGLE_PLUS
            "tumblr" -> TUMBLR
            "linkedin" -> LINKED_IN
            "path" -> PATH
            else -> null
        }
    }
}
