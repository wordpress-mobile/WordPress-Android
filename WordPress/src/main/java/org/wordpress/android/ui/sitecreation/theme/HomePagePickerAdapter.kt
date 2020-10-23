package org.wordpress.android.ui.sitecreation.theme

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.util.image.ImageManager

class HomePagePickerAdapter(val imageManager: ImageManager) : Adapter<HomePagePickerViewHolder>() {
    private var layouts: List<LayoutGridItemUiState> = listOf()

    fun setData(data: List<LayoutGridItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(LayoutsDiffCallback(layouts, data))
        layouts = data
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HomePagePickerViewHolder(parent)

    override fun onBindViewHolder(holder: HomePagePickerViewHolder, position: Int) {
        holder.onBind(layouts[position], imageManager)
    }

    override fun getItemCount(): Int = layouts.size
}
