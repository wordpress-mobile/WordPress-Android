package org.wordpress.android.ui.main

import android.content.Context
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemReselectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import org.wordpress.android.R
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.MY_SITE
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.NOTIFS
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.READER
import org.wordpress.android.ui.notifications.NotificationsListFragment
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.ReaderParentPostListFragment
import org.wordpress.android.ui.reader.ReaderPostListFragment
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration
import org.wordpress.android.util.getColorStateListFromAttribute

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
    private val unselectedButtonAlpha = ResourcesCompat.getFloat(
            resources,
            R.dimen.material_emphasis_disabled
    )

    private var currentPosition: Int
        get() = getPositionForItemId(selectedItemId)
        set(position) = updateCurrentPosition(position)

    val activeFragment: Fragment?
        get() = navAdapter.getFragment(currentPosition)

    var currentSelectedPage: PageType
        get() = getPageForItemId(selectedItemId)
        set(pageType) = updateCurrentPosition(pages().indexOf(pageType))

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
            val customView: View = inflater.inflate(R.layout.navbar_item, menuView, false)

            val txtLabel = customView.findViewById<TextView>(R.id.nav_label)
            val imgIcon = customView.findViewById<ImageView>(R.id.nav_icon)
            txtLabel.text = getTitleForPosition(i)
            customView.contentDescription = getContentDescriptionForPosition(i)
            imgIcon.setImageResource(getDrawableResForPosition(i))
            if (i == getPosition(READER)) {
                customView.id = R.id.bottom_nav_reader_button // identify view for QuickStart
            }

            itemView.addView(customView)
        }

        currentPosition = AppPrefs.getMainPageIndex(numPages() - 1)
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
        currentPosition = position
        pageListener.onPageChanged(position)
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        // scroll the active fragment's contents to the top when user re-taps the current item
        val position = getPositionForItemId(item.itemId)
        (navAdapter.getFragment(position) as? OnScrollToTopListener)?.onScrollToTop()
    }

    private fun getPositionForItemId(@IdRes itemId: Int): Int {
        return getPosition(getPageForItemId(itemId))
    }

    private fun getPageForItemId(@IdRes itemId: Int): PageType {
        return when (itemId) {
            R.id.nav_sites -> MY_SITE
            R.id.nav_reader -> READER
            else -> NOTIFS
        }
    }

    @IdRes
    private fun getItemIdForPosition(position: Int): Int {
        return when (getPageTypeOrNull(position)) {
            MY_SITE -> R.id.nav_sites
            READER -> R.id.nav_reader
            else -> R.id.nav_notifications
        }
    }

    private fun updateCurrentPosition(position: Int) {
        // remove the title and selected state from the previously selected item
        if (prevPosition > -1) {
            setTitleViewSelected(prevPosition, false)
            setImageViewSelected(prevPosition, false)
        }

        // set the title and selected state from the newly selected item
        setTitleViewSelected(position, true)

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
                    // This is used because the main activity sometimes crashes because it's trying to switch fragments
                    // after `onSaveInstanceState` was already called. This is the related issue
                    // https://github.com/wordpress-mobile/WordPress-Android/issues/10852
                    .commitAllowingStateLoss()
        }
    }

    private fun setImageViewSelected(position: Int, isSelected: Boolean) {
        getImageViewForPosition(position)?.let {
            it.isSelected = isSelected
            it.alpha = if (isSelected) 1f else unselectedButtonAlpha
        }
    }

    private fun setTitleViewSelected(position: Int, isSelected: Boolean) {
        getTitleViewForPosition(position)?.setTextColor(
                context.getColorStateListFromAttribute(
                        if (isSelected) R.attr.colorPrimary else R.attr.wpColorOnSurfaceMedium
                )
        )
    }

    @DrawableRes
    private fun getDrawableResForPosition(position: Int): Int {
        return when (getPageTypeOrNull(position)) {
            MY_SITE -> R.drawable.ic_my_sites_white_24dp
            READER -> R.drawable.ic_reader_white_24dp
            else -> R.drawable.ic_bell_white_24dp
        }
    }

    private fun getTitleForPosition(position: Int): CharSequence {
        @StringRes val idRes: Int = when (pages().getOrNull(position)) {
            MY_SITE -> R.string.my_site_section_screen_title
            READER -> R.string.reader_screen_title
            else -> R.string.notifications_screen_title
        }
        return context.getString(idRes)
    }

    private fun getContentDescriptionForPosition(position: Int): CharSequence {
        @StringRes val idRes: Int = when (pages().getOrNull(position)) {
            MY_SITE -> R.string.tabbar_accessibility_label_my_site
            READER -> R.string.tabbar_accessibility_label_reader
            else -> R.string.tabbar_accessibility_label_notifications
        }
        return context.getString(idRes)
    }

    fun getContentDescriptionForPageType(pageType: PageType): CharSequence {
        return getContentDescriptionForPosition(getPosition(pageType))
    }

    private fun getTagForPosition(position: Int): String {
        return when (getPageTypeOrNull(position)) {
            MY_SITE -> TAG_MY_SITE
            READER -> TAG_READER
            else -> TAG_NOTIFS
        }
    }

    private fun getTitleViewForPosition(position: Int): TextView? {
        return getItemView(position)?.findViewById(R.id.nav_label)
    }

    private fun getImageViewForPosition(position: Int): ImageView? {
        val itemView = getItemView(position)
        return itemView?.findViewById(R.id.nav_icon)
    }

    fun getFragment(pageType: PageType) = navAdapter.getFragment(getPosition(pageType))

    private fun getItemView(position: Int): BottomNavigationItemView? {
        if (isValidPosition(position)) {
            val menuView = getChildAt(0) as BottomNavigationMenuView
            return menuView.getChildAt(position) as BottomNavigationItemView
        }
        return null
    }

    fun showReaderBadge(showBadge: Boolean) {
        showBadge(getPosition(READER), showBadge)
    }

    fun showNoteBadge(showBadge: Boolean) {
        showBadge(getPosition(NOTIFS), showBadge)
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
        return position in 0 until numPages()
    }

    private inner class NavAdapter {
        private val mFragments = SparseArray<Fragment>(numPages())

        private fun createFragment(position: Int): Fragment? {
            val fragment: Fragment = when (pages().getOrNull(position)) {
                MY_SITE -> MySiteFragment.newInstance()
                READER -> ReaderParentPostListFragment()
                NOTIFS -> NotificationsListFragment.newInstance()
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
            return if (fragment != null) {
                mFragments.put(position, fragment)
                fragment
            } else {
                createFragment(position)
            }
        }
    }

    companion object {
        private val pages = listOf(MY_SITE, READER, NOTIFS)

        private const val TAG_MY_SITE = "tag-mysite"
        private const val TAG_READER = "tag-reader"
        private const val TAG_NOTIFS = "tag-notifs"

        private fun numPages(): Int = pages.size

        private fun pages(): List<PageType> = pages

        private fun getPageTypeOrNull(position: Int): PageType? {
            return pages().getOrNull(position)
        }

        fun getPosition(pageType: PageType): Int {
            return pages().indexOf(pageType)
        }

        @JvmStatic
        fun getPageType(position: Int): PageType {
            return pages()[position]
        }
    }

    enum class PageType {
        MY_SITE, READER, NOTIFS
    }
}
