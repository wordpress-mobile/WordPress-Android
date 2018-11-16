package org.wordpress.android.ui.stats.refresh.usecases

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
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.Failed
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.ListInsightItem
import org.wordpress.android.ui.stats.refresh.toFormattedString
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.Facebook
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.GooglePlus
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.LinkedIn
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.Path
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.Tumblr
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.Twitter
import org.wordpress.android.ui.stats.refresh.usecases.PublicizeUseCase.Service.Unknown
import org.wordpress.android.util.ImageResource
import org.wordpress.android.util.TextResource
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val FACEBOOK_ICON = "https://secure.gravatar.com/blavatar/2343ec78a04c6ea9d80806345d31fd78?s="
private const val TWITTER_ICON = "https://secure.gravatar.com/blavatar/7905d1c4e12c54933a44d19fcd5f9356?s="
private const val TUMBLR_ICON = "https://secure.gravatar.com/blavatar/84314f01e87cb656ba5f382d22d85134?s="
private const val GOOGLE_PLUS_ICON = "https://secure.gravatar.com/blavatar/4a4788c1dfc396b1f86355b274cc26b3?s="
private const val LINKED_IN_ICON = "https://secure.gravatar.com/blavatar/f54db463750940e0e7f7630fe327845e?s="
private const val PATH_ICON = "https://secure.gravatar.com/blavatar/3a03c8ce5bf1271fb3760bb6e79b02c1?s="

class PublicizeUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsStore: InsightsStore,
    private val resourceProvider: ResourceProvider
) : BaseInsightsUseCase(PUBLICIZE, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel): InsightsItem? {
        return buildPublicizeUiModel(insightsStore.getPublicizeData(site))
    }

    override suspend fun fetchRemoteData(site: SiteModel, refresh: Boolean, forced: Boolean): InsightsItem {
        val response = insightsStore.fetchPublicizeData(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> Failed(
                    string.stats_view_publicize,
                    error.message ?: error.type.name
            )
            else -> buildPublicizeUiModel(model)
        }
    }

    private fun buildPublicizeUiModel(model: PublicizeModel?): InsightsItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_view_publicize))
        val dimension = resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)
        if (model == null || model.services.isEmpty()) {
            items.add(Empty)
        } else {
            model.services.mapIndexed { index, service ->
                val mappedService = getService(service.name)
                Item(
                        mappedService.iconUrl?.let { ImageResource(it + dimension) },
                        mappedService.name,
                        TextResource(service.followers.toFormattedString()),
                        index < model.services.size - 1
                )
            }
        }
        return ListInsightItem(items)
    }

    sealed class Service(val iconUrl: String? = null, val name: TextResource) {
        object Facebook : Service(FACEBOOK_ICON, TextResource(R.string.stats_insights_facebook))
        object Twitter : Service(TWITTER_ICON, TextResource(R.string.stats_insights_twitter))
        object Tumblr : Service(TUMBLR_ICON, TextResource(R.string.stats_insights_tumblr))
        object GooglePlus : Service(GOOGLE_PLUS_ICON, TextResource(R.string.stats_insights_google_plus))
        object LinkedIn : Service(LINKED_IN_ICON, TextResource(R.string.stats_insights_linkedin))
        object Path : Service(PATH_ICON, TextResource(R.string.stats_insights_path))
        data class Unknown(val label: String) : Service(name = TextResource(label))
    }

    private fun getService(service: String): Service {
        return when (service) {
            "facebook" -> Facebook
            "twitter" -> Twitter
            "google_plus" -> GooglePlus
            "tumblr" -> Tumblr
            "linkedin" -> LinkedIn
            "path" -> Path
            else -> Unknown(service)
        }
    }
}
