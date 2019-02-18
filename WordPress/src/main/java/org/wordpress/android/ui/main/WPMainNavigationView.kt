package org.wordpress.android.ui.main

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.support.design.bottomnavigation.LabelVisibilityMode
import android.support.design.internal.BottomNavigationItemView
import android.support.design.internal.BottomNavigationMenuView
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.BottomNavigationView.OnNavigationItemReselectedListener
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.notifications.NotificationsListFragment
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.ReaderPostListFragment
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration

/*
 * Bottom navigation view and related adapter used by the main activity for the
 * four primary views - note that we ignore the built-in icons and labels and
 * insert our own custom views so we have more control over their appearance
 */
class WPMainNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr),
        OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private lateinit var navAdapter: NavAdapter
    private lateinit var fragmentManager: FragmentManager
    private lateinit var pageListener: OnPageListener
    private var prevPosition = -1

    val activeFragment: Fragment?
        get() = navAdapter.getFragment(currentPosition)

    var currentPosition: Int
        get() = getPositionForItemId(selectedItemId)
        set(position) = updateCurrentPosition(position)

    interface OnPageListener {
        fun onPageChanged(position: Int)
        fun onNewPostButtonClicked()
    }

    fun init(fm: FragmentManager, listener: OnPageListener) {
        fragmentManager = fm
        pageListener = listener

        navAdapter = NavAdapter()
        assignNavigationListeners(true)
        disableShiftMode()

        // overlay each item with our custom view
        val menuView = getChildAt(0) as BottomNavigationMenuView
        val inflater = LayoutInflater.from(context)
        for (i in 0 until menu.size()) {
            val itemView = menuView.getChildAt(i) as BottomNavigationItemView
            val customView: View
            // remove the background ripple and use a different layout for the post button
            if (i == PAGE_NEW_POST) {
                itemView.background = null
                customView = inflater.inflate(R.layout.navbar_post_item, menuView, false)
                customView.id = R.id.bottom_nav_new_post_button // identify view for QuickStart
            } else {
                customView = inflater.inflate(R.layout.navbar_item, menuView, false)
                val txtLabel = customView.findViewById<TextView>(R.id.nav_label)
                val imgIcon = customView.findViewById<ImageView>(R.id.nav_icon)
                txtLabel.text = getTitleForPosition(i)
                customView.contentDescription = getContentDescriptionForPosition(i)
                imgIcon.setImageResource(getDrawableResForPosition(i))
                if (i == PAGE_READER) {
                    customView.id = R.id.bottom_nav_reader_button // identify view for QuickStart
                }
            }

            itemView.addView(customView)
        }

        currentPosition = AppPrefs.getMainPageIndex()
    }

    private fun disableShiftMode() {
        labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
    }

    private fun assignNavigationListeners(assign: Boolean) {
        setOnNavigationItemSelectedListener(if (assign) this else null)
        setOnNavigationItemReselectedListener(if (assign) this else null)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val position = getPositionForItemId(item.itemId)
        return if (position == PAGE_NEW_POST) {
            handlePostButtonClicked()
            false
        } else {
            currentPosition = position
            pageListener.onPageChanged(position)
            true
        }
    }

    private fun handlePostButtonClicked() {
        val postView = getItemView(PAGE_NEW_POST)

        // animate the button icon before telling the listener the post button was clicked - this way
        // the user sees the animation before the editor appears
        AniUtils.startAnimation(postView, R.anim.navbar_button_scale, object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                // noop
            }

            override fun onAnimationEnd(animation: Animation) {
                pageListener.onNewPostButtonClicked()
            }

            override fun onAnimationRepeat(animation: Animation) {
                // noop
            }
        })
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        // scroll the active fragment's contents to the top when user re-taps the current item
        val position = getPositionForItemId(item.itemId)
        if (position != PAGE_NEW_POST) {
            (navAdapter.getFragment(position) as? OnScrollToTopListener)?.onScrollToTop()
        }
    }

    private fun getPositionForItemId(@IdRes itemId: Int): Int {
        return when (itemId) {
            R.id.nav_sites -> PAGE_MY_SITE
            R.id.nav_reader -> PAGE_READER
            R.id.nav_write -> PAGE_NEW_POST
            R.id.nav_me -> PAGE_ME
            else -> PAGE_NOTIFS
        }
    }

    @IdRes
    private fun getItemIdForPosition(position: Int): Int {
        return when (position) {
            PAGE_MY_SITE -> R.id.nav_sites
            PAGE_READER -> R.id.nav_reader
            PAGE_NEW_POST -> R.id.nav_write
            PAGE_ME -> R.id.nav_me
            else -> R.id.nav_notifications
        }
    }

    private fun updateCurrentPosition(position: Int) {
        // new post page can't be selected, only tapped
        if (position == PAGE_NEW_POST) {
            return
        }

        // remove the title and selected state from the previously selected item
        if (prevPosition > -1) {
            showTitleForPosition(prevPosition, false)
            setImageViewSelected(prevPosition, false)
        }

        // set the title and selected state from the newly selected item
        showTitleForPosition(position, true)
        setImageViewSelected(position, true)

        AppPrefs.setMainPageIndex(position)
        prevPosition = position

        // temporarily disable the nav listeners so they don't fire when we change the selected page
        assignNavigationListeners(false)
        try {
            selectedItemId = getItemIdForPosition(position)
        } finally {
            assignNavigationListeners(true)
        }

        val fragment = navAdapter.getFragment(position)
        if (fragment != null) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, getTagForPosition(position))
                    .commit()
        }
    }

    private fun setImageViewSelected(position: Int, isSelected: Boolean) {
        getImageViewForPosition(position)?.isSelected = isSelected
    }

    @DrawableRes
    private fun getDrawableResForPosition(position: Int): Int {
        return when (position) {
            PAGE_MY_SITE -> R.drawable.ic_my_sites_white_24dp
            PAGE_READER -> R.drawable.ic_reader_white_24dp
            PAGE_NEW_POST -> R.drawable.ic_create_white_24dp
            PAGE_ME -> R.drawable.ic_user_circle_white_24dp
            else -> R.drawable.ic_bell_white_24dp
        }
    }

    fun getTitleForPosition(position: Int): CharSequence {
        @StringRes val idRes: Int = when (position) {
            PAGE_MY_SITE -> R.string.my_site_section_screen_title
            PAGE_READER -> R.string.reader_screen_title
            PAGE_NEW_POST -> R.string.write_post
            PAGE_ME -> R.string.me_section_screen_title
            else -> R.string.notifications_screen_title
        }
        return context.getString(idRes)
    }

    fun getContentDescriptionForPosition(position: Int): CharSequence {
        @StringRes val idRes: Int = when (position) {
            PAGE_MY_SITE -> R.string.tabbar_accessibility_label_my_site
            PAGE_READER -> R.string.tabbar_accessibility_label_reader
            PAGE_NEW_POST -> R.string.tabbar_accessibility_label_write
            PAGE_ME -> R.string.tabbar_accessibility_label_me
            else -> R.string.tabbar_accessibility_label_notifications
        }
        return context.getString(idRes)
    }

    private fun getTagForPosition(position: Int): String {
        return when (position) {
            PAGE_MY_SITE -> TAG_MY_SITE
            PAGE_READER -> TAG_READER
            PAGE_ME -> TAG_ME
            else -> TAG_NOTIFS
        }
    }

    private fun getTitleViewForPosition(position: Int): TextView? {
        if (position == PAGE_NEW_POST) {
            return null
        }
        return getItemView(position)?.findViewById(R.id.nav_label)
    }

    private fun getImageViewForPosition(position: Int): ImageView? {
        val itemView = getItemView(position)
        return itemView?.findViewById(R.id.nav_icon)
    }

    private fun showTitleForPosition(position: Int, show: Boolean) {
        val txtTitle = getTitleViewForPosition(position)
        txtTitle?.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun getFragment(position: Int) = navAdapter.getFragment(position)

    private fun getItemView(position: Int): BottomNavigationItemView? {
        if (isValidPosition(position)) {
            val menuView = getChildAt(0) as BottomNavigationMenuView
            return menuView.getChildAt(position) as BottomNavigationItemView
        }
        return null
    }

    fun showReaderBadge(showBadge: Boolean) {
        showBadge(PAGE_READER, showBadge)
    }

    fun showNoteBadge(showBadge: Boolean) {
        showBadge(PAGE_NOTIFS, showBadge)
    }

    /*
     * show or hide the badge on the 'pageId' icon in the bottom bar
     */
    private fun showBadge(pageId: Int, showBadge: Boolean) {
        val badgeView = getItemView(pageId)?.findViewById<View>(R.id.badge)

        val currentVisibility = badgeView?.visibility
        val newVisibility = if (showBadge) View.VISIBLE else View.GONE
        if (currentVisibility == newVisibility) {
            return
        }

        if (showBadge) {
            AniUtils.fadeIn(badgeView, Duration.MEDIUM)
        } else {
            AniUtils.fadeOut(badgeView, Duration.MEDIUM)
        }
    }

    private fun isValidPosition(position: Int): Boolean {
        return position in 0..(NUM_PAGES - 1)
    }

    private inner class NavAdapter {
        private val mFragments = SparseArray<Fragment>(NUM_PAGES)

        private fun createFragment(position: Int): Fragment? {
            val fragment: Fragment = when (position) {
                PAGE_MY_SITE -> MySiteFragment.newInstance()
                PAGE_READER -> ReaderPostListFragment.newInstance()
                PAGE_ME -> MeFragment.newInstance()
                PAGE_NOTIFS -> NotificationsListFragment.newInstance()
                else -> return null
            }

            mFragments.put(position, fragment)
            return fragment
        }

        internal fun getFragment(position: Int): Fragment? {
            if (isValidPosition(position) && mFragments.get(position) != null) {
                return mFragments.get(position)
            }

            val fragment = fragmentManager.findFragmentByTag(getTagForPosition(position))
            if (fragment != null) {
                mFragments.put(position, fragment)
                return fragment
            } else {
                return createFragment(position)
            }
        }
    }

    companion object {
        private const val NUM_PAGES = 5

        internal const val PAGE_MY_SITE = 0
        internal const val PAGE_READER = 1
        internal const val PAGE_NEW_POST = 2
        internal const val PAGE_ME = 3
        internal const val PAGE_NOTIFS = 4

        private const val TAG_MY_SITE = "tag-mysite"
        private const val TAG_READER = "tag-reader"
        private const val TAG_ME = "tag-me"
        private const val TAG_NOTIFS = "tag-notifs"
    }
}
