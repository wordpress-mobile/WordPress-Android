package org.wordpress.android.ui.stats.refresh.utils

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper.Service.FACEBOOK
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper.Service.GOOGLE_PLUS
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper.Service.LINKED_IN
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper.Service.PATH
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper.Service.TUMBLR
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper.Service.TWITTER
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

private const val FACEBOOK_ICON = "https://secure.gravatar.com/blavatar/2343ec78a04c6ea9d80806345d31fd78?s="
private const val TWITTER_ICON = "https://secure.gravatar.com/blavatar/7905d1c4e12c54933a44d19fcd5f9356?s="
private const val TUMBLR_ICON = "https://secure.gravatar.com/blavatar/84314f01e87cb656ba5f382d22d85134?s="
private const val GOOGLE_PLUS_ICON = "https://secure.gravatar.com/blavatar/4a4788c1dfc396b1f86355b274cc26b3?s="
private const val LINKED_IN_ICON = "https://secure.gravatar.com/blavatar/f54db463750940e0e7f7630fe327845e?s="
private const val PATH_ICON = "https://secure.gravatar.com/blavatar/3a03c8ce5bf1271fb3760bb6e79b02c1?s="

class ServiceMapper
@Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils,
    private val contentDescriptionHelper: ContentDescriptionHelper
) {
    fun map(
        services: List<PublicizeModel.Service>,
        header: Header
    ): List<ListItemWithIcon> {
        val dimension = resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)
        return services.mapIndexed { index, service ->
            val mappedService = getService(service.name)
            val text = if (mappedService?.nameResource == null) service.name else null
            val contentDescription = contentDescriptionHelper.buildContentDescription(
                header,
                text ?: "",
                service.followers
            )
            ListItemWithIcon(
                iconUrl = mappedService?.iconUrl?.let { it + dimension },
                text = text,
                textResource = mappedService?.nameResource,
                value = statsUtils.toFormattedString(service.followers),
                showDivider = index < services.size - 1,
                contentDescription = contentDescription
            )
        }
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
