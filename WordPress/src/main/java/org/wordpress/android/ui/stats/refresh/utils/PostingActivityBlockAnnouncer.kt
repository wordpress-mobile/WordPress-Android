package org.wordpress.android.ui.stats.refresh.utils

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import androidx.core.view.AccessibilityDelegateCompat
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.INVISIBLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.VERY_LOW
import org.wordpress.android.util.AccessibilityUtils
import javax.inject.Inject

class PostingActivityBlockAnnouncer
@Inject constructor() {
    private lateinit var activityItem: ActivityItem
    private var currentBoxIndex: Int = 0
    private var currentBlockIndex: Int = 0
    private lateinit var blockViews: List<View>

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
                            when (event?.eventType) {
                                TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                                    val view = blockViews.find { view -> view.id == host?.id }
                                    currentBlockIndex = blockViews.indexOf(view)
                                    currentBoxIndex = 0
                                }
                            }
                        }
                    })

            blockView.setOnClickListener { view ->
                val boxes = activityItem.blocks[currentBlockIndex].boxes
                var boxItem = boxes[currentBoxIndex]

                while (boxItem.box == INVISIBLE || boxItem.box == VERY_LOW) {
                    currentBoxIndex++

                    if (currentBoxIndex == boxes.size - 1) {
                        currentBoxIndex = 0
                    }
                    boxItem = boxes[currentBoxIndex]
                }

                view.announceForAccessibility(boxItem.contentDescription)
                if (currentBoxIndex != boxes.size - 1) {
                    currentBoxIndex++
                } else {
                    currentBoxIndex = 0
                }
            }
        }
    }
}
