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
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.google.android.material.elevation.ElevationOverlayProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.ThemeBrowserFragmentBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.plans.PlansConstants
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

/**
 * A fragment display the themes on a grid view.
 */
class ThemeBrowserFragment : Fragment(), AbsListView.RecyclerListener,
    SearchView.OnQueryTextListener {
    interface ThemeBrowserFragmentCallback {
        fun onActivateSelected(themeId: String)
        fun onTryAndCustomizeSelected(themeId: String)
        fun onViewSelected(themeId: String)
        fun onDetailsSelected(themeId: String)
        fun onSupportSelected(themeId: String)
        fun onSwipeToRefresh()
    }

    private var _binding: ThemeBrowserFragmentBinding? = null
    private val binding get() = _binding!!
    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null
    private var currentThemeId: String? = null
    private var lastSearch: String? = null
    var currentThemeTextView: TextView? = null
        private set
    private var headerCustomizeButton: View? = null
    private val adapter: ThemeBrowserAdapter by lazy {
        ThemeBrowserAdapter(activity, requireNotNull(site).planId, callback, imageManager).apply {
            registerDataSetObserver(ThemeDataSetObserver())
        }
    }
    private var shouldRefreshOnStart = false
    private var site: SiteModel? = null
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var callback: ThemeBrowserFragmentCallback? = null
    private var quickStartEvent: QuickStartEvent? = null

    @Inject
    lateinit var themeStore: ThemeStore

    @Inject
    lateinit var quickStartStore: QuickStartStore

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        site = arguments?.getSerializableCompat(WordPress.SITE)
        if (site == null) {
            ToastUtils.showToast(activity, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            requireActivity().finish()
        }
        setHasOptionsMenu(true)
        if (savedInstanceState != null) {
            lastSearch = savedInstanceState.getString(KEY_LAST_SEARCH)
            quickStartEvent = savedInstanceState.getParcelableCompat(QuickStartEvent.KEY)
        }
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        callback = try {
            activity as ThemeBrowserFragmentCallback
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement ThemeBrowserFragmentCallback")
        }
    }

    override fun onDetach() {
        super.onDetach()
        searchView?.setOnQueryTextListener(null)
        callback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ThemeBrowserFragmentBinding.inflate(inflater, container, false)
        configureGridView(inflater)
        configureSwipeToRefresh()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? ScrollableViewInitializedListener)?.onScrollableViewInitialized(binding.themeListview.id)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    @Suppress("deprecation")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter.setThemeList(fetchThemes())
        binding.themeListview.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (searchMenuItem != null && requireNotNull(searchMenuItem).isActionViewExpanded) {
            outState.putString(KEY_LAST_SEARCH, searchView?.query.toString())
        }
        outState.putParcelable(QuickStartEvent.KEY, quickStartEvent)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)
        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView
        searchView?.setOnQueryTextListener(this)
        searchView?.maxWidth = Int.MAX_VALUE
        if (!TextUtils.isEmpty(lastSearch)) {
            searchMenuItem?.expandActionView()
            onQueryTextSubmit(lastSearch)
            searchView?.setQuery(lastSearch, true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_search) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.THEMES_ACCESSED_SEARCH, site)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        adapter.filter.filter(query)
        searchView?.clearFocus()
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter.filter.filter(newText)
        return true
    }

    override fun onMovedToScrapHeap(view: View?) {
        // cancel image fetch requests if the view has been moved to recycler.
        val niv = view?.findViewById<ImageView>(R.id.theme_grid_item_image)
        if (niv != null) {
            imageManager.cancelRequestAndClearImageView(niv)
        }
    }

    fun setCurrentThemeId(currentThemeId: String?) {
        this.currentThemeId = currentThemeId
        refreshView()
    }

    private fun addHeaderViews(inflater: LayoutInflater) {
        addMainHeader(inflater)
    }

    private fun configureSwipeToRefresh() {
        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(binding.ptrLayout) {
            if (!isAdded) {
                return@buildSwipeToRefreshHelper
            }
            if (!NetworkUtils.checkConnection(activity)) {
                swipeToRefreshHelper?.isRefreshing = false
                binding.textEmpty.setText(R.string.no_network_title)
                return@buildSwipeToRefreshHelper
            }
            setRefreshing(true)
            callback?.onSwipeToRefresh()
        }
        swipeToRefreshHelper?.isRefreshing = shouldRefreshOnStart
    }

    private fun configureGridView(inflater: LayoutInflater) {
        addHeaderViews(inflater)
        binding.themeListview.setRecyclerListener(this)
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
        headerCustomizeButton = header.findViewById(R.id.customize)
        headerCustomizeButton?.setOnClickListener(View.OnClickListener { v: View? ->
            AnalyticsUtils.trackWithSiteDetails(
                AnalyticsTracker.Stat.THEMES_CUSTOMIZE_ACCESSED,
                site
            )
            callback?.onTryAndCustomizeSelected(currentThemeId)
        })
        val details = header.findViewById<LinearLayout>(R.id.details)
        details.setOnClickListener { v: View? ->
            callback?.onDetailsSelected(currentThemeId)
        }
        val support = header.findViewById<LinearLayout>(R.id.support)
        support.setOnClickListener { v: View? ->
            callback?.onSupportSelected(currentThemeId)
        }
        binding.themeListview.addHeaderView(header)
    }

    private fun setThemeNameIfAlreadyAvailable() {
        val currentTheme = themeStore.getActiveThemeForSite(requireNotNull(site))
        if (currentTheme != null) {
            currentThemeTextView?.text = currentTheme.name
        }
    }

    fun setRefreshing(refreshing: Boolean) {
        shouldRefreshOnStart = refreshing
        swipeToRefreshHelper?.let {
            it.isRefreshing = refreshing
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
        binding.emptyView.visibility = if (!hasThemes) View.VISIBLE else View.GONE
        if (!hasThemes && !NetworkUtils.isNetworkAvailable(activity)) {
            binding.textEmpty.setText(R.string.no_network_title)
        }
        binding.themeListview.visibility = if (hasVisibleThemes) View.VISIBLE else View.GONE
        binding.actionableEmptyView.visibility = if (hasNoMatchingThemes) View.VISIBLE else View.GONE
    }

    private fun fetchThemes(): List<ThemeModel> {
        site?.let {
            return if (it.isWPCom) {
                sortedWpComThemes
            } else {
                sortedJetpackThemes
            }
        }
        return ArrayList()
    }

    fun refreshView() {
        adapter.setThemeList(fetchThemes())
    }

    private val sortedWpComThemes: List<ThemeModel>
        private get() {
            val wpComThemes = themeStore.wpComThemes

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
            val wpComThemes = themeStore.wpComThemes
            val uploadedThemes = themeStore.getThemesForSite(requireNotNull(site))

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

    private fun moveActiveThemeToFront(themes: MutableList<ThemeModel>?) {
        if (themes.isNullOrEmpty() || TextUtils.isEmpty(currentThemeId)) {
            return
        }

        // find the index of the active theme
        var activeThemeIndex = 0
        for (theme in themes) {
            if (currentThemeId == theme.themeId) {
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

    private fun removeNonActivePremiumThemes(themes: MutableList<ThemeModel>) {
        if (themes.isEmpty()) {
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

    private fun removeDuplicateThemes(wpComThemes: MutableList<ThemeModel>, uploadedThemes: List<ThemeModel>?) {
        if (wpComThemes.isEmpty() || uploadedThemes.isNullOrEmpty()) {
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
        site?.let {
            val planId = it.planId
            return planId == PlansConstants.PREMIUM_PLAN_ID ||
                    planId == PlansConstants.BUSINESS_PLAN_ID ||
                    planId == PlansConstants.JETPACK_PREMIUM_PLAN_ID ||
                    planId == PlansConstants.JETPACK_BUSINESS_PLAN_ID
        }
        return false
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
        fun newInstance(site: SiteModel): ThemeBrowserFragment {
            val fragment = ThemeBrowserFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            fragment.arguments = bundle
            return fragment
        }
    }
}
