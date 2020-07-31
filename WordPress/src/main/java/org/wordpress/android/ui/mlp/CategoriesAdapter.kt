package org.wordpress.android.ui.mlp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.modal_layout_picker_category.view.*
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.login.util.getColorStateListFromAttribute
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.setVisible

/**
 * Renders the Layout categories tab bar
 */
class CategoriesAdapter(
    private val context: Context,
    private val categories: List<CategoryListItem>,
    private val selectionListener: LayoutSelectionListener
) : RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.modal_layout_picker_category, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val category = categories[position]
        holder.category.text = category.title
        holder.emoji.text = category.emoji
        holder.container.contentDescription = if (category.selected) context.getString(
                R.string.mlp_layout_selected,
                category.title
        ) else category.title
        holder.container.setOnClickListener {
            selectionListener.categoryTapped(category)
        }
        holder.container.backgroundTintList
        selectionListener.selectedCategoryData.observe(selectionListener.lifecycleOwner, Observer {
            setSelectedStateUI(holder, it == category.slug)
        })
    }

    private fun setSelectedStateUI(holder: ViewHolder, selected: Boolean) {
        holder.check.setVisible(selected)
        holder.emoji.setVisible(!selected)
        holder.container.backgroundTintList = context.getColorStateListFromAttribute(
                if (selected) attr.categoriesButtonBackgroundSelected else attr.categoriesButtonBackground
        )
        holder.category.setTextColor(
                context.getColorFromAttribute(
                        if (selected) attr.categoriesButtonTextSelected else attr.categoriesButtonText
                )
        )
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.category_container
        val category: TextView = itemView.category
        val emoji: TextView = itemView.emoji
        val check: ImageView = itemView.check
    }
}
