package org.wordpress.android.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import org.wordpress.android.R
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewMode

class PreviewModeMenuAdapter(context: Context, private val selectedPreviewMode: PreviewMode) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val menuItems = arrayOf(PreviewMode.DEFAULT, PreviewMode.DESKTOP)

    override fun getCount(): Int {
        return menuItems.size
    }

    override fun getItem(position: Int): PreviewMode {
        return menuItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var view = convertView
        val holder: PreviewModeMenuHolder
        if (view == null) {
            view = inflater.inflate(R.layout.preview_mode_popup_menu_item, parent, false)
            holder = PreviewModeMenuHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as PreviewModeMenuHolder
        }

        val previewMode = menuItems[position]
        val labelResId = when (previewMode) {
            PreviewMode.DEFAULT -> R.string.web_preview_default
            PreviewMode.DESKTOP -> R.string.web_preview_desktop
        }

        holder.label.setText(labelResId)

        if (previewMode === selectedPreviewMode) {
            holder.checkmark.visibility = View.VISIBLE
        } else {
            holder.checkmark.visibility = View.GONE
        }

        return view
    }

    internal inner class PreviewModeMenuHolder(view: View) {
        val label: TextView = view.findViewById(R.id.preview_mode_label)
        val checkmark: ImageView = view.findViewById(R.id.checkmark)
    }
}
