package org.wordpress.android.ui.prefs.categories.list

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.models.CategoryNode

object SiteSettingsCategoriesDiffCallback : DiffUtil.ItemCallback<CategoryNode>() {
    const val LEVEL_CHANGED_KEY = "category_level_changed"
    const val NAME_CHANGED_KEY = "category_name_changed"

    override fun areItemsTheSame(oldItem: CategoryNode, newItem: CategoryNode): Boolean {
        return oldItem.categoryId == newItem.categoryId
    }

    override fun areContentsTheSame(oldItem: CategoryNode, newItem: CategoryNode): Boolean {
        return oldItem.level == newItem.level &&
                oldItem.name == newItem.name
    }

    override fun getChangePayload(oldItem: CategoryNode, newItem: CategoryNode): Any? {
        val changesBundle = Bundle()
        if (oldItem.level != newItem.level) {
            changesBundle.putInt(LEVEL_CHANGED_KEY, newItem.level)
        }
        if (oldItem.name != newItem.name) {
            changesBundle.putString(NAME_CHANGED_KEY, newItem.name)
        }
        if (changesBundle.keySet().size > 0) {
            return changesBundle
        }
        return super.getChangePayload(oldItem, newItem)
    }
}
