package org.wordpress.android.ui.stats.refresh.utils

import android.view.View
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import javax.inject.Inject

class PostingActivityBlockAnnouncer
@Inject constructor() {
    private var activityItem: ActivityItem? = null
    private var currentBoxIndex: Int? = null
    private var currentBlockIndex: Int? = null

     fun onBlockClicked(block:View?) {

    }

    fun onBlockFocused() {

    }

    fun onBlockBinded(activityItem: ActivityItem) {
        this.activityItem = activityItem
    }
}
