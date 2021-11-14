package org.wordpress.android.ui.prefs.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.SiteSettingsCategoriesRowBinding
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.utils.UiHelpers

class SiteSettingsCategoriesAdapter(private val uiHelpers: UiHelpers) :
        RecyclerView.Adapter<SiteSettingsCategoriesViewHolder>() {
    private val items = mutableListOf<CategoryNode>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SiteSettingsCategoriesViewHolder {
        val binding = SiteSettingsCategoriesRowBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        return SiteSettingsCategoriesViewHolder(binding, uiHelpers)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: SiteSettingsCategoriesViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    @MainThread
    fun update(newItems: List<CategoryNode>) {
        val diffResult = DiffUtil.calculateDiff(
                SiteSettingCategoriesDiffUtils(
                        items.toList(),
                        newItems
                )
        )
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SiteSettingCategoriesDiffUtils(
        val oldItems: List<CategoryNode>,
        val newItems: List<CategoryNode>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].categoryId == newItems[newItemPosition].categoryId
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
