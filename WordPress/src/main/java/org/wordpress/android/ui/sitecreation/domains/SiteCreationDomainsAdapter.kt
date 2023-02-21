package org.wordpress.android.ui.sitecreation.domains

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainViewHolder.DomainComposeItemViewHolder
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainViewHolder.DomainSuggestionErrorViewHolder
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainViewHolder.DomainSuggestionItemViewHolder
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Old
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Type
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Type.DOMAIN_V1
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Type.DOMAIN_V2
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Type.ERROR_FETCH_V1
import org.wordpress.android.ui.utils.UiHelpers

class SiteCreationDomainsAdapter(
    private val uiHelpers: UiHelpers,
) : Adapter<SiteCreationDomainViewHolder<*>>() {
    private val items = mutableListOf<ListItemUiState>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SiteCreationDomainViewHolder<*> {
        return when (Type.values()[viewType]) {
            DOMAIN_V1 -> DomainSuggestionItemViewHolder(parent, uiHelpers)
            DOMAIN_V2 -> DomainComposeItemViewHolder(parent)
            ERROR_FETCH_V1 -> DomainSuggestionErrorViewHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: SiteCreationDomainViewHolder<*>, position: Int) {
        val item = items[position]
        return when (holder) {
            is DomainSuggestionItemViewHolder -> holder.onBind(item as Old.DomainUiState)
            is DomainSuggestionErrorViewHolder -> holder.onBind(item as Old.ErrorItemUiState)
            is DomainComposeItemViewHolder -> holder.onBind(item as New.DomainUiState)
        }
    }

    override fun getItemViewType(position: Int) = items[position].type.ordinal

    override fun getItemCount(): Int = items.size

    @MainThread
    fun update(newItems: List<ListItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(DomainsDiffUtils(items.toList(), newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    @Suppress("ForbiddenComment")
    override fun onViewRecycled(holder: SiteCreationDomainViewHolder<*>) {
        if (holder is DomainComposeItemViewHolder) {
            // TODO: Remove this for Compose 1.2.0-beta02+ and RecyclerView 1.3.0-alpha02+
            holder.composeView.disposeComposition()
        }
        super.onViewRecycled(holder)
    }

    private class DomainsDiffUtils(
        val oldItems: List<ListItemUiState>,
        val newItems: List<ListItemUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return when (oldItem) {
                is Old.ErrorItemUiState -> true
                is Old.DomainUiState -> oldItem.name == (newItem as Old.DomainUiState).name
                is New.DomainUiState -> oldItem.domainName == (newItem as New.DomainUiState).domainName
            }
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
