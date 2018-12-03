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
    private val itemClickListener: (HistoryListItem) -> Unit,
    val imageManager: ImageManager
) : HistoryViewHolder(parent, R.layout.history_list_item) {
    private val container: View = itemView.findViewById(R.id.item_layout)
    private val avatar: ImageView = itemView.findViewById(R.id.item_avatar)
    private val title: TextView = itemView.findViewById(R.id.item_title)
    private val subtitle: TextView = itemView.findViewById(R.id.item_subtitle)
    private val diffLayout: LinearLayout = itemView.findViewById(R.id.diff_layout)
    private val diffAdditions: TextView = itemView.findViewById(R.id.diff_additions)
    private val diffDeletions: TextView = itemView.findViewById(R.id.diff_deletions)
    private lateinit var boundRevision: HistoryListItem.Revision

    fun bind(revision: HistoryListItem.Revision) {
        boundRevision = revision

        container.setOnClickListener { itemClickListener(boundRevision) }
        title.text = boundRevision.formattedTime
        subtitle.text = boundRevision.authorDisplayName

        if (!TextUtils.isEmpty(this.boundRevision.authorAvatarURL)) {
            imageManager.loadIntoCircle(avatar, ImageType.AVATAR_WITH_BACKGROUND,
                    StringUtils.notNullStr(boundRevision.authorAvatarURL))
        }

        if (boundRevision.totalAdditions == 0 && boundRevision.totalDeletions == 0) {
            diffLayout.visibility = View.GONE
        } else {
            diffLayout.visibility = View.VISIBLE

            if (boundRevision.totalAdditions > 0) {
                diffAdditions.text = boundRevision.totalAdditions.toString()
                diffAdditions.visibility = View.VISIBLE
            } else {
                diffAdditions.visibility = View.GONE
            }

            if (boundRevision.totalDeletions > 0) {
                diffDeletions.text = boundRevision.totalDeletions.toString()
                diffDeletions.visibility = View.VISIBLE
            } else {
                diffDeletions.visibility = View.GONE
            }
        }
    }

    override fun updateChanges(bundle: Bundle) {
        super.updateChanges(bundle)
        if (bundle.containsKey(HistoryDiffCallback.AVATAR_CHANGED_KEY)) {
            val avatarUrl = bundle.getString(HistoryDiffCallback.AVATAR_CHANGED_KEY)
            boundRevision.authorAvatarURL = avatarUrl
            if (!TextUtils.isEmpty(avatarUrl)) {
                imageManager.loadIntoCircle(avatar, ImageType.AVATAR, StringUtils.notNullStr(avatarUrl))
            }
        }
        if (bundle.containsKey(HistoryDiffCallback.DISPLAY_NAME_CHANGED_KEY)) {
            val authorDisplayName = bundle.getString(HistoryDiffCallback.DISPLAY_NAME_CHANGED_KEY)
            boundRevision.authorDisplayName = authorDisplayName
            subtitle.text = authorDisplayName
        }
    }
}
