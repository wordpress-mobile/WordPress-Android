package org.wordpress.android.ui.mysite.cards.dynamiccard

import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.CardOrder
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardModel
import org.wordpress.android.ui.deeplinks.handlers.DeepLinkHandlers
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.Dynamic
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DynamicCardsBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.config.DynamicDashboardCardsFeatureConfig
import org.wordpress.android.util.config.FeatureFlagConfig
import javax.inject.Inject

class DynamicCardsBuilder @Inject constructor(
    private val urlUtils: UrlUtilsWrapper,
    private val deepLinkHandlers: DeepLinkHandlers,
    private val dynamicDashboardCardsFeatureConfig: DynamicDashboardCardsFeatureConfig,
    private val featureFlagConfig: FeatureFlagConfig,
) {
    fun build(params: DynamicCardsBuilderParams, order: CardOrder): List<Dynamic>? {
        if (!dynamicDashboardCardsFeatureConfig.isEnabled() || !shouldBuildCard(params, order)) {
            return null
        }
        return convertToDynamicCards(params, order)
    }

    private fun shouldBuildCard(params: DynamicCardsBuilderParams, order: CardOrder): Boolean {
        return !(params.dynamicCards == null || params.dynamicCards.dynamicCards.none { it.order == order })
    }

    private fun convertToDynamicCards(params: DynamicCardsBuilderParams, order: CardOrder): List<Dynamic> {
        val cards = params.dynamicCards?.dynamicCards?.filter { it.order == order && it.isEnabled() }.orEmpty()
        return cards.map { card ->
            Dynamic(
                id = card.id,
                rows = card.rows.map { row ->
                    Dynamic.Row(
                        iconUrl = row.icon,
                        title = row.title,
                        description = row.description,
                    )
                },
                title = card.title,
                image = card.featuredImage,
                action = getActionSource(params, card),
                onHideMenuItemClick = ListItemInteraction.create(card.id, params.onHideMenuItemClick),
            )
        }
    }

    fun DynamicCardModel.isEnabled(): Boolean {
        // If there is no feature flag or there is no such remote feature flag, then the card is enabled
        if (remoteFeatureFlag.isNullOrEmpty() ||
            featureFlagConfig.getString(requireNotNull(remoteFeatureFlag)).isEmpty()
        ) return true
        return featureFlagConfig.isEnabled(requireNotNull(remoteFeatureFlag))
    }

    private fun getActionSource(
        params: DynamicCardsBuilderParams,
        card: DynamicCardModel
    ): Dynamic.ActionSource? = when {
        isValidUrlOrDeeplink(card.url) && isValidActionTitle(card.action) -> Dynamic.ActionSource.Button(
            url = requireNotNull(card.url),
            title = requireNotNull(card.action),
            onClick = ListItemInteraction.create(
                data = DynamicCardsBuilderParams.ClickParams(id = card.id, actionUrl = requireNotNull(card.url)),
                action = params.onCtaClick
            )
        )
        isValidUrlOrDeeplink(card.url) -> Dynamic.ActionSource.Card(
            url = requireNotNull(card.url),
            onClick = ListItemInteraction.create(
                data = DynamicCardsBuilderParams.ClickParams(id = card.id, actionUrl = requireNotNull(card.url)),
                action = params.onCardClick
            )
        )
        else -> null
    }

    private fun isValidUrlOrDeeplink(url: String?): Boolean {
        return !url.isNullOrEmpty() && (urlUtils.isValidUrlAndHostNotNull(url)
                || deepLinkHandlers.isDeepLink(url))
    }

    private fun isValidActionTitle(title: String?): Boolean {
        return !title.isNullOrEmpty()
    }
}
