package org.wordpress.android.ui.quickstart

import android.support.v7.util.DiffUtil
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

class QuickStartTasksDiffCallback(
    private val oldList: List<QuickStartTask>,
    private val newList: List<QuickStartTask>
) : DiffUtil.Callback() {
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }
}
