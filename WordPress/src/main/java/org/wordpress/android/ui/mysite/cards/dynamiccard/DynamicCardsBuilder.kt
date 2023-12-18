package org.wordpress.android.ui.mysite.cards.dynamiccard

import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.CardOrder
import org.wordpress.android.ui.deeplinks.handlers.DeepLinkHandlers
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.Dynamic
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DynamicCardsBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction.Companion.create
import org.wordpress.android.util.UriWrapper
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
        val cards = params.dynamicCards?.dynamicCards?.filter { it.order == order && it.isEnabled()}.orEmpty()
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
                onHideMenuItemClick = create(card.id, params.onHideMenuItemClick),
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
        card: CardModel.DynamicCardsModel.DynamicCardModel
    ): Dynamic.ActionSource? = when {
        isValidUrlOrDeeplink(card.url) && isValidActionTitle(card.action) -> Dynamic.ActionSource.Button(
            requireNotNull(card.url),
            create(requireNotNull(card.url), params.onActionClick),
            requireNotNull(card.action)
        )
        isValidUrlOrDeeplink(card.url) -> Dynamic.ActionSource.Card(
            requireNotNull(card.url),
            create(requireNotNull(card.url), params.onActionClick)
        )
        else -> null
    }

    private fun isValidUrlOrDeeplink(url: String?): Boolean {
        return !url.isNullOrEmpty() && (urlUtils.isValidUrlAndHostNotNull(url)
                || deepLinkHandlers.buildNavigateAction(UriWrapper(url)) != null)
    }

    private fun isValidActionTitle(title: String?): Boolean {
        return !title.isNullOrEmpty()
    }
}
