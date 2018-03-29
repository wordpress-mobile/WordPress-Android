package org.wordpress.android.widgets;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ScrollView;


/**
 * Code copied from android.support.v4.widget.NestedScrollView with one modification in the AccessibilityDelegate
 * .performAccessibilityAction(..) method. There is a bug in the Support Library which breaks scroll in the
 * Accessibility Switch Control mode.
 * Steps to reproduce the bug
 * 1. Go to Stats
 * 2. Scroll to the bottom using the Switch Control
 * 3. Try to scroll to the top -> clicking on the scrollView doesn't do anything.
 * <p>
 * WARNING - Do not modify this file, so we can remove it, when the bug in the support library is fixed.
 * https://issuetracker.google.com/issues/68366782
 * https://issuetracker.google.com/issues/70310373
 */
public class WPNestedScrollView extends NestedScrollView {
    private static final AccessibilityDelegate ACCESSIBILITY_DELEGATE = new AccessibilityDelegate();

    public WPNestedScrollView(Context context) {
        this(context, null);
    }

    public WPNestedScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WPNestedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewCompat.setAccessibilityDelegate(this, ACCESSIBILITY_DELEGATE);
    }


    int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0,
                    child.getHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
        }
        return scrollRange;
    }

    static class AccessibilityDelegate extends AccessibilityDelegateCompat {
        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
            if (super.performAccessibilityAction(host, action, arguments)) {
                return true;
            }
            final WPNestedScrollView nsvHost = (WPNestedScrollView) host;
            if (!nsvHost.isEnabled()) {
                return false;
            }
            int viewportHeight;
            int targetScrollY;
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                    nsvHost.fling(0);
                    viewportHeight = nsvHost.getHeight() - nsvHost.getPaddingBottom()
                                               - nsvHost.getPaddingTop();
                    targetScrollY = Math.min(nsvHost.getScrollY() + viewportHeight,
                            nsvHost.getScrollRange());
                    if (targetScrollY != nsvHost.getScrollY()) {
                        nsvHost.smoothScrollTo(0, targetScrollY);
                        return true;
                    }

                    return false;
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                    nsvHost.fling(0);
                    viewportHeight = nsvHost.getHeight() - nsvHost.getPaddingBottom()
                                               - nsvHost.getPaddingTop();
                    targetScrollY = Math.max(nsvHost.getScrollY() - viewportHeight, 0);
                    if (targetScrollY != nsvHost.getScrollY()) {
                        nsvHost.smoothScrollTo(0, targetScrollY);
                        return true;
                    }

                    return false;
            }
            return false;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            final WPNestedScrollView nsvHost = (WPNestedScrollView) host;
            info.setClassName(ScrollView.class.getName());
            if (nsvHost.isEnabled()) {
                final int scrollRange = nsvHost.getScrollRange();
                if (scrollRange > 0) {
                    info.setScrollable(true);
                    if (nsvHost.getScrollY() > 0) {
                        info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
                    }
                    if (nsvHost.getScrollY() < scrollRange) {
                        info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                    }
                }
            }
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            final WPNestedScrollView nsvHost = (WPNestedScrollView) host;
            event.setClassName(ScrollView.class.getName());
            final boolean scrollable = nsvHost.getScrollRange() > 0;
            event.setScrollable(scrollable);
            event.setScrollX(nsvHost.getScrollX());
            event.setScrollY(nsvHost.getScrollY());
            AccessibilityRecordCompat.setMaxScrollX(event, nsvHost.getScrollX());
            AccessibilityRecordCompat.setMaxScrollY(event, nsvHost.getScrollRange());
        }
    }
}
