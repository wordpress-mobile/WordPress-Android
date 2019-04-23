package org.wordpress.android.ui.stats.refresh.lists.sections.insights

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.DOWN
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.REMOVE
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsMenuAdapter.InsightsMenuItem.UP

class InsightsMenuAdapter(context: Context, isUpVisible: Boolean, isDownVisible: Boolean) : BaseAdapter() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val items = listOfNotNull(if (isUpVisible) UP else null, if (isDownVisible) DOWN else null, REMOVE)

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return items[position].id
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: MenuHolder
        if (view == null) {
            view = mInflater.inflate(R.layout.stats_insights_popup_menu_item, parent, false)
            holder = MenuHolder(view!!)
            view.tag = holder
        } else {
            holder = view.tag as MenuHolder
        }

        val textRes: Int
        val iconRes: Int
        when (items[position]) {
            UP -> {
                textRes = R.string.stats_menu_move_up
                iconRes = R.drawable.ic_arrow_up_grey_dark_24dp
            }
            DOWN -> {
                textRes = R.string.stats_menu_move_down
                iconRes = R.drawable.ic_arrow_down_grey_dark_24dp
            }
            REMOVE -> {
                textRes = R.string.stats_menu_remove
                iconRes = R.drawable.ic_trash_grey_dark_24dp
            }
        }

        holder.text.setText(textRes)
        holder.icon.setImageResource(iconRes)
        return view
    }

    internal inner class MenuHolder(view: View) {
        val text: TextView = view.findViewById(R.id.text)
        val icon: ImageView = view.findViewById(R.id.image)
    }

    enum class InsightsMenuItem(val id: Long) {
        UP(0), DOWN(1), REMOVE(2)
    }
}
