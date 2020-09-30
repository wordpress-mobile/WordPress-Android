package org.wordpress.android.ui.stats.refresh.lists.sections.insights

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.ReferrerMenuAdapter.ReferrerMenuItem.MARK_AS_SPAM
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.ReferrerMenuAdapter.ReferrerMenuItem.OPEN_WEBSITE

class ReferrerMenuAdapter(context: Context) : BaseAdapter() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val items = listOfNotNull(OPEN_WEBSITE, MARK_AS_SPAM)

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
            OPEN_WEBSITE -> {
                textRes = R.string.open_website
                iconRes = R.drawable.ic_external_grey_min_24dp
            }
            MARK_AS_SPAM -> {
                textRes = R.string.mark_as_spam
                iconRes = R.drawable.ic_spam_red_24dp
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

    enum class ReferrerMenuItem(val id: Long) {
        OPEN_WEBSITE(0), MARK_AS_SPAM(1)
    }
}
