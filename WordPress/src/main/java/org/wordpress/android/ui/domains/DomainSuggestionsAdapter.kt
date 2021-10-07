package org.wordpress.android.ui.domains

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter

class DomainSuggestionsAdapter(
    private val itemSelectionListener: (DomainSuggestionItem?) -> Unit
) : Adapter<DomainSuggestionsViewHolder>() {
    private val list = mutableListOf<DomainSuggestionItem>()
    var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            DomainSuggestionsViewHolder(parent, itemSelectionListener)

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: DomainSuggestionsViewHolder, position: Int) {
        holder.bind(list[position])
    }

    internal fun updateSuggestionsList(items: List<DomainSuggestionItem>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }
}
