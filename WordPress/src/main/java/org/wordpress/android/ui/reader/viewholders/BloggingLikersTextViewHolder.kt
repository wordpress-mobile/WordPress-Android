package org.wordpress.android.ui.reader.viewholders

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.ui.engagement.EngagedPeopleViewHolder
import org.wordpress.android.ui.reader.adapters.FACE_ITEM_AVATAR_SIZE_DIMEN
import org.wordpress.android.ui.reader.adapters.FACE_ITEM_LEFT_OFFSET_DIMEN
import org.wordpress.android.ui.reader.adapters.TrainOfFacesItem.BloggersLikingTextItem
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.getColorFromAttribute

class BloggingLikersTextViewHolder(
    parent: ViewGroup,
    private val context: Context
) : EngagedPeopleViewHolder(parent, R.layout.blogger_likers_text_item) {
    private val bloggersText = itemView.findViewById<TextView>(R.id.num_bloggers)

    fun bind(bloggersTextItem: BloggersLikingTextItem) {
        val position = adapterPosition

        if (position >= 0) {
            val displayWidth = DisplayUtils.getDisplayPixelWidth(context)
            val paddingWidth = 2 * context.resources.getDimensionPixelSize(R.dimen.reader_detail_margin)
            val avatarSize = context.resources.getDimensionPixelSize(FACE_ITEM_AVATAR_SIZE_DIMEN)
            val leftOffest = context.resources.getDimensionPixelSize(FACE_ITEM_LEFT_OFFSET_DIMEN)
            val facesWidth = position * avatarSize - (position - 1).coerceAtLeast(0) * leftOffest

            itemView.layoutParams.width = displayWidth - paddingWidth - facesWidth
        }

        bloggersText.text = with(bloggersTextItem) {
            SpannableString(text).formatWithSpan(itemView.context, text, closure)
        }
    }

    private fun SpannableString.formatWithSpan(context: Context, text: String, closure: String): Spannable {
        val start = 0
        val end = text.lastIndexOf(closure) - 1
        return if (end <= start || end >= text.length - 1) {
            this
        } else {
            this.apply {
                setSpan(
                        ForegroundColorSpan(context.getColorFromAttribute(attr.colorPrimary)),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                        UnderlineSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}
