package org.wordpress.android.ui.history

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

class RevisionItemViewHolder(
    parent: ViewGroup,
    private val itemClickListener: (HistoryListItem) -> Unit, val imageManager: ImageManager
) : HistoryViewHolder(parent, R.layout.history_list_item) {
    private val container: View = itemView.findViewById(R.id.item_layout)
    private val avatar: ImageView = itemView.findViewById(R.id.item_avatar)
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

        if (!TextUtils.isEmpty(revision.authorAvatarURL)) {
            imageManager.loadIntoCircle(avatar, ImageType.AVATAR, StringUtils.notNullStr(revision.authorAvatarURL))
        }

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

    override fun updateChanges(bundle: Bundle) {
        if (bundle.containsKey(HistoryDiffCallback.AVATAR_CHANGED_KEY)) {
            val avatarUrl = bundle.getString(HistoryDiffCallback.AVATAR_CHANGED_KEY)
            if (!TextUtils.isEmpty(avatarUrl)) {
                imageManager.loadIntoCircle(avatar, ImageType.AVATAR, StringUtils.notNullStr(avatarUrl))
            }
        }

        super.updateChanges(bundle)
    }
}
