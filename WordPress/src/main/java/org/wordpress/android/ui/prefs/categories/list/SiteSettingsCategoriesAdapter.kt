package org.wordpress.android.ui.prefs.categories.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.databinding.SiteSettingsCategoriesRowBinding
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.utils.UiHelpers

class SiteSettingsCategoriesAdapter(
    private val uiHelpers: UiHelpers,
    private val onClickListener: (CategoryNode) -> Unit
) : ListAdapter<CategoryNode, SiteSettingsCategoriesViewHolder>(SiteSettingsCategoriesDiffCallback) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SiteSettingsCategoriesViewHolder {
        val binding = SiteSettingsCategoriesRowBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        return SiteSettingsCategoriesViewHolder(binding, uiHelpers, onClickListener)
    }

    override fun onBindViewHolder(holder: SiteSettingsCategoriesViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    override fun onBindViewHolder(holder: SiteSettingsCategoriesViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && ((payloads[0] as? Bundle)?.size() ?: 0) > 0) {
            val bundle = payloads[0] as Bundle
            holder.updateChanges(bundle)
        } else onBindViewHolder(holder, position)
    }
}
