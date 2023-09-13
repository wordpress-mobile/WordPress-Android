package org.wordpress.android.ui.mysite

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackFeatureCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackSwitchMenu
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PersonalizeCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryEmptyHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.SingleActionCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge

@Suppress("ComplexMethod")
object MySiteAdapterDiffCallback : DiffUtil.ItemCallback<MySiteCardAndItem>() {
    override fun areItemsTheSame(oldItem: MySiteCardAndItem, updatedItem: MySiteCardAndItem): Boolean {
        return oldItem.type == updatedItem.type && when {
            oldItem is QuickLinkRibbon && updatedItem is QuickLinkRibbon -> true
            oldItem is DomainRegistrationCard && updatedItem is DomainRegistrationCard -> true
            oldItem is QuickStartCard && updatedItem is QuickStartCard -> true
            oldItem is InfoItem && updatedItem is InfoItem -> oldItem.title == updatedItem.title
            oldItem is CategoryHeaderItem && updatedItem is CategoryHeaderItem -> oldItem.title == updatedItem.title
            oldItem is CategoryEmptyHeaderItem
                    && updatedItem is CategoryEmptyHeaderItem -> oldItem.title == updatedItem.title
            oldItem is ListItem && updatedItem is ListItem -> oldItem.primaryText == updatedItem.primaryText
            oldItem is DashboardCards && updatedItem is DashboardCards -> true
            oldItem is JetpackBadge && updatedItem is JetpackBadge -> true
            oldItem is SingleActionCard && updatedItem is SingleActionCard -> {
                oldItem.textResource == updatedItem.textResource
                        && oldItem.imageResource == updatedItem.imageResource
            }
            oldItem is JetpackFeatureCard && updatedItem is JetpackFeatureCard -> true
            oldItem is JetpackSwitchMenu && updatedItem is JetpackSwitchMenu -> true
            oldItem is JetpackInstallFullPluginCard && updatedItem is JetpackInstallFullPluginCard -> true
            oldItem is PersonalizeCardModel && updatedItem is PersonalizeCardModel -> true
            else -> throw UnsupportedOperationException("Diff not implemented yet")
        }
    }

    override fun areContentsTheSame(oldItem: MySiteCardAndItem, newItem: MySiteCardAndItem): Boolean {
        if (oldItem.activeQuickStartItem || newItem.activeQuickStartItem) return false
        return oldItem == newItem
    }
}
