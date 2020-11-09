package org.wordpress.android.ui.sitecreation.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.R
import org.wordpress.android.util.image.ImageManager

class HomePagePickerAdapter(val imageManager: ImageManager, val thumbDimensionProvider: ThumbDimensionProvider) :
        Adapter<HomePagePickerViewHolder>() {
    private var layouts: List<LayoutGridItemUiState> = listOf()

    fun setData(data: List<LayoutGridItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(LayoutsDiffCallback(layouts, data))
        layouts = data
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomePagePickerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_page_picker_item, parent, false)
        val height = thumbDimensionProvider.height
        val width = thumbDimensionProvider.width
        val image = view.findViewById<View>(R.id.preview)
        image.minimumHeight = height
        image.minimumWidth = width
        view.minimumHeight = height
        view.minimumWidth = width
        return HomePagePickerViewHolder(view, parent)
    }

    override fun onBindViewHolder(holder: HomePagePickerViewHolder, position: Int) {
        holder.onBind(layouts[position], imageManager)
    }

    override fun getItemCount(): Int = layouts.size
}
