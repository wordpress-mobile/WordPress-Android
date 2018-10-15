package org.wordpress.android.ui.history

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R

class RevisionItemViewHolder(
    parent: ViewGroup,
    private val itemClickListener: (HistoryListItem) -> Unit
) : HistoryViewHolder(parent, R.layout.history_list_item) {
    private val container: View = itemView.findViewById(R.id.item_layout)
    private val title: TextView = itemView.findViewById(R.id.item_title)
    private val subtitle: TextView = itemView.findViewById(R.id.item_subtitle)
    private val diffLayout: LinearLayout = itemView.findViewById(R.id.diff_layout)
    private val diffAdditions: TextView = itemView.findViewById(R.id.diff_additions)
    private val diffDeletions: TextView = itemView.findViewById(R.id.diff_deletions)

    fun bind(revision: HistoryListItem.Revision) {
        container.setOnClickListener { itemClickListener(revision) }
        title.text = revision.timeSpan
        // TODO: Replace date and time with post status or username.
        subtitle.text = TextUtils.concat(revision.formattedDate + " at " + revision.formattedTime)

        if (revision.totalAdditions == 0 && revision.totalDeletions == 0) {
            diffLayout.visibility = View.GONE
        } else {
            if (revision.totalAdditions > 0) {
                diffAdditions.text = revision.totalAdditions.toString()
                diffAdditions.visibility = View.VISIBLE
            } else {
                diffAdditions.visibility = View.GONE
            }

            if (revision.totalDeletions > 0) {
                diffDeletions.text = revision.totalDeletions.toString()
                diffDeletions.visibility = View.VISIBLE
            } else {
                diffDeletions.visibility = View.GONE
            }
        }
    }
}
