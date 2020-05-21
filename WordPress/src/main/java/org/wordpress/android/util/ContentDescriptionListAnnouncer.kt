package org.wordpress.android.util

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import androidx.annotation.StringRes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

class ContentDescriptionListAnnouncer {
    private var currentIndex = 0

    fun setupAnnouncer(
        @StringRes emptyListText: Int,
        @StringRes endOfListText: Int,
        @StringRes clickActionText: Int? = null,
        contentDescriptions: List<String>,
        targetView: View
    ) {
        AccessibilityUtils.setAccessibilityDelegateSafely(
                targetView,
                object : AccessibilityDelegateCompat() {
                    override fun onPopulateAccessibilityEvent(
                        host: View?,
                        event: AccessibilityEvent?
                    ) {
                        super.onPopulateAccessibilityEvent(host, event)
                        if (event?.eventType == TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                            currentIndex = 0
                        }
                    }

                    override fun onInitializeAccessibilityNodeInfo(
                        host: View?,
                        info: AccessibilityNodeInfoCompat?
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)

                        clickActionText?.let {
                            info?.addAction(
                                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                            ACTION_CLICK, host?.context?.getString(
                                            it
                                    )
                                    )
                            )
                        }
                    }
                })

        targetView.setOnClickListener {
            if (contentDescriptions.isEmpty()) {
                it.announceForAccessibility(it.context.getString(emptyListText))
                return@setOnClickListener
            }

            if (currentIndex == contentDescriptions.size) {
                it.announceForAccessibility(it.context.getString(endOfListText))
                currentIndex = 0
            } else {
                it.announceForAccessibility(contentDescriptions[currentIndex])
                currentIndex++
            }
        }
    }
}
