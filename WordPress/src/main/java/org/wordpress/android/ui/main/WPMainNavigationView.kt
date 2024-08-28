package org.wordpress.android.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.navigation.NavigationBarItemView
import com.google.android.material.navigation.NavigationBarMenuView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationBarView.OnItemReselectedListener
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.ME
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.MY_SITE
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.NOTIFS
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.READER
import org.wordpress.android.ui.main.jetpack.staticposter.JetpackStaticPosterFragment
import org.wordpress.android.ui.main.jetpack.staticposter.UiData
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteFragment
import org.wordpress.android.ui.notifications.NotificationsListFragment
import org.wordpress.android.ui.posts.PostUtils.EntryPoint
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.ReaderFragment
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.extensions.getColorStateListFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject
import com.google.android.material.R as MaterialR

/*
 * Bottom navigation view and related adapter used by the main activity for the
 * four primary views - note that we ignore the built-in icons and labels and
 * insert our own custom views so we have more control over their appearance
 */
@AndroidEntryPoint
@SuppressLint("RestrictedApi") // https://github.com/wordpress-mobile/WordPress-Android/issues/21079
class WPMainNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    OnItemSelectedListener, OnItemReselectedListener {
    private lateinit var navAdapter: NavAdapter
    private var fragmentManager: FragmentManager? = null
    private lateinit var pageListener: OnPageListener
    private var prevPosition = -1
    private lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
    private val unselectedButtonAlpha = ResourcesCompat.getFloat(
        resources,
        MaterialR.dimen.material_emphasis_disabled
    )
    private lateinit var navigationBarView: NavigationBarView

    val disabledColorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    val enabledColorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1f) })

    @Inject
    lateinit var meGravatarLoader: MeGravatarLoader

    @Inject
    lateinit var accountStore: AccountStore

    private var currentPosition: Int
        get() = getPositionForItemId(navigationBarView.selectedItemId)
        set(position) = updateCurrentPosition(position)

    val activeFragment: Fragment?
        get() = navAdapter.getFragment(currentPosition)

    var currentSelectedPage: PageType
        get() = getPageForItemId(navigationBarView.selectedItemId)
        set(pageType) = updateCurrentPosition(pages().indexOf(pageType))

    interface OnPageListener {
        fun onPageChanged(position: Int)
        fun onNewPostButtonClicked(promptId: Int, origin: EntryPoint)
    }

    fun init(fm: FragmentManager, listener: OnPageListener, helper: JetpackFeatureRemovalPhaseHelper) {
        fragmentManager = fm
        pageListener = listener
        jetpackFeatureRemovalPhaseHelper = helper

        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.main_navigation_view, this, true)
        navigationBarView = findViewById(R.id.main_navigation_bar_view)
        val horizontalPadding: Int = context.resources.getDimensionPixelSize(R.dimen.my_site_navigation_rail_padding)
        navigationBarView.setPadding(horizontalPadding, 0, horizontalPadding, 0)

        navAdapter = NavAdapter()
        assignNavigationListeners(true)
        disableShiftMode()

        // overlay each item with our custom view
        val menuView = navigationBarView.getChildAt(0) as NavigationBarMenuView
        if (!BuildConfig.ENABLE_READER) hideReaderTab()

        for (i in 0 until navigationBarView.menu.size()) {
            val itemView = menuView.getChildAt(i) as NavigationBarItemView
            val customView: View = inflater.inflate(R.layout.navbar_item, menuView, false)

            val txtLabel = customView.findViewById<TextView>(R.id.nav_label)
            val imgIcon = customView.findViewById<ImageView>(R.id.nav_icon)
            txtLabel.text = getTitleForPosition(i)
            customView.contentDescription = getContentDescriptionForPosition(i)
            imgIcon.setImageResource(getDrawableResForPosition(i))
            if (i == getPosition(READER)) {
                customView.id = R.id.bottom_nav_reader_button // identify view for QuickStart
            }
            if (i == getPosition(NOTIFS)) {
                customView.id = R.id.bottom_nav_notifications_button // identify view for QuickStart
            }

            if (i == getPosition(ME)) {
                loadGravatar(imgIcon, accountStore.account?.avatarUrl.orEmpty())
            }
            itemView.addView(customView)
        }

        if(getMainPageIndex() != getPosition(ME)) {
            setImageViewSelected(getPosition(ME), false)
        }

        currentPosition = getMainPageIndex()
    }

    private fun loadGravatar(imgIcon: ImageView, avatarUrl: String) {
        if (avatarUrl.isEmpty()) {
            AppLog.d(AppLog.T.MAIN, "Attempted to load an empty Gravatar URL!")
            return
        }
        imgIcon.setPadding(resources.getDimensionPixelSize(R.dimen.navbar_me_icon_padding))
        AppLog.d(AppLog.T.MAIN, meGravatarLoader.constructGravatarUrl(avatarUrl))
        imgIcon.let {
            meGravatarLoader.load(
                false,
                meGravatarLoader.constructGravatarUrl(avatarUrl),
                null,
                it,
                ImageType.USER,
                object : ImageManager.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: Exception?, model: Any?) {
                        val appLogMessage = "onLoadFailed while loading Gravatar image!"
                        if (e == null) {
                            AppLog.e(
                                AppLog.T.MAIN,
                                "$appLogMessage e == null"
                            )
                        } else {
                            AppLog.e(
                                AppLog.T.MAIN,
                                appLogMessage,
                                e
                            )
                        }
                    }

                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any?) {
                        ImageViewCompat.setImageTintList(imgIcon, null)
                        if (resource is BitmapDrawable) {
                            var bitmap = resource.bitmap
                            // create a copy since the original bitmap may by automatically recycled
                            bitmap = bitmap.copy(bitmap.config, true)
                            WordPress.getBitmapCache().put(
                                avatarUrl,
                                bitmap
                            )
                        }
                    }
                }
            )
        }
    }

    private fun getMainPageIndex(): Int {
        return if (jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()) 0
        else AppPrefs.getMainPageIndex(numPages() - 1)
    }

    private fun hideReaderTab() {
        navigationBarView.menu.removeItem(R.id.nav_reader)
    }

    private fun disableShiftMode() {
        navigationBarView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
    }

    private fun assignNavigationListeners(assign: Boolean) {
        navigationBarView.setOnItemSelectedListener(if (assign) this else null)
        navigationBarView.setOnItemReselectedListener(if (assign) this else null)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val position = getPositionForItemId(item.itemId)
        currentPosition = position
        performHapticFeedback()
        pageListener.onPageChanged(position)
        return true
    }

    private fun performHapticFeedback() {
        val position = currentPosition
        getItemView(position)?.run {
            performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY
            )
        }
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
            R.id.nav_notifications -> NOTIFS
            else -> ME
        }
    }

    @IdRes
    private fun getItemIdForPosition(position: Int): Int {
        return when (getPageTypeOrNull(position)) {
            MY_SITE -> R.id.nav_sites
            READER -> R.id.nav_reader
            NOTIFS -> R.id.nav_notifications
            else -> R.id.nav_me
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

        if (jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures())
            AppPrefs.setMainPageIndex(0)
        else AppPrefs.setMainPageIndex(position)

        // temporarily disable the nav listeners so they don't fire when we change the selected page
        assignNavigationListeners(false)
        try {
            navigationBarView.selectedItemId = getItemIdForPosition(position)
        } finally {
            assignNavigationListeners(true)
        }

        val fragment = navAdapter.getFragment(position)
        val previousFragment = navAdapter.getFragment(prevPosition)
        if (fragment != null) {
            if (previousFragment != null) {
                fragmentManager?.beginTransaction()?.detach(previousFragment)?.attach(fragment)?.commit()
            } else {
                fragmentManager?.beginTransaction()?.attach(fragment)?.commit()
            }
        }
        prevPosition = position
    }

    private fun setImageViewSelected(position: Int, isSelected: Boolean) {
        getImageViewForPosition(position)?.let {
            if(position == getPosition(ME)) {
                if(!isSelected){
                    it.colorFilter = disabledColorFilter
                } else
                    it.colorFilter = enabledColorFilter
            }

            it.isSelected = isSelected
            it.alpha = if (isSelected) 1f else unselectedButtonAlpha
            if(it.isSelected) {
                val pop = AnimationUtils.loadAnimation(it.context, R.anim.bottom_nav_icon_pop)
                it.startAnimation(pop)
            }
        }
    }

    private fun setTitleViewSelected(position: Int, isSelected: Boolean) {
        getTitleViewForPosition(position)?.setTextColor(
            context.getColorStateListFromAttribute(
                if (isSelected) R.attr.wpColorNavBar else R.attr.wpColorOnSurfaceMedium
            )
        )
    }

    @DrawableRes
    private fun getDrawableResForPosition(position: Int): Int {
        return when (getPageTypeOrNull(position)) {
            MY_SITE -> R.drawable.ic_home_selected
            READER -> R.drawable.ic_reader_selected
            NOTIFS -> R.drawable.ic_notifications_selected
            else -> R.drawable.ic_me_bottom_nav
        }
    }

    private fun getTitleForPosition(position: Int): CharSequence {
        @StringRes val idRes: Int = when (pages().getOrNull(position)) {
            MY_SITE -> R.string.my_site_section_screen_title
            READER -> R.string.reader_screen_title
            NOTIFS -> R.string.notifications_screen_title
            else -> R.string.me_section_screen_title
        }
        return context.getString(idRes)
    }

    private fun getContentDescriptionForPosition(position: Int): CharSequence {
        @StringRes val idRes: Int = when (pages().getOrNull(position)) {
            MY_SITE -> R.string.tabbar_accessibility_label_my_site
            READER -> R.string.tabbar_accessibility_label_reader
            NOTIFS -> R.string.tabbar_accessibility_label_notifications
            else -> R.string.tabbar_accessibility_label_me
        }
        return context.getString(idRes)
    }

    fun getContentDescriptionForPageType(pageType: PageType): CharSequence {
        return getContentDescriptionForPosition(getPosition(pageType))
    }

    private fun getTagForPageType(pageType: PageType): String {
        return when (pageType) {
            MY_SITE -> TAG_MY_SITE
            READER -> TAG_READER
            NOTIFS -> TAG_NOTIFS
            ME -> TAG_ME
        }
    }

    private fun getTitleViewForPosition(position: Int): TextView? {
        return getItemView(position)?.findViewById(R.id.nav_label)
    }

    private fun getImageViewForPosition(position: Int): ImageView? {
        val itemView = getItemView(position)
        return itemView?.findViewById(R.id.nav_icon)
    }

    fun getFragment(pageType: PageType) = navAdapter.getFragmentIfExists(getPosition(pageType))

    private fun getItemView(position: Int): NavigationBarItemView? {
        if (isValidPosition(position)) {
            val menuView = navigationBarView.getChildAt(0) as NavigationBarMenuView
            return menuView.getChildAt(position) as NavigationBarItemView
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
        val badgeView = getItemView(pageId)?.findViewById<View>(R.id.nav_badge)

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
        private fun createFragment(pageType: PageType, helper: JetpackFeatureRemovalPhaseHelper): Fragment {
            val shouldUseStaticPostersFragment = helper.shouldShowStaticPage()
            val fragment = when (pageType) {
                MY_SITE -> MySiteFragment.newInstance()
                READER -> if (shouldUseStaticPostersFragment)
                    JetpackStaticPosterFragment.newInstance(UiData.READER)
                else ReaderFragment()

                NOTIFS -> if (shouldUseStaticPostersFragment)
                    JetpackStaticPosterFragment.newInstance(UiData.NOTIFICATIONS)
                else NotificationsListFragment.newInstance()

                ME -> MeFragment.newInstance()
            }
            fragmentManager?.beginTransaction()
                ?.add(R.id.fragment_container, fragment, getTagForPageType(pageType))
                ?.commitNow()
            return fragment
        }


        internal fun getFragment(position: Int): Fragment? {
            return pages().getOrNull(position)?.let { pageType ->
                val currentFragment = fragmentManager?.findFragmentByTag(getTagForPageType(pageType))
                return currentFragment?.let {
                    when (it) {
                        is ReaderFragment, is NotificationsListFragment -> checkAndCreateForStaticPage(it, pageType)
                        is JetpackStaticPosterFragment -> checkAndCreateForNonStaticPage(it, pageType)
                        else -> it
                    }
                } ?: createFragment(pageType, jetpackFeatureRemovalPhaseHelper)
            }
        }

        private fun checkAndCreateForStaticPage(fragment: Fragment, pageType: PageType): Fragment {
            return if (jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()) {
                fragmentManager?.beginTransaction()?.remove(fragment)?.commitNow()
                createFragment(pageType, jetpackFeatureRemovalPhaseHelper)
            } else {
                fragment
            }
        }

        private fun checkAndCreateForNonStaticPage(fragment: Fragment, pageType: PageType): Fragment {
            return if (!jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()) {
                fragmentManager?.beginTransaction()?.remove(fragment)?.commitNow()
                createFragment(pageType, jetpackFeatureRemovalPhaseHelper)
            } else {
                fragment
            }
        }

        internal fun getFragmentIfExists(position: Int): Fragment? {
            return pages().getOrNull(position)?.let { pageType ->
                fragmentManager?.findFragmentByTag(getTagForPageType(pageType))
            }
        }
    }

    companion object {
        private val pages = if (BuildConfig.ENABLE_READER) {
            listOf(MY_SITE, READER, NOTIFS, ME)
        } else {
            listOf(MY_SITE, NOTIFS, ME)
        }

        private const val TAG_MY_SITE = "tag-mysite"
        private const val TAG_READER = "tag-reader"
        private const val TAG_NOTIFS = "tag-notifs"
        private const val TAG_ME = "tag-me"

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
        MY_SITE, READER, NOTIFS, ME
    }
}
