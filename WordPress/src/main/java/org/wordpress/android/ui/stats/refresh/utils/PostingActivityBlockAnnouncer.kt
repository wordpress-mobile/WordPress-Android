package org.wordpress.android.ui.stats.refresh.utils

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.INVISIBLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.VERY_LOW
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class PostingActivityBlockAnnouncer
@Inject constructor(private val resourceProvider: ResourceProvider) {
    private lateinit var activityItem: ActivityItem
    private var currentBoxIndex: Int = 0
    private var currentBlockIndex: Int = 0
    private lateinit var blockViews: List<View>
    private var isStatsInPeriod = false

    fun initialize(activityItem: ActivityItem, vararg blockViews: View) {
        this.activityItem = activityItem
        this.blockViews = blockViews.toList()

        setupViewDelegate()
    }

    private fun setupViewDelegate() {
        blockViews.forEach { blockView ->
            AccessibilityUtils.setAccessibilityDelegateSafely(
                    blockView,
                    object : AccessibilityDelegateCompat() {
                        override fun onPopulateAccessibilityEvent(
                            host: View?,
                            event: AccessibilityEvent?
                        ) {
                            super.onPopulateAccessibilityEvent(host, event)
                            if (event?.eventType == TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                                val view = blockViews.find { view -> view.id == host?.id }
                                currentBlockIndex = blockViews.indexOf(view)
                                currentBoxIndex = 0
                                isStatsInPeriod = false
                            }
                        }

                        override fun onInitializeAccessibilityNodeInfo(
                            host: View?,
                            info: AccessibilityNodeInfoCompat?
                        ) {
                            super.onInitializeAccessibilityNodeInfo(host, info)
                            info?.addAction(
                                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                            ACTION_CLICK, resourceProvider.getString(
                                            R.string.stats_posting_activity_action
                                    )
                                    )
                            )
                        }
                    })

            blockView.setOnClickListener { view ->
                val boxItems = activityItem.blocks[currentBlockIndex].boxItems
                var boxItem = boxItems[currentBoxIndex]

                // If a box has no stats then it's ignored.
                while (boxItem.box == INVISIBLE || boxItem.box == VERY_LOW) {
                    currentBoxIndex++

                    // If the end of the box list was reached then restart the index.
                    if (currentBoxIndex == boxItems.size - 1) {
                        if (isStatsInPeriod) {
                            view.announceForAccessibility(
                                    resourceProvider.getString(
                                            R.string.stats_posting_activity_end_description
                                    )
                            )
                        } else {
                            view.announceForAccessibility(
                                    resourceProvider.getString(
                                            R.string.stats_posting_activity_empty_description
                                    )
                            )
                        }

                        currentBoxIndex = 0
                        return@setOnClickListener
                    }

                    boxItem = boxItems[currentBoxIndex]
                }

                // Once the loop didn't return from the listener then a box with stats was found.
                isStatsInPeriod = true

                view.announceForAccessibility(boxItem.contentDescription)

                // If this isn't the last box then increase the index if not then announce that we reached the end.
                if (currentBoxIndex != boxItems.size - 1) {
                    currentBoxIndex++
                } else {
                    view.announceForAccessibility(
                            resourceProvider.getString(
                                    R.string.stats_posting_activity_end_description
                            )
                    )
                    currentBoxIndex = 0
                }
            }
        }
    }
}
