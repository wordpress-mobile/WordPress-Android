package org.wordpress.android.ui

import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

abstract class ViewPagerFragment : Fragment {
    constructor() : super()

    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    /**
     * Provide a scrollable view that will be used with "lift on scroll" functionality of AppBar in parent
     * fragment/activity. ID will of the scrollable view be set to unique one using View.generateViewId()
     */
    abstract fun getScrollableViewForUniqueIdProvision(): View?

    /**
     * It is expected that onResume will be called when the fragment in ViewPager becomes active.
     * This is default behavior of FragmentStateAdapter and FragmentStatePagerAdapter with
     * BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT flag.
     */
    override fun onResume() {
        super.onResume()

        // set unique ID to scrollable view and notify parent fragment/activity
        val scrollableContainer = getScrollableViewForUniqueIdProvision()
        scrollableContainer?.let {
            if (parentFragment is NestedScrollableContainerInitializedListener) {
                scrollableContainer.id = View.generateViewId()
                (parentFragment as NestedScrollableContainerInitializedListener).onScrollableContainerInitialized(
                        scrollableContainer.id
                )
            } else if (activity is NestedScrollableContainerInitializedListener) {
                scrollableContainer.id = View.generateViewId()
                (activity as NestedScrollableContainerInitializedListener).onScrollableContainerInitialized(
                        scrollableContainer.id
                )
            }
        }
    }
}
