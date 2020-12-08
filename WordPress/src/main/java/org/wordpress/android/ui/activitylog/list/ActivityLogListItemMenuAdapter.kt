package org.wordpress.android.ui.activitylog.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.SecondaryAction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils.setImageResourceWithTint
import org.wordpress.android.util.getColorResIdFromAttribute
import java.util.ArrayList

class ActivityLogListItemMenuAdapter(
    context: Context?,
    uiHelpers: UiHelpers,
    menuItems: List<SecondaryAction?>
) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val menuItems: MutableList<SecondaryAction> = ArrayList()
    private val uiHelpers: UiHelpers
    override fun getCount(): Int {
        return menuItems.size
    }

    override fun getItem(position: Int): Any {
        return menuItems[position]
    }

    override fun getItemId(position: Int): Long {
        return menuItems[position].type.ordinal.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        val holder: ActivityListItemMenuHolder
        if (convertView == null) {
            convertView = inflater.inflate(layout.activity_log_list_item_menu_item, parent, false)
            holder = ActivityListItemMenuHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ActivityListItemMenuHolder
        }
        val (label, labelColor, iconRes, iconColor) = menuItems[position]
        val textRes = uiHelpers.getTextOfUiString(
                convertView.context,
                label
        )
        val textColorRes = convertView.context.getColorResIdFromAttribute(labelColor)
        val iconColorRes = convertView.context.getColorResIdFromAttribute(iconColor)
        holder.mText.text = textRes
        holder.mText.setTextColor(
                AppCompatResources.getColorStateList(
                        convertView.context,
                        textColorRes
                )
        )
        setImageResourceWithTint(
                holder.icon,
                iconRes,
                iconColorRes
        )
        return convertView
    }

    internal inner class ActivityListItemMenuHolder(view: View) {
        private val label: TextView
        private val icon: ImageView

        init {
            label = view.findViewById(id.text)
            icon = view.findViewById(id.image)
        }
    }

    init {
        this.menuItems.addAll(menuItems)
        this.uiHelpers = uiHelpers
    }
}