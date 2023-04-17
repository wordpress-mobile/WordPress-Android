package org.wordpress.android.ui.mysite.cards.dashboard.activity

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard.ActivityCardWithItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard.ActivityCardWithItems.ActivityItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.ActivityCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.DashboardCardActivityLogConfig
import java.util.Date
import javax.inject.Inject

private const val MAX_ITEMS_IN_CARD: Int = 3

class ActivityCardBuilder @Inject constructor(
    private val dashboardCardActivityLogConfig: DashboardCardActivityLogConfig,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val siteUtilsWrapper: SiteUtilsWrapper,
) {
    fun build(params: ActivityCardBuilderParams): ActivityCard? {
        return if (shouldBuildActivityCard(params)) {
            buildActivityCard(params)
        } else {
            null
        }
    }

    private fun buildActivityCard(params: ActivityCardBuilderParams): ActivityCardWithItems {
        val activities = params.activityCardModel?.activities
        val content = activities?.take(MAX_ITEMS_IN_CARD)?.mapToActivityItems(params.onActivityItemClick) ?: emptyList()
        return ActivityCardWithItems(
            title = UiStringRes(R.string.dashboard_activity_card_title),
            activityItems = content,
            footerLink = ActivityCard.FooterLink(
                label = UiStringRes(R.string.dashboard_activity_card_footer_link),
                onClick = params.onFooterLinkClick
            )
        )
    }

    private fun List<ActivityLogModel>.mapToActivityItems(onClick: (activityId: String) -> Unit) =
        map {
            ActivityItem(
                label = UiString.UiStringText(it.content?.text?:""),
                subLabel = it.summary,
                displayDate = buildDateLine(it.published),
                icon = ActivityLogListItem.Icon.fromValue(it.gridicon).drawable,
                iconBackgroundColor = ActivityLogListItem.Status.fromValue(it.status).color,
                onClick = ListItemInteraction.create(it.activityID, onClick),
            )
        }

    private fun buildDateLine(published: Date) = dateTimeUtilsWrapper.javaDateToTimeSpan(published)

    private fun shouldBuildActivityCard(params: ActivityCardBuilderParams) : Boolean {
        if (!dashboardCardActivityLogConfig.isEnabled() ||
            params.activityCardModel == null ||
            params.activityCardModel.activities.isEmpty()) {
            return false
        }

        val isWpComOrJetpack = siteUtilsWrapper.isAccessedViaWPComRest(
            params.site
        ) || params.site.isJetpackConnected
        return params.site.hasCapabilityManageOptions && isWpComOrJetpack && !params.site.isWpForTeamsSite
    }
}
