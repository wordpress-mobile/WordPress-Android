package org.wordpress.android.ui.mysite

import androidx.recyclerview.widget.DiffUtil
import org.apache.commons.lang3.NotImplementedException
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock
import org.wordpress.android.ui.mysite.MySiteItem.DynamicCard.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.mysite.MySiteItem.QuickStartBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock

class MySiteAdapterDiffCallback(
    private val oldItems: List<MySiteItem>,
    private val updatedItems: List<MySiteItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = updatedItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val updatedItem = updatedItems[newItemPosition]
        return oldItem.type == updatedItem.type && when {
            oldItem is SiteInfoBlock && updatedItem is SiteInfoBlock -> true
            oldItem is QuickActionsBlock && updatedItem is QuickActionsBlock -> true
            oldItem is DomainRegistrationBlock && updatedItem is DomainRegistrationBlock -> true
            oldItem is QuickStartBlock && updatedItem is QuickStartBlock -> true
            oldItem is QuickStartCard && updatedItem is QuickStartCard -> oldItem.id == updatedItem.id
            oldItem is CategoryHeader && updatedItem is CategoryHeader -> oldItem.title == updatedItem.title
            oldItem is ListItem && updatedItem is ListItem -> oldItem.primaryText == updatedItem.primaryText
            else -> throw NotImplementedException("Diff not implemented yet")
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == updatedItems[newItemPosition]
    }
}
