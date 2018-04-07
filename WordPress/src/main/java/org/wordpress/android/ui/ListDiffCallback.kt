package org.wordpress.android.ui

import android.support.v7.util.DiffUtil
import org.wordpress.android.models.networkresource.ListNetworkResource

class ListDiffCallback<T>(private val oldList: List<T>?,
                          private val newList: List<T>?,
                          private val areItemsTheSame: (T?, T?) -> Boolean,
                          private val areContentsTheSame: (T?, T?) -> Boolean) : DiffUtil.Callback() {
    companion object {
        fun <T> fromListNetworkResource(listNetworkResource: ListNetworkResource<T>,
                                        areItemsTheSame: (T?, T?) -> Boolean,
                                        areContentsTheSame: (T?, T?) -> Boolean): ListDiffCallback<T> {
            return ListDiffCallback(listNetworkResource.previous?.data, listNetworkResource.data,
                    areItemsTheSame, areContentsTheSame)
        }
    }

    override fun getOldListSize(): Int = oldList?.size ?: 0

    override fun getNewListSize(): Int = newList?.size ?: 0

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
            = areItemsTheSame(oldList?.get(oldItemPosition), newList?.get(newItemPosition))

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
            = areContentsTheSame(oldList?.get(oldItemPosition), newList?.get(newItemPosition))
}
