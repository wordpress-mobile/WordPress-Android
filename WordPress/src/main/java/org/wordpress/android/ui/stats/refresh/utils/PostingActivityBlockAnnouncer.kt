package org.wordpress.android.ui.stats.refresh.utils

import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import androidx.core.view.accessibility.AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
import androidx.core.view.accessibility.AccessibilityEventCompat.TYPE_VIEW_CONTEXT_CLICKED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
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
        blockViews.forEach {
            AccessibilityUtils.setAccessibilityDelegateSafely(
                    it,
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

                                TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> {
                                    currentBlockIndex = 0
                                    currentBoxIndex = 0
                                }

                                TYPE_VIEW_CONTEXT_CLICKED -> {
                                    val box = activityItem.blocks[currentBlockIndex].boxes[currentBoxIndex]
                                    host?.announceForAccessibility(box.contentDescription)
                                    if (currentBoxIndex != activityItem.blocks.size - 1) {
                                        currentBoxIndex++
                                    } else {
                                        currentBoxIndex = 0
                                    }
                                }
                            }
                        }
                    })
        }
    }
}
