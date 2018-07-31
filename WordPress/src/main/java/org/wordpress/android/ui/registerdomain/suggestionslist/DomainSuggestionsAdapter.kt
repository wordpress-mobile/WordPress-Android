package org.wordpress.android.ui.registerdomain.suggestionslist

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.ui.activitylog.list.ActivityLogDiffCallback
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem

class DomainSuggestionsAdapter(
    private val itemSelectionListener: (DomainSuggestionResponse) -> Unit
) : Adapter<DomainSuggestionsViewHolder>() {
    private val list = mutableListOf<DomainSuggestionResponse>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DomainSuggestionsViewHolder {
        return DomainSuggestionsViewHolder(parent, itemSelectionListener)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: DomainSuggestionsViewHolder, position: Int) {
        holder.bind(list[position])
    }

    internal fun updateSuggestionsList(items: List<DomainSuggestionResponse>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }
}


