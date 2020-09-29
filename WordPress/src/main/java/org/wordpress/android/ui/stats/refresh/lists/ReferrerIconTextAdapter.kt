package org.wordpress.android.ui.stats.refresh.lists

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.wordpress.android.R

class ReferrerIconTextAdapter(private val context: Activity, private val text: Array<String>, private val imgid: Array<Int>)
    : ArrayAdapter<String>(context, R.layout.mark_referrer_as_spam_dialog_item, text) {
    // Not necessary to use a recycler because it's only two items
    @SuppressLint("ViewHolder")
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.mark_referrer_as_spam_dialog_item, null, true)

        val titleText = rowView.findViewById(R.id.text) as TextView
        val imageView = rowView.findViewById(R.id.icon) as ImageView

        titleText.text = text[position]
        imageView.setImageResource(imgid[position])

        return rowView
    }
}
