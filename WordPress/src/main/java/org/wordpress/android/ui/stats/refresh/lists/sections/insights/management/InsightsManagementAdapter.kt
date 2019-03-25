package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel

class InsightsManagementAdapter : ListAdapter<InsightModel, InsightsManagementViewHolder>(DIFF_CALLBACK) {
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<InsightModel>() {
            override fun areItemsTheSame(m1: InsightModel, m2: InsightModel): Boolean = m1.name == m2.name
            override fun areContentsTheSame(m1: InsightModel, m2: InsightModel): Boolean = m1 == m2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): InsightsManagementViewHolder {
        return InsightsManagementViewHolder(parent)
    }

    override fun onBindViewHolder(holder: InsightsManagementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
