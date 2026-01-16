package org.wordpress.android.ui.posts.editor

import android.view.View
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import dagger.Reusable
import org.wordpress.android.widgets.WPViewPager
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.navigation.EditPostDestination
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.extensions.setLiftOnScrollTargetViewIdAndRequestLayout
import javax.inject.Inject

/**
 * Manages ViewPager navigation and UI updates for different editor destinations.
 *
 * This class extracts navigation-related logic from EditPostActivity to improve
 * testability and separation of concerns.
 */
@Reusable
class EditorNavigationManager @Inject constructor() {
    /**
     * Listener for navigation events that require Activity-level handling.
     */
    interface NavigationListener {
        fun getSiteModel(): SiteModel
        fun getEditPostRepository(): EditPostRepository
        fun provideViewPager(): WPViewPager?
        fun getAppBarLayout(): AppBarLayout?
        fun getToolbar(): Toolbar?
        fun getEditorPhotoPicker(): EditorPhotoPicker?
        fun setTitle(title: CharSequence)
        fun setTitle(resId: Int)
        fun invalidateOptionsMenu()
    }

    private var listener: NavigationListener? = null

    companion object {
        const val VIEW_PAGER_PAGE_CONTENT = 0
        const val VIEW_PAGER_PAGE_SETTINGS = 1
        const val VIEW_PAGER_PAGE_PUBLISH_SETTINGS = 2
        const val VIEW_PAGER_PAGE_HISTORY = 3
        const val VIEW_PAGER_OFFSCREEN_PAGE_LIMIT = 4
    }

    /**
     * Initializes the manager with the required listener.
     */
    fun start(listener: NavigationListener) {
        this.listener = listener
    }

    /**
     * Updates ViewPager position based on navigation destination.
     */
    fun updateViewPagerPosition(destination: EditPostDestination) {
        val targetPage = when (destination) {
            EditPostDestination.Editor -> VIEW_PAGER_PAGE_CONTENT
            EditPostDestination.Settings -> VIEW_PAGER_PAGE_SETTINGS
            EditPostDestination.PublishSettings -> VIEW_PAGER_PAGE_PUBLISH_SETTINGS
            EditPostDestination.History -> VIEW_PAGER_PAGE_HISTORY
        }

        listener?.provideViewPager()?.let { pager ->
            if (pager.currentItem != targetPage) {
                AppLog.d(
                    AppLog.T.POSTS,
                    "EditorNavigationManager: Moving ViewPager from ${pager.currentItem} to $targetPage"
                )
                pager.currentItem = targetPage
            }
        }
    }

    /**
     * Updates UI elements based on current navigation destination.
     */
    @Suppress("LongMethod")
    fun updateUIForDestination(destination: EditPostDestination) {
        val listener = this.listener ?: return
        val appBarLayout = listener.getAppBarLayout()
        val toolbar = listener.getToolbar()

        when (destination) {
            EditPostDestination.Editor -> {
                listener.setTitle(SiteUtils.getSiteNameOrHomeURL(listener.getSiteModel()))
                appBarLayout?.setLiftOnScrollTargetViewIdAndRequestLayout(View.NO_ID)
                toolbar?.setBackgroundResource(R.drawable.tab_layout_background)
            }

            EditPostDestination.Settings -> {
                val titleResId = if (listener.getEditPostRepository().isPage) {
                    R.string.page_settings
                } else {
                    R.string.post_settings
                }
                listener.setTitle(titleResId)
                listener.getEditorPhotoPicker()?.hidePhotoPicker()
                appBarLayout?.liftOnScrollTargetViewId = R.id.settings_fragment_root
                toolbar?.background = null
            }

            EditPostDestination.PublishSettings -> {
                listener.setTitle(R.string.publish_date)
                listener.getEditorPhotoPicker()?.hidePhotoPicker()
                appBarLayout?.setLiftOnScrollTargetViewIdAndRequestLayout(View.NO_ID)
                toolbar?.background = null
            }

            EditPostDestination.History -> {
                listener.setTitle(R.string.history_title)
                listener.getEditorPhotoPicker()?.hidePhotoPicker()
                appBarLayout?.liftOnScrollTargetViewId = R.id.empty_recycler_view
                toolbar?.background = null
            }
        }

        // Refresh options menu as visibility may change based on destination
        listener.invalidateOptionsMenu()
    }

    /**
     * Returns the target page index for a given destination.
     */
    fun getPageForDestination(destination: EditPostDestination): Int {
        return when (destination) {
            EditPostDestination.Editor -> VIEW_PAGER_PAGE_CONTENT
            EditPostDestination.Settings -> VIEW_PAGER_PAGE_SETTINGS
            EditPostDestination.PublishSettings -> VIEW_PAGER_PAGE_PUBLISH_SETTINGS
            EditPostDestination.History -> VIEW_PAGER_PAGE_HISTORY
        }
    }
}
