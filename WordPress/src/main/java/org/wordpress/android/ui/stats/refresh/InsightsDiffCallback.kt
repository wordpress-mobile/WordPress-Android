package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil.Callback
import android.util.Log

class InsightsDiffCallback(
    private val oldList: List<InsightsItem>,
    private val newList: List<InsightsItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return when {
            oldItem.insightsType != null && newItem.insightsType != null -> oldItem.insightsType == newItem.insightsType
            else -> oldItem.type == newItem.type
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        val result = oldList[oldItemPosition] == newList[newItemPosition]
        if (!result) {
            Log.d("list_insights_diff", "C: Old item: ${oldItem.insightsType} - type: ${oldItem.type}")
            Log.d("list_insights_diff", "C: New item: ${newItem.insightsType} - type: ${newItem.type}")
            Log.d("list_insights_diff", "areContentsTheSame: $result")
        }
        return result
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        if (oldItem is ListInsightItem && newItem is ListInsightItem) {
            return true
        }
        return null
    }
}
