package org.wordpress.android.ui.sitecreation.theme

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter

class HomePagePickerAdapter : Adapter<HomePagePickerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HomePagePickerViewHolder(parent)

    override fun onBindViewHolder(holder: HomePagePickerViewHolder, position: Int) {}

    override fun getItemCount(): Int = 10 // Demo
}
