package org.wordpress.android.ui.sitecreation.theme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R

class HomePagePickerViewHolder(internal val parent: ViewGroup) :
        RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        R.layout.home_page_picker_item,
                        parent,
                        false
                )
        )
