package org.wordpress.android.ui.mysite

import androidx.recyclerview.widget.DiffUtil
import org.apache.commons.lang3.NotImplementedException
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem

@Suppress("TooManyFunctions")
class MySiteAdapterDiffCallback(
    private val oldCardAndItems: List<MySiteCardAndItem>,
    private val updatedCardAndItems: List<MySiteCardAndItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldCardAndItems.size

    override fun getNewListSize(): Int = updatedCardAndItems.size

    @Suppress("ComplexMethod")
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldCardAndItems[oldItemPosition]
        val updatedItem = updatedCardAndItems[newItemPosition]
        return oldItem.type == updatedItem.type && when {
            oldItem is SiteInfoCard && updatedItem is SiteInfoCard -> true
            oldItem is QuickActionsCard && updatedItem is QuickActionsCard -> true
            oldItem is DomainRegistrationCard && updatedItem is DomainRegistrationCard -> true
            oldItem is QuickStartCard && updatedItem is QuickStartCard -> true
            oldItem is QuickStartDynamicCard && updatedItem is QuickStartDynamicCard -> oldItem.id == updatedItem.id
            oldItem is CategoryHeaderItem && updatedItem is CategoryHeaderItem -> oldItem.title == updatedItem.title
            oldItem is ListItem && updatedItem is ListItem -> oldItem.primaryText == updatedItem.primaryText
            oldItem is PostCard && updatedItem is PostCard -> true
            else -> throw NotImplementedException("Diff not implemented yet")
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldCardAndItems[oldItemPosition]
        val newItem = updatedCardAndItems[newItemPosition]
        return when {
            oldItem is QuickActionsCard && newItem is QuickActionsCard -> areContentsTheSame(oldItem, newItem)
            oldItem is QuickStartCard && newItem is QuickStartCard -> areContentsTheSame(oldItem, newItem)
            oldItem is PostCardWithPostItems && newItem is PostCardWithPostItems -> areContentsTheSame(oldItem, newItem)
            oldItem is PostCardWithoutPostItems && newItem is PostCardWithoutPostItems -> areContentsTheSame(
                    oldItem,
                    newItem
            )
            else -> oldCardAndItems[oldItemPosition] == updatedCardAndItems[newItemPosition]
        }
    }

    // QuickActionsCard
    private fun areContentsTheSame(oldItem: QuickActionsCard, newItem: QuickActionsCard) =
            oldItem.title == newItem.title &&
            oldItem.showPages == newItem.showPages &&
            oldItem.showPagesFocusPoint == newItem.showPagesFocusPoint &&
            oldItem.showStatsFocusPoint == newItem.showStatsFocusPoint

    // QuickStartCard
    private fun areContentsTheSame(oldItem: QuickStartCard, newItem: QuickStartCard) =
            oldItem.moreMenuVisible == newItem.moreMenuVisible &&
            oldItem.title == newItem.title &&
            oldItem.activeQuickStartItem == newItem.activeQuickStartItem &&
            areQuickStartTaskTypeItemsContentsTheSame(oldItem.taskTypeItems, newItem.taskTypeItems)

    // QuickStartTaskTypeItem (List)
    private fun areQuickStartTaskTypeItemsContentsTheSame(
        oldList: List<QuickStartTaskTypeItem>,
        newList: List<QuickStartTaskTypeItem>
    ): Boolean {
        oldList.forEachIndexed { index, quickStartTaskTypeItem ->
            if (!areContentsTheSame(quickStartTaskTypeItem, newList[index])) return false
        }
        return true
    }

    // QuickStartTaskTypeItem
    private fun areContentsTheSame(oldItem: QuickStartTaskTypeItem, newItem: QuickStartTaskTypeItem) =
            oldItem.title == newItem.title &&
                    oldItem.subtitle == newItem.subtitle &&
                    oldItem.quickStartTaskType == newItem.quickStartTaskType &&
                    oldItem.titleEnabled == newItem.titleEnabled &&
                    oldItem.strikeThroughTitle == newItem.strikeThroughTitle

    // PostCardWithPostItems
    private fun areContentsTheSame(oldItem: PostCardWithPostItems, newItem: PostCardWithPostItems) =
            oldItem.postCardType == newItem.postCardType &&
            oldItem.title == newItem.title &&
            arePostItemsContentsTheSame(oldItem.postItems, newItem.postItems)

    // PostItem (List)
    private fun arePostItemsContentsTheSame(oldList: List<PostItem>, newList: List<PostItem>): Boolean {
        oldList.forEachIndexed { index, postItem ->
            if (!areContentsTheSame(postItem, newList[index])) return false
        }
        return true
    }

    // PostItem
    private fun areContentsTheSame(oldItem: PostItem, newItem: PostItem) =
            oldItem.title == newItem.title &&
            oldItem.excerpt == newItem.excerpt &&
            oldItem.featuredImageUrl == newItem.featuredImageUrl &&
            oldItem.isTimeIconVisible == newItem.isTimeIconVisible

    private fun areContentsTheSame(oldItem: PostCardWithoutPostItems, newItem: PostCardWithoutPostItems) =
            oldItem.postCardType == newItem.postCardType &&
                    oldItem.title == newItem.title &&
                    oldItem.excerpt == newItem.excerpt
}
