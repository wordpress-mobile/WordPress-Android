package org.wordpress.android.ui.prefs.categories.list

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.databinding.SiteSettingsCategoriesRowBinding
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiHelpers

class SiteSettingsCategoriesViewHolder(
    private val categoryBinding: SiteSettingsCategoriesRowBinding,
    val uiHelpers: UiHelpers,
    private val onClickListener: (CategoryNode) -> Unit
) : RecyclerView.ViewHolder(categoryBinding.root) {
    private val verticalPadding = uiHelpers.getPxOfUiDimen(itemView.context, UIDimenRes(R.dimen.margin_large))
    private val horizontalPadding = uiHelpers.getPxOfUiDimen(itemView.context, UIDimenRes(R.dimen.margin_extra_large))

    fun onBind(categoryNode: CategoryNode) = with(categoryBinding) {
        siteSettingsCategoryRowLayout.setOnClickListener { onClickListener.invoke(categoryNode) }
        setPaddingForCategoryName(categoryNode.level)
        siteSettingsCategoryText.text = StringEscapeUtils.unescapeHtml4(categoryNode.name)
    }

    private fun setPaddingForCategoryName(categoryLevel: Int) {
        ViewCompat.setPaddingRelative(
                categoryBinding.siteSettingsCategoryText,
                horizontalPadding * categoryLevel,
                verticalPadding,
                horizontalPadding,
                verticalPadding
        )
    }

    fun updateChanges(bundle: Bundle) {
        if (bundle.containsKey(SiteSettingsCategoriesDiffCallback.NAME_CHANGED_KEY)) {
            val categoryName = bundle.getString(SiteSettingsCategoriesDiffCallback.NAME_CHANGED_KEY)
            categoryBinding.siteSettingsCategoryText.text = StringEscapeUtils.unescapeHtml4(categoryName)
        }
        if (bundle.containsKey(SiteSettingsCategoriesDiffCallback.LEVEL_CHANGED_KEY)) {
            val newLevel = bundle.getInt(SiteSettingsCategoriesDiffCallback.LEVEL_CHANGED_KEY)
            setPaddingForCategoryName(newLevel)
        }
    }
}
