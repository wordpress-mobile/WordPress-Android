package org.wordpress.android.ui.reader.subfilter.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SectionTitle
import org.wordpress.android.ui.utils.UiHelpers

class SectionTitleViewHolder(
    parent: ViewGroup
) : SubfilterListItemViewHolder(parent, R.layout.subfilter_section_title) {
    private val sectionTitle = itemView.findViewById<TextView>(R.id.section_title)

    fun bind(sectionTitle: SectionTitle, uiHelpers: UiHelpers) {
        super.bind(sectionTitle, uiHelpers)
        this.sectionTitle.text = uiHelpers.getTextOfUiString(parent.context, sectionTitle.label)
    }
}
