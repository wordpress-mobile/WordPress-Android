package org.wordpress.android.ui.themes

import android.annotation.SuppressLint
import android.app.Activity
import android.database.DataSetObserver
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.google.android.material.elevation.ElevationOverlayProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.plans.PlansConstants
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.QuickStartUtils.addQuickStartFocusPointAboveTheView
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.HeaderGridView
import javax.inject.Inject

/**
 * A fragment display the themes on a grid view.
 */
class ThemeBrowserFragment : Fragment(), AbsListView.RecyclerListener,
    SearchView.OnQueryTextListener {
    interface ThemeBrowserFragmentCallback {
        fun onActivateSelected(themeId: String?)
        fun onTryAndCustomizeSelected(themeId: String?)
        fun onViewSelected(themeId: String?)
        fun onDetailsSelected(themeId: String?)
        fun onSupportSelected(themeId: String?)
        fun onSwipeToRefresh()
    }

    private var mSwipeToRefreshHelper: SwipeToRefreshHelper? = null
    private var mCurrentThemeId: String? = null
    private var mLastSearch: String? = null
    private var mGridView: HeaderGridView? = null
    private var mEmptyView: RelativeLayout? = null
    private var mActionableEmptyView: ActionableEmptyView? = null
    var currentThemeTextView: TextView? = null
        private set
    private var mHeaderCustomizeButton: View? = null
    private val adapter: ThemeBrowserAdapter by lazy {
        ThemeBrowserAdapter(activity, mSite!!.planId, mCallback, mImageManager).apply {
            registerDataSetObserver(ThemeDataSetObserver())
        }
    }
    private var mShouldRefreshOnStart = false
    private var mEmptyTextView: TextView? = null
    private var mSite: SiteModel? = null
    private var mSearchMenuItem: MenuItem? = null
    private var mSearchView: SearchView? = null
    private var mCallback: ThemeBrowserFragmentCallback? = null
    private var mQuickStartEvent: QuickStartEvent? = null

    @JvmField
    @Inject
    var mThemeStore: ThemeStore? = null

    @JvmField
    @Inject
    var mQuickStartStore: QuickStartStore? = null

    @JvmField
    @Inject
    var mDispatcher: Dispatcher? = null

    @JvmField
    @Inject
    var mImageManager: ImageManager? = null

    @JvmField
    @Inject
    var mQuickStartUtilsWrapper: QuickStartUtilsWrapper? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
        mSite = arguments!!.getSerializable(WordPress.SITE) as SiteModel?
        if (mSite == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            activity!!.finish()
        }
        setHasOptionsMenu(true)
        if (savedInstanceState != null) {
            mLastSearch = savedInstanceState.getString(KEY_LAST_SEARCH)
            mQuickStartEvent = savedInstanceState.getParcelable(QuickStartEvent.KEY)
        }
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mCallback = try {
            activity as ThemeBrowserFragmentCallback
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement ThemeBrowserFragmentCallback")
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (mSearchView != null) {
            mSearchView!!.setOnQueryTextListener(null)
        }
        mCallback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.theme_browser_fragment, container, false)
        mActionableEmptyView = view.findViewById(R.id.actionable_empty_view)
        mEmptyTextView = view.findViewById(R.id.text_empty)
        mEmptyView = view.findViewById(R.id.empty_view)
        configureGridView(inflater, view)
        configureSwipeToRefresh(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        if (activity is ScrollableViewInitializedListener) {
            (activity as ScrollableViewInitializedListener?)!!.onScrollableViewInitialized(
                mGridView!!.id
            )
        }
    }

    @Suppress("deprecation")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter.setThemeList(fetchThemes())
        mGridView!!.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mSearchMenuItem != null && mSearchMenuItem!!.isActionViewExpanded) {
            outState.putString(KEY_LAST_SEARCH, mSearchView!!.query.toString())
        }
        outState.putParcelable(QuickStartEvent.KEY, mQuickStartEvent)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)
        mSearchMenuItem = menu.findItem(R.id.menu_search)
        mSearchView = mSearchMenuItem.getActionView() as SearchView?
        mSearchView!!.setOnQueryTextListener(this)
        mSearchView!!.maxWidth = Int.MAX_VALUE
        if (!TextUtils.isEmpty(mLastSearch)) {
            mSearchMenuItem.expandActionView()
            onQueryTextSubmit(mLastSearch!!)
            mSearchView!!.setQuery(mLastSearch, true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_search) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.THEMES_ACCESSED_SEARCH, mSite)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        adapter.filter.filter(query)
        if (mSearchView != null) {
            mSearchView!!.clearFocus()
        }
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        adapter.filter.filter(newText)
        return true
    }

    override fun onMovedToScrapHeap(view: View) {
        // cancel image fetch requests if the view has been moved to recycler.
        val niv = view.findViewById<ImageView>(R.id.theme_grid_item_image)
        if (niv != null) {
            mImageManager!!.cancelRequestAndClearImageView(niv)
        }
    }

    fun setCurrentThemeId(currentThemeId: String?) {
        mCurrentThemeId = currentThemeId
        refreshView()
    }

    private fun addHeaderViews(inflater: LayoutInflater) {
        addMainHeader(inflater)
    }

    private fun configureSwipeToRefresh(view: View) {
        mSwipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(
            view.findViewById(R.id.ptr_layout)
        ) {
            if (!isAdded) {
                return@buildSwipeToRefreshHelper
            }
            if (!NetworkUtils.checkConnection(activity)) {
                mSwipeToRefreshHelper!!.isRefreshing = false
                mEmptyTextView!!.setText(R.string.no_network_title)
                return@buildSwipeToRefreshHelper
            }
            setRefreshing(true)
            mCallback!!.onSwipeToRefresh()
        }
        mSwipeToRefreshHelper.setRefreshing(mShouldRefreshOnStart)
    }

    private fun configureGridView(inflater: LayoutInflater, view: View) {
        mGridView = view.findViewById(R.id.theme_listview)
        addHeaderViews(inflater)
        mGridView.setRecyclerListener(this)
    }

    private fun addMainHeader(inflater: LayoutInflater) {
        @SuppressLint("InflateParams") val header =
            inflater.inflate(R.layout.theme_grid_cardview_header, null)

        // inflater doesn't work with automatic elevation in night mode so we set card background color manually
        val headerCardView = header.findViewById<View>(R.id.header_card)
        val elevationOverlayProvider = ElevationOverlayProvider(header.context)
        val elevatedSurfaceColor =
            elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(headerCardView.elevation)
        headerCardView.setBackgroundColor(elevatedSurfaceColor)
        currentThemeTextView = header.findViewById(R.id.header_theme_text)
        setThemeNameIfAlreadyAvailable()
        mHeaderCustomizeButton = header.findViewById(R.id.customize)
        mHeaderCustomizeButton.setOnClickListener(View.OnClickListener { v: View? ->
            AnalyticsUtils.trackWithSiteDetails(
                AnalyticsTracker.Stat.THEMES_CUSTOMIZE_ACCESSED,
                mSite
            )
            mCallback!!.onTryAndCustomizeSelected(mCurrentThemeId)
        })
        val details = header.findViewById<LinearLayout>(R.id.details)
        details.setOnClickListener { v: View? -> mCallback!!.onDetailsSelected(mCurrentThemeId) }
        val support = header.findViewById<LinearLayout>(R.id.support)
        support.setOnClickListener { v: View? -> mCallback!!.onSupportSelected(mCurrentThemeId) }
        mGridView!!.addHeaderView(header)
    }

    private fun setThemeNameIfAlreadyAvailable() {
        val currentTheme = mThemeStore!!.getActiveThemeForSite(mSite!!)
        if (currentTheme != null) {
            currentThemeTextView!!.text = currentTheme.name
        }
    }

    fun setRefreshing(refreshing: Boolean) {
        mShouldRefreshOnStart = refreshing
        if (mSwipeToRefreshHelper != null) {
            mSwipeToRefreshHelper!!.isRefreshing = refreshing
            if (!refreshing) {
                refreshView()
            }
        }
    }

    private fun updateDisplay() {
        if (!isAdded || view == null) {
            return
        }
        val hasThemes = adapter.unfilteredCount > 0
        val hasVisibleThemes = adapter.count > 0
        val hasNoMatchingThemes = hasThemes && !hasVisibleThemes
        mEmptyView!!.visibility = if (!hasThemes) View.VISIBLE else View.GONE
        if (!hasThemes && !NetworkUtils.isNetworkAvailable(activity)) {
            mEmptyTextView!!.setText(R.string.no_network_title)
        }
        mGridView!!.visibility =
            if (hasVisibleThemes) View.VISIBLE else View.GONE
        mActionableEmptyView!!.visibility =
            if (hasNoMatchingThemes) View.VISIBLE else View.GONE
    }

    private fun fetchThemes(): List<ThemeModel> {
        if (mSite == null) {
            return ArrayList()
        }
        return if (mSite!!.isWPCom) {
            sortedWpComThemes
        } else sortedJetpackThemes
    }

    fun refreshView() {
        adapter.setThemeList(fetchThemes())
    }

    private val sortedWpComThemes: List<ThemeModel>
        private get() {
            val wpComThemes = mThemeStore!!.wpComThemes

            // first thing to do is attempt to find the active theme and move it to the front of the list
            moveActiveThemeToFront(wpComThemes)

            // then remove all premium themes from the list with an exception for the active theme
            if (!shouldShowPremiumThemes()) {
                removeNonActivePremiumThemes(wpComThemes)
            }
            return wpComThemes
        }
    private val sortedJetpackThemes: List<ThemeModel>
        private get() {
            val wpComThemes = mThemeStore!!.wpComThemes
            val uploadedThemes = mThemeStore!!.getThemesForSite(mSite!!)

            // put the active theme at the top of the uploaded themes list
            moveActiveThemeToFront(uploadedThemes)

            // remove all premium themes from the WP.com themes list
            removeNonActivePremiumThemes(wpComThemes)

            // remove uploaded themes from WP.com themes list (including active theme)
            removeDuplicateThemes(wpComThemes, uploadedThemes)
            val allThemes: MutableList<ThemeModel> = ArrayList()
            allThemes.addAll(uploadedThemes)
            allThemes.addAll(wpComThemes)
            return allThemes
        }

    private fun moveActiveThemeToFront(themes: List<ThemeModel>?) {
        if (themes == null || themes.isEmpty() || TextUtils.isEmpty(mCurrentThemeId)) {
            return
        }

        // find the index of the active theme
        var activeThemeIndex = 0
        for (theme in themes) {
            if (mCurrentThemeId == theme.themeId) {
                theme.active = true
                activeThemeIndex = themes.indexOf(theme)
                break
            }
        }

        // move active theme to front of list
        if (activeThemeIndex > 0) {
            themes.add(0, themes.removeAt(activeThemeIndex))
        }
    }

    private fun removeNonActivePremiumThemes(themes: MutableList<ThemeModel>?) {
        if (themes == null || themes.isEmpty()) {
            return
        }
        val iterator = themes.iterator()
        while (iterator.hasNext()) {
            val theme = iterator.next()
            if (!theme.isFree && !theme.active) {
                iterator.remove()
            }
        }
    }

    private fun removeDuplicateThemes(
        wpComThemes: MutableList<ThemeModel>?,
        uploadedThemes: List<ThemeModel>?
    ) {
        if (wpComThemes == null || wpComThemes.isEmpty() || uploadedThemes == null || uploadedThemes.isEmpty()) {
            return
        }
        for (uploadedTheme in uploadedThemes) {
            val wpComIterator = wpComThemes.iterator()
            while (wpComIterator.hasNext()) {
                val wpComTheme = wpComIterator.next()
                if (StringUtils.equals(
                        wpComTheme.themeId,
                        uploadedTheme.themeId.replace("-wpcom", "")
                    )
                ) {
                    wpComIterator.remove()
                    break
                }
            }
        }
    }

    private fun shouldShowPremiumThemes(): Boolean {
        if (mSite == null) {
            return false
        }
        val planId = mSite!!.planId
        return planId == PlansConstants.PREMIUM_PLAN_ID || planId == PlansConstants.BUSINESS_PLAN_ID || planId == PlansConstants.JETPACK_PREMIUM_PLAN_ID || planId == PlansConstants.JETPACK_BUSINESS_PLAN_ID
    }

    private inner class ThemeDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            updateDisplay()
        }

        override fun onInvalidated() {
            updateDisplay()
        }
    }

    companion object {
        @JvmField
        val TAG: String = ThemeBrowserFragment::class.java.name
        private const val KEY_LAST_SEARCH = "last_search"
        @JvmStatic
        fun newInstance(site: SiteModel?): ThemeBrowserFragment {
            val fragment = ThemeBrowserFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            fragment.arguments = bundle
            return fragment
        }
    }
}
