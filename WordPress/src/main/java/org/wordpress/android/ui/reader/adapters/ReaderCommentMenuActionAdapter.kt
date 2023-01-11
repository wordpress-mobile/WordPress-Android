package org.wordpress.android.ui.reader.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.reader.adapters.ReaderCommentMenuActionAdapter.ReaderCommentMenuActionType.DIVIDER_NO_ACTION
import org.wordpress.android.ui.reader.adapters.ReaderCommentMenuActionAdapter.ReaderCommentMenuItem.PrimaryItemMenu
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.getColorStateListFromAttribute

// Based on Reader card menu
class ReaderCommentMenuActionAdapter(
    context: Context?,
    val uiHelpers: UiHelpers,
    val menuItems: List<ReaderCommentMenuItem>
) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return menuItems.size
    }

    override fun getItem(position: Int): Any {
        return menuItems[position]
    }

    override fun getItemId(position: Int): Long {
        return menuItems[position].type.ordinal.toLong()
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (menuItems[position].type === DIVIDER_NO_ACTION) {
            TYPE_SPACER
        } else {
            TYPE_CONTENT
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val cardAction = menuItems[position]
        return if (getItemViewType(position) == TYPE_SPACER) {
            handleSpacer(convertView, parent)
        } else {
            handleAction(
                cardAction as PrimaryItemMenu,
                convertView,
                parent
            )
        }
    }

    private fun handleSpacer(convertView: View?, parent: ViewGroup): View {
        var innerConvertView = convertView
        val holder: ReaderCommentMenuSpacerHolder
        if (innerConvertView == null) {
            innerConvertView = inflater.inflate(layout.popup_menu_divider, parent, false)
            holder = ReaderCommentMenuSpacerHolder(innerConvertView)
            innerConvertView.tag = holder
        } else {
            holder = innerConvertView.tag as ReaderCommentMenuSpacerHolder
        }
        holder.spacer.visibility = View.VISIBLE
        holder.spacer.isEnabled = false
        holder.spacer.isClickable = false
        return innerConvertView!!
    }

    private fun handleAction(
        item: PrimaryItemMenu,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var innerConvertView = convertView
        val holder: ReaderCommentMenuHolder
        if (innerConvertView == null) {
            innerConvertView = inflater.inflate(layout.reader_popup_menu_item, parent, false)
            holder = ReaderCommentMenuHolder(innerConvertView)
            innerConvertView.tag = holder
        } else {
            holder = innerConvertView.tag as ReaderCommentMenuHolder
        }
        val textRes = uiHelpers.getTextOfUiString(innerConvertView!!.context, item.labelResId)
        val iconRes = item.iconRes
        holder.text.text = textRes
        holder.icon.setImageDrawable(ContextCompat.getDrawable(holder.icon.context, iconRes))
        holder.icon.imageTintList = holder.icon.context.getColorStateListFromAttribute(R.attr.wpColorOnSurfaceMedium)
        return innerConvertView
    }

    internal inner class ReaderCommentMenuHolder(view: View) {
        val text: TextView = view.findViewById(id.text)
        val icon: ImageView = view.findViewById(id.image)
    }

    internal inner class ReaderCommentMenuSpacerHolder(view: View) {
        val spacer: View = view.findViewById(id.divider)
    }

    sealed class ReaderCommentMenuItem {
        abstract val type: ReaderCommentMenuActionType

        data class PrimaryItemMenu(
            override val type: ReaderCommentMenuActionType,
            val labelResId: UiString,
            val contentDescription: UiString? = null,
            @DrawableRes val iconRes: Int
        ) : ReaderCommentMenuItem()

        data class Divider(
            override val type: ReaderCommentMenuActionType = DIVIDER_NO_ACTION
        ) : ReaderCommentMenuItem()
    }

    enum class ReaderCommentMenuActionType {
        APPROVE, UNAPPROVE, SPAM, TRASH, EDIT, SHARE, DIVIDER_NO_ACTION
    }

    companion object {
        private const val TYPE_SPACER = 0
        private const val TYPE_CONTENT = 1
    }
}
