package org.wordpress.android.ui.activitylog.list

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers

class NoticeItemViewHolder(parent: ViewGroup) :
    ActivityLogViewHolder(parent, R.layout.activity_log_list_notice_item) {
    private val label: TextView = itemView.findViewById(R.id.label)
    private val primaryButton: TextView = itemView.findViewById(R.id.primary_button)
    private val secondaryButton: TextView = itemView.findViewById(R.id.secondary_button)

    fun bind(item: ActivityLogListItem.Notice, uiHelpers: UiHelpers) {
        uiHelpers.setTextOrHide(label, item.label)
        primaryButton.setOnClickListener { item.primaryAction() }
        secondaryButton.setOnClickListener { item.secondaryAction() }
    }
}
