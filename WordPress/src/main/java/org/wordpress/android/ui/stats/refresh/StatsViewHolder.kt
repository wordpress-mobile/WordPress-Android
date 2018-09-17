package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlin.reflect.KClass

abstract class StatsViewHolder(
    private val viewModelProvider: ViewModelProvider,
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    fun <D: ViewModel> viewModel(viewModelClass: KClass<D>): D {
        return viewModelProvider.get(viewModelClass.java)
    }
}
