package org.wordpress.android.ui.prefs.categories

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
    val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(categoryBinding.root) {
    fun onBind(categoryNode: CategoryNode) = with(categoryBinding) {
        siteSettingsCategoryRowLayout.setOnClickListener {
            // todo : ajeshr
        }
        siteSettingsCategoryText.text = StringEscapeUtils.unescapeHtml4(categoryNode.name)
        val verticalPadding: Int = uiHelpers.getPxOfUiDimen(
                siteSettingsCategoryText.context,
                UIDimenRes(R.dimen.margin_large)
        )
        val horizontalPadding: Int = uiHelpers.getPxOfUiDimen(
                siteSettingsCategoryText.context,
                UIDimenRes(R.dimen.margin_extra_large)
        )
        ViewCompat.setPaddingRelative(
                siteSettingsCategoryText,
                horizontalPadding * categoryNode.level,
                verticalPadding,
                horizontalPadding,
                verticalPadding
        )
    }
}

