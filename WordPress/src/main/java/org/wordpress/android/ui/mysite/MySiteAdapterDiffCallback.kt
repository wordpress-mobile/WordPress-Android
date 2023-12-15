package org.wordpress.android.ui.mysite

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ActivityCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloganuaryNudgeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardPlansCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.Dynamic
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ErrorWithinCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackFeatureCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackSwitchMenu
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PersonalizeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryEmptyHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.SingleActionCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge

@Suppress("ComplexMethod")
object MySiteAdapterDiffCallback : DiffUtil.ItemCallback<MySiteCardAndItem>() {
    override fun areItemsTheSame(oldItem: MySiteCardAndItem, updatedItem: MySiteCardAndItem): Boolean {
        return oldItem.type == updatedItem.type && when {
            oldItem is SiteInfoHeaderCard && updatedItem is SiteInfoHeaderCard -> true
            oldItem is QuickLinksItem && updatedItem is QuickLinksItem -> true
            oldItem is DomainRegistrationCard && updatedItem is DomainRegistrationCard -> true
            oldItem is QuickStartCard && updatedItem is QuickStartCard -> true
            oldItem is InfoItem && updatedItem is InfoItem -> oldItem.title == updatedItem.title
            oldItem is CategoryHeaderItem && updatedItem is CategoryHeaderItem -> oldItem.title == updatedItem.title
            oldItem is CategoryEmptyHeaderItem
                    && updatedItem is CategoryEmptyHeaderItem -> oldItem.title == updatedItem.title

            oldItem is ListItem && updatedItem is ListItem -> oldItem.primaryText == updatedItem.primaryText
            oldItem is ErrorCard && updatedItem is ErrorCard -> true
            oldItem is ErrorWithinCard && updatedItem is ErrorWithinCard -> true
            oldItem is TodaysStatsCardWithData && updatedItem is TodaysStatsCardWithData -> true
            oldItem is PostCardWithPostItems && updatedItem is PostCardWithPostItems -> true
            oldItem is BloggingPromptCardWithData && updatedItem is BloggingPromptCardWithData -> true
            oldItem is BloganuaryNudgeCardModel && updatedItem is BloganuaryNudgeCardModel -> true
            oldItem is PromoteWithBlazeCard && updatedItem is PromoteWithBlazeCard -> true
            oldItem is BlazeCampaignsCardModel && updatedItem is BlazeCampaignsCardModel -> true
            oldItem is DashboardPlansCard && updatedItem is DashboardPlansCard -> true
            oldItem is PagesCard && updatedItem is PagesCard -> true
            oldItem is ActivityCard && updatedItem is ActivityCard -> true
            oldItem is JetpackBadge && updatedItem is JetpackBadge -> true
            oldItem is SingleActionCard && updatedItem is SingleActionCard -> {
                oldItem.textResource == updatedItem.textResource
                        && oldItem.imageResource == updatedItem.imageResource
            }

            oldItem is JetpackFeatureCard && updatedItem is JetpackFeatureCard -> true
            oldItem is JetpackSwitchMenu && updatedItem is JetpackSwitchMenu -> true
            oldItem is JetpackInstallFullPluginCard && updatedItem is JetpackInstallFullPluginCard -> true
            oldItem is MySiteCardAndItem.Card.NoCardsMessage && updatedItem is
                    MySiteCardAndItem.Card.NoCardsMessage -> true
            oldItem is PersonalizeCardModel && updatedItem is PersonalizeCardModel -> true
            oldItem is WpSotw2023NudgeCardModel && updatedItem is WpSotw2023NudgeCardModel -> true
            oldItem is Dynamic && updatedItem is Dynamic -> {
                oldItem.id == updatedItem.id
            }
            else -> throw UnsupportedOperationException("Diff not implemented yet")
        }
    }


    override fun areContentsTheSame(oldItem: MySiteCardAndItem, newItem: MySiteCardAndItem): Boolean {
        if (oldItem.activeQuickStartItem || newItem.activeQuickStartItem) return false
        return oldItem == newItem
    }
}
