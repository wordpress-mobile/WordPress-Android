package org.wordpress.android.ui.stats.refresh.utils

import android.view.View
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import javax.inject.Inject

class PostingActivityBlockAnnouncer
@Inject constructor() {
    private var activityItem: ActivityItem? = null
    private var currentBoxIndex: Int? = null
    private var currentBlockIndex: Int? = null
    private var blockViews:List<View>? = null 

     fun onBlocksInflated(vararg blockViews:View) {
         this.blockViews = blockViews.toList()
    }

    fun onActivityItemBound(activityItem: ActivityItem) {
        this.activityItem = activityItem
    }
}
