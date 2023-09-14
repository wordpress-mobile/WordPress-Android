package org.wordpress.android.ui.mysite.cards.dashboard

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DomainTransferCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardPlansCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorWithinCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DashboardCardType
import org.wordpress.android.ui.mysite.cards.blaze.BlazeCampaignsCardViewHolder
import org.wordpress.android.ui.mysite.cards.blaze.PromoteWithBlazeCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.activity.ActivityCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.cards.dashboard.error.ErrorCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.error.ErrorWithinCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer.DomainTransferCardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.plans.PlansCardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.image.ImageManager

class CardsAdapter(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    private val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker,
    private val htmlCompatWrapper: HtmlCompatWrapper,
    private val learnMoreClicked: () -> Unit
) : Adapter<CardViewHolder<*>>() {
    private val items = mutableListOf<DashboardCard>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder<*> {
        return when (viewType) {
            DashboardCardType.ERROR_CARD.ordinal -> ErrorCardViewHolder(parent)
            DashboardCardType.TODAYS_STATS_CARD_ERROR.ordinal,
            DashboardCardType.POST_CARD_ERROR.ordinal -> ErrorWithinCardViewHolder(parent, uiHelpers)
            DashboardCardType.TODAYS_STATS_CARD.ordinal -> TodaysStatsCardViewHolder(parent, uiHelpers)
            DashboardCardType.POST_CARD_WITH_POST_ITEMS.ordinal ->
                PostCardViewHolder.PostCardWithPostItemsViewHolder(parent, imageManager, uiHelpers)
            DashboardCardType.BLOGGING_PROMPT_CARD.ordinal -> BloggingPromptCardViewHolder(
                parent,
                uiHelpers,
                bloggingPromptsCardAnalyticsTracker,
                htmlCompatWrapper,
                learnMoreClicked
            )
            DashboardCardType.DASHBOARD_DOMAIN_TRANSFER_CARD.ordinal -> DomainTransferCardViewHolder(parent)
            DashboardCardType.PROMOTE_WITH_BLAZE_CARD.ordinal -> PromoteWithBlazeCardViewHolder(parent, uiHelpers)
            DashboardCardType.BLAZE_CAMPAIGNS_CARD.ordinal -> BlazeCampaignsCardViewHolder(parent)
            DashboardCardType.DASHBOARD_PLANS_CARD.ordinal -> PlansCardViewHolder(parent, uiHelpers)
            DashboardCardType.PAGES_CARD.ordinal -> PagesCardViewHolder(parent, uiHelpers)
            DashboardCardType.ACTIVITY_CARD.ordinal -> ActivityCardViewHolder(parent, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CardViewHolder<*>, position: Int) {
        when (holder) {
            is ErrorCardViewHolder -> holder.bind(items[position] as ErrorCard)
            is ErrorWithinCardViewHolder -> holder.bind(items[position] as ErrorWithinCard)
            is TodaysStatsCardViewHolder -> holder.bind(items[position] as TodaysStatsCardWithData)
            is PostCardViewHolder<*> -> holder.bind(items[position] as PostCard)
            is BloggingPromptCardViewHolder -> holder.bind(items[position] as BloggingPromptCardWithData)
            is DomainTransferCardViewHolder -> holder.bind(items[position] as DomainTransferCardModel)
            is PromoteWithBlazeCardViewHolder -> holder.bind(items[position] as PromoteWithBlazeCard)
            is BlazeCampaignsCardViewHolder -> holder.bind(items[position] as BlazeCampaignsCardModel)
            is PlansCardViewHolder -> holder.bind(items[position] as DashboardPlansCard)
            is PagesCardViewHolder -> holder.bind(items[position] as PagesCard)
            is ActivityCardViewHolder -> holder.bind(items[position] as ActivityCard)
        }
    }

    override fun getItemViewType(position: Int) = items[position].dashboardCardType.ordinal

    fun update(newItems: List<DashboardCard>) {
        val diffResult = DiffUtil.calculateDiff(DashboardCardsDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    @Suppress("ComplexMethod")
    class DashboardCardsDiffUtil(
        private val oldList: List<DashboardCard>,
        private val newList: List<DashboardCard>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val newItem = newList[newItemPosition]
            val oldItem = oldList[oldItemPosition]

            return oldItem.dashboardCardType == newItem.dashboardCardType && when {
                oldItem is ErrorCard && newItem is ErrorCard -> true
                oldItem is ErrorWithinCard && newItem is ErrorWithinCard -> true
                oldItem is TodaysStatsCardWithData && newItem is TodaysStatsCardWithData -> true
                oldItem is PostCardWithPostItems && newItem is PostCardWithPostItems -> true
                oldItem is BloggingPromptCardWithData && newItem is BloggingPromptCardWithData -> true
                oldItem is PromoteWithBlazeCard && newItem is PromoteWithBlazeCard -> true
                oldItem is BlazeCampaignsCardModel && newItem is BlazeCampaignsCardModel -> true
                oldItem is DomainTransferCardModel && newItem is DomainTransferCardModel -> true
                oldItem is DashboardPlansCard && newItem is DashboardPlansCard -> true
                oldItem is PagesCard && newItem is PagesCard -> true
                oldItem is ActivityCard && newItem is ActivityCard -> true
                else -> throw UnsupportedOperationException("Diff not implemented yet")
            }
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
    }
}
