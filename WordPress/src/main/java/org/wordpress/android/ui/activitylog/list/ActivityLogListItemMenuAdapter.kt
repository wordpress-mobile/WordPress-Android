package org.wordpress.android.ui.activitylog.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import org.wordpress.android.R
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.SecondaryAction
import org.wordpress.android.util.ColorUtils.setImageResourceWithTint
import org.wordpress.android.util.extensions.getColorResIdFromAttribute

class ActivityLogListItemMenuAdapter(
    context: Context,
    isRestoreHidden: Boolean
) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val items: List<SecondaryAction> = SecondaryAction.values()
            .toList()
            .filter { !(it == SecondaryAction.RESTORE && isRestoreHidden) }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return items[position].itemId
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ActivityListItemMenuHolder
        val view: View
        if (convertView == null) {
            view = inflater.inflate(R.layout.activity_log_list_item_menu_item, parent, false)
            holder = ActivityListItemMenuHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ActivityListItemMenuHolder
        }

        val textRes: Int
        val iconRes: Int
        val colorRes = view.context.getColorResIdFromAttribute(R.attr.wpColorOnSurfaceMedium)
        when (items[position]) {
            SecondaryAction.RESTORE -> {
                textRes = R.string.activity_log_item_menu_restore_label
                iconRes = R.drawable.ic_history_white_24dp
            }
            SecondaryAction.DOWNLOAD_BACKUP -> {
                textRes = R.string.activity_log_item_menu_download_backup_label
                iconRes = R.drawable.ic_get_app_white_24dp
            }
        }
        holder.text.setText(textRes)
        holder.text.setTextColor(
                AppCompatResources.getColorStateList(
                        view.context,
                        colorRes
                )
        )
        setImageResourceWithTint(
                holder.icon,
                iconRes,
                colorRes
        )
        return view
    }

    internal inner class ActivityListItemMenuHolder(view: View) {
        val text: TextView = view.findViewById(R.id.text)
        val icon: ImageView = view.findViewById(R.id.image)
    }
}
