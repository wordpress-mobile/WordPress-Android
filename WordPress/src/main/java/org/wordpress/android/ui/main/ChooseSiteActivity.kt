package org.wordpress.android.ui.main

import android.animation.ValueAnimator
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.ChooseSiteActivityBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.widgets.WPDialogSnackbar
import javax.inject.Inject

@AndroidEntryPoint
class ChooseSiteActivity : BaseAppCompatActivity() {
    private val viewModel: SiteViewModel by viewModels()
    private val adapter = ChooseSiteAdapter()
    private val mode by lazy { SitePickerMode.valueOf(intent.getStringExtra(KEY_SITE_PICKER_MODE)!!) }
    private val localId: Int? by lazy { intent.getIntExtra(KEY_SITE_LOCAL_ID, -1).takeIf { it != -1 } }
    private lateinit var binding: ChooseSiteActivityBinding
    private lateinit var menuSearch: MenuItem
    private lateinit var menuEditPin: MenuItem
    private lateinit var refreshHelper: SwipeToRefreshHelper
    private var searchKeyword: String? = null
    private var isAddSiteMenuOpen = false
    // Accessibility importance each covered view had before the menu hid it, keyed by view id.
    private val contentAccessibilityImportance = mutableMapOf<Int, Int>()
    private val addSiteMenuItems by lazy {
        // ordered bottom-to-top so the stagger animates upward from the main FAB
        listOf(binding.fabMenuItemSelfHosted, binding.fabMenuItemWpcom)
    }
    private val fabRestingCornerSize by lazy {
        resources.getDimension(R.dimen.fab_corner_size_resting)
    }
    private var fabCornerAnimator: ValueAnimator? = null
    private var currentFabCornerSize = 0f
    private var fabColorAnimator: ValueAnimator? = null
    private var currentFabContainerColor = 0
    // Bumped on every open and close so a menu transition that is still waiting on a layout pass can
    // tell it has been superseded and drop its animation.
    private var addSiteMenuGeneration = 0

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var siteStore: SiteStore

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChooseSiteActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        if (savedInstanceState == null) {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_DISPLAYED)
        }
        binding.toolbarMain.setNavigationOnClickListener {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_DISMISSED)
            finish()
        }
        setupAddSiteFab()
        binding.progress.isVisible = !appPrefsWrapper.hasFetchedSites
        setupRecycleView()

        viewModel.sites.observe(this) {
            binding.progress.isVisible = !appPrefsWrapper.hasFetchedSites
            binding.recyclerView.isVisible = it.isNotEmpty()
            binding.actionableEmptyView.isVisible = it.isEmpty()
            adapter.setSites(it)
        }

        refreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(binding.ptrLayout) {
            refreshHelper.isRefreshing = true
            dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(SiteUtils.getFetchSitesPayload()))
        }

        localId?.let {
            appPrefsWrapper.addRecentSiteLocalId(it)
            adapter.selectedSiteId = it
        }

        viewModel.loadSites(mode)
    }

    private fun setupAddSiteFab() {
        currentFabCornerSize = fabRestingCornerSize
        currentFabContainerColor = getColor(R.color.fab_container)
        setupFabAccessibility()
        applyFabAccessibilityState()
        binding.fabAddSite.setOnClickListener {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_ADD_SITE_TAPPED)
            // when the user is signed in and can add a self-hosted site there are two choices, so
            // expand the FAB menu; otherwise there's only one action, so trigger it directly
            if (canExpandAddSiteMenu()) {
                if (isAddSiteMenuOpen) closeAddSiteMenu() else openAddSiteMenu()
            } else {
                AddSiteHandler.addSite(this, accountStore.hasAccessToken(), SiteCreationSource.MY_SITE)
            }
        }
        binding.fabAddSite.setOnLongClickListener {
            ToastUtils.showToast(this, R.string.site_picker_add_a_site, ToastUtils.Duration.SHORT)
            true
        }
        binding.fabAddSite.redirectContextClickToLongPressListener()

        binding.fabMenuScrim.setOnClickListener { closeAddSiteMenu() }
        val createWpcomSite = {
            closeAddSiteMenu()
            ActivityLauncher.newBlogForResult(this, SiteCreationSource.MY_SITE)
        }
        val addSelfHostedSite = {
            closeAddSiteMenu()
            ActivityLauncher.addSelfHostedSiteForResult(this)
        }
        binding.fabMenuItemWpcom.setOnClickListener { createWpcomSite() }
        binding.fabMenuItemSelfHosted.setOnClickListener { addSelfHostedSite() }

        onBackPressedDispatcher.addCallback(this) {
            if (isAddSiteMenuOpen) {
                closeAddSiteMenu()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun openAddSiteMenu() {
        if (isAddSiteMenuOpen) return
        isAddSiteMenuOpen = true
        AnalyticsTracker.track(
            Stat.ADD_SITE_ALERT_DISPLAYED,
            mapOf(KEY_SOURCE to SiteCreationSource.MY_SITE.label)
        )

        setContentBehindMenuAccessible(false)
        applyFabAccessibilityState()
        binding.fabMenuScrim.animate().cancel()
        binding.fabMenuScrim.isVisible = true
        binding.fabMenuScrim.animate().alpha(SCRIM_ALPHA).setDuration(FAB_MENU_ANIM_DURATION).start()
        applyFabIcon(isOpen = true)
        animateFabCornerSize(fabOpenCornerSize())
        applyFabColors(isOpen = true)

        val generation = ++addSiteMenuGeneration
        addSiteMenuItems.forEachIndexed { index, item ->
            item.animate().cancel()
            item.alpha = 0f
            item.scaleX = FAB_MENU_ITEM_COLLAPSED_SCALE
            item.scaleY = FAB_MENU_ITEM_COLLAPSED_SCALE
            // Make the item visible first so it gets measured, then pivot from its trailing edge.
            item.isVisible = true
            setMenuItemTransformOrigin(item) {
                if (generation != addSiteMenuGeneration) return@setMenuItemTransformOrigin
                item.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(index * FAB_MENU_STAGGER)
                    .setDuration(FAB_MENU_ANIM_DURATION)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .withEndAction(null)
                    .start()
            }
        }
    }

    private fun closeAddSiteMenu() {
        if (!isAddSiteMenuOpen) return
        isAddSiteMenuOpen = false

        setContentBehindMenuAccessible(true)
        applyFabAccessibilityState()
        binding.fabMenuScrim.animate().cancel()
        binding.fabMenuScrim.animate().alpha(0f).setDuration(FAB_MENU_ANIM_DURATION)
            .withEndAction { binding.fabMenuScrim.isVisible = false }.start()
        applyFabIcon(isOpen = false)
        animateFabCornerSize(fabRestingCornerSize)
        applyFabColors(isOpen = false)

        val generation = ++addSiteMenuGeneration
        addSiteMenuItems.forEachIndexed { index, item ->
            item.animate().cancel()
            // An item that has not been laid out yet cannot animate out, and its pending open was
            // just superseded, so collapse it outright rather than leaving it on screen.
            if (item.width == 0 || item.height == 0) {
                collapseMenuItem(item)
                return@forEachIndexed
            }
            setMenuItemTransformOrigin(item) {
                if (generation != addSiteMenuGeneration) return@setMenuItemTransformOrigin
                item.animate()
                    .alpha(0f)
                    .scaleX(FAB_MENU_ITEM_COLLAPSED_SCALE)
                    .scaleY(FAB_MENU_ITEM_COLLAPSED_SCALE)
                    .setStartDelay(index * FAB_MENU_STAGGER)
                    .setDuration(FAB_MENU_ANIM_DURATION)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .withEndAction { item.isVisible = false }
                    .start()
            }
        }
    }

    private fun collapseMenuItem(item: View) {
        item.isVisible = false
        item.alpha = 0f
        item.scaleX = FAB_MENU_ITEM_COLLAPSED_SCALE
        item.scaleY = FAB_MENU_ITEM_COLLAPSED_SCALE
    }

    /**
     * Describes the FAB as the expandable menu toggle it is, labelling its click action with whether
     * it will open or close the menu. Installed once: the delegate reads the menu state when a
     * screen reader asks, so only the state description has to be refreshed per transition.
     */
    private fun setupFabAccessibility() {
        ViewCompat.setAccessibilityDelegate(
            binding.fabAddSite,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    if (!canExpandAddSiteMenu()) return
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_CLICK,
                            getString(
                                if (isAddSiteMenuOpen) {
                                    R.string.site_picker_add_a_site_collapse
                                } else {
                                    R.string.site_picker_add_a_site_expand
                                }
                            )
                        )
                    )
                }
            }
        )
    }

    /**
     * Announces whether the menu is expanded. With only one action available the FAB triggers it
     * directly instead of expanding a menu, so it carries no expanded state in that case.
     */
    private fun applyFabAccessibilityState() {
        ViewCompat.setStateDescription(
            binding.fabAddSite,
            if (!canExpandAddSiteMenu()) {
                null
            } else {
                getString(
                    if (isAddSiteMenuOpen) {
                        R.string.site_picker_add_site_menu_expanded
                    } else {
                        R.string.site_picker_add_site_menu_collapsed
                    }
                )
            }
        )
    }

    private fun canExpandAddSiteMenu() =
        accountStore.hasAccessToken() && BuildConfig.ENABLE_ADD_SELF_HOSTED_SITE

    /**
     * The scrim blocks touches but leaves the views behind it in the accessibility tree, so a
     * screen reader can still reach and activate site rows hidden under the open menu. Mark them as
     * inert while it is open, remembering each view's own importance so reopening restores what it
     * had rather than assuming AUTO.
     */
    private fun setContentBehindMenuAccessible(isAccessible: Boolean) {
        val covered = listOf(
            binding.appbarMain,
            binding.ptrLayout,
            binding.actionableEmptyView,
            binding.progress
        )
        if (isAccessible) {
            covered.forEach { view ->
                view.importantForAccessibility = contentAccessibilityImportance.getOrDefault(
                    view.id,
                    View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                )
            }
            contentAccessibilityImportance.clear()
        } else {
            covered.forEach { view ->
                contentAccessibilityImportance.getOrPut(view.id) { view.importantForAccessibility }
                view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
        }
    }

    /**
     * Morphs the FAB between its resting squircle and the full circle Material 3 uses while a FAB
     * menu is open.
     */
    private fun animateFabCornerSize(targetSize: Float) {
        fabCornerAnimator?.cancel()
        fabCornerAnimator = ValueAnimator.ofFloat(currentFabCornerSize, targetSize).apply {
            duration = FAB_MENU_ANIM_DURATION
            addUpdateListener { animator ->
                val size = animator.animatedValue as Float
                currentFabCornerSize = size
                applyFabCornerSize(size)
            }
            start()
        }
    }

    private fun applyFabCornerSize(size: Float) {
        // setCornerSize updates the drawable's existing model in place, so the corner animates
        // without rebuilding a ShapeAppearanceModel on every frame.
        forEachFabShapeDrawable { it.setCornerSize(size) }
    }

    /** Half the FAB's height is a circle; falls back to the resting corner before it is measured. */
    private fun fabOpenCornerSize() = if (binding.fabAddSite.height > 0) {
        binding.fabAddSite.height / 2f
    } else {
        fabRestingCornerSize
    }

    /**
     * Walks the FAB's background for the MaterialShapeDrawable that paints its container, which is
     * the content layer of a RippleDrawable today.
     *
     * Driving the morph through FloatingActionButton's own setShapeAppearanceModel instead left the
     * container a rounded square while the menu was open, reproducibly, on device against Material
     * 1.14.0. Do not swap this walk for the public setter without checking that again.
     *
     * Why is not fully pinned down. Both public setters do reach this drawable, so the earlier note
     * here claiming they do not was wrong. The likely explanation is that they write through the
     * impl's stored ShapeAppearanceModel, which createShapeDrawable rebuilds the background from:
     * any rebuild mid-morph would restore the resting squircle the style declares and lose the
     * interpolated corner. Mutating the live drawable keeps the corner on the instance being drawn
     * either way, and avoids rebuilding a whole model every frame.
     *
     * The search handles nesting so that a wrapped background does not silently stop the morph.
     */
    private fun forEachFabShapeDrawable(action: (MaterialShapeDrawable) -> Unit) {
        fun visit(drawable: Drawable?) {
            when (drawable) {
                is MaterialShapeDrawable -> action(drawable)
                is InsetDrawable -> visit(drawable.drawable)
                is LayerDrawable -> for (index in 0 until drawable.numberOfLayers) {
                    visit(drawable.getDrawable(index))
                }
                else -> Unit
            }
        }
        visit(binding.fabAddSite.background)
    }

    /**
     * Swaps the FAB's plus for a close icon outright. Material 3's own FAB menus cut between the two
     * glyphs rather than transitioning them, leaving the container's shape and color change to carry
     * the motion.
     */
    private fun applyFabIcon(isOpen: Boolean) {
        binding.fabAddSite.setImageResource(
            if (isOpen) R.drawable.ic_close_white_24dp else R.drawable.ic_plus_fab_24dp
        )
        // The spec sizes the close glyph smaller than a regular FAB icon while keeping the same
        // container, so the size has to follow the icon rather than sit on the layout. At rest the
        // FAB goes back to the 24dp every other FAB inherits from Material's own style.
        val imageSize = resources.getDimensionPixelSize(
            if (isOpen) R.dimen.fab_menu_close_button_icon_size else R.dimen.fab_icon_size_resting
        )
        // setMaxImageSize already ignores an unchanged size, so this only ever does work on a swap.
        binding.fabAddSite.setMaxImageSize(imageSize)
        // setMaxImageSize rescales the image matrix immediately but leaves the padding that centres
        // the drawable to the next measure pass, so the new glyph draws at the old offset until
        // something else triggers one. Ask for it here instead of letting it land a frame later.
        binding.fabAddSite.requestLayout()
    }

    /**
     * Material 3 grows the menu out of the FAB's top trailing corner, so each item scales from the
     * corner nearest the FAB rather than from its own centre.
     *
     * The items start out gone, so on the first open they have not been measured yet and their width
     * is still zero. Deferring to the next layout pass means the pivot lands on the trailing edge
     * rather than the leading one, which would otherwise make that first expansion grow the wrong
     * way across.
     */
    private fun setMenuItemTransformOrigin(item: View, onReady: () -> Unit) {
        if (item.width > 0 && item.height > 0) {
            item.pivotX = item.width.toFloat()
            item.pivotY = item.height.toFloat()
            onReady()
            return
        }
        item.doOnLayout {
            it.pivotX = it.width.toFloat()
            it.pivotY = it.height.toFloat()
            onReady()
        }
    }

    /**
     * Swaps the FAB between its resting tonal colors and the inverted close-button colors Material 3
     * uses while the menu is open, so the close button reads as distinct from the tonal menu items.
     */
    private fun applyFabColors(isOpen: Boolean, animate: Boolean = true) {
        val onContainer = if (isOpen) R.color.fab_close_on_container else R.color.fab_on_container
        // setImageResource reapplies the FAB's own tint as a color filter, which takes precedence
        // over imageTintList, so the icon color has to go through the support tint instead.
        binding.fabAddSite.supportImageTintList =
            AppCompatResources.getColorStateList(this, onContainer)

        val target = getColor(if (isOpen) R.color.fab_close_container else R.color.fab_container)
        fabColorAnimator?.cancel()
        if (!animate) {
            applyFabContainerColor(target)
            return
        }
        fabColorAnimator = ValueAnimator.ofArgb(currentFabContainerColor, target).apply {
            duration = FAB_MENU_ANIM_DURATION
            addUpdateListener { applyFabContainerColor(it.animatedValue as Int) }
            start()
        }
    }

    private fun applyFabContainerColor(color: Int) {
        currentFabContainerColor = color
        val tint = ColorStateList.valueOf(color)
        forEachFabShapeDrawable { it.setTintList(tint) }
    }

    /**
     * Restores the expanded menu after a configuration change by jumping straight to the open
     * end state — no entrance animation and no analytics, both of which belong to a user-initiated
     * open. Setting the FAB visible directly (rather than via show()) skips its scale animation.
     */
    private fun expandAddSiteMenuInstantly() {
        isAddSiteMenuOpen = true
        binding.fabAddSite.isVisible = true
        applyFabIcon(isOpen = true)
        // This runs before the first layout pass, so the FAB has no height to halve yet and the
        // background it paints has not been built. Wait for layout before writing the shape and the
        // container color, or the restored FAB stays a resting squircle in its resting color while
        // the menu is open.
        binding.fabAddSite.doOnLayout {
            if (!isAddSiteMenuOpen) return@doOnLayout
            currentFabCornerSize = fabOpenCornerSize()
            applyFabCornerSize(currentFabCornerSize)
            applyFabColors(isOpen = true, animate = false)
        }
        setContentBehindMenuAccessible(false)
        applyFabAccessibilityState()
        binding.fabMenuScrim.animate().cancel()
        binding.fabMenuScrim.isVisible = true
        binding.fabMenuScrim.alpha = SCRIM_ALPHA
        // Supersede any transition still waiting on a layout pass, so it cannot animate over the
        // end state being restored here.
        addSiteMenuGeneration++
        addSiteMenuItems.forEach { item ->
            item.animate().cancel()
            item.isVisible = true
            item.alpha = 1f
            item.scaleX = 1f
            item.scaleY = 1f
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
        isRunning = true
        dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(SiteUtils.getFetchSitesPayload()))
    }

    override fun onResume() {
        super.onResume()
        ActivityId.trackLastActivity(ActivityId.SITE_PICKER)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        isRunning = false
        super.onStop()
    }

    override fun onDestroy() {
        fabCornerAnimator?.cancel()
        fabCornerAnimator = null
        fabColorAnimator?.cancel()
        fabColorAnimator = null
        super.onDestroy()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (refreshHelper.isRefreshing) {
            refreshHelper.isRefreshing = false
        }
        if (event.isError.not()) {
            appPrefsWrapper.hasFetchedSites = true
            viewModel.loadSites(mode, searchKeyword)
        }
        // Hide the "first fetch" spinner whether the fetch succeeded or errored so the user
        // isn't stranded looking at a spinner when the network call fails.
        binding.progress.isVisible = false
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteRemoved(event: OnSiteRemoved) {
        if (event.isError.not()) {
            viewModel.loadSites(mode, searchKeyword)
        } else {
            // shouldn't happen
            AppLog.e(AppLog.T.DB, "Encountered unexpected error while attempting to remove site: " + event.error)
            ToastUtils.showToast(this, R.string.site_picker_remove_site_error)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.choose_site, menu)
        menuSearch = menu.findItem(R.id.menu_search)
        menuEditPin = menu.findItem(R.id.menu_pin)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (adapter.mode == ActionMode.Pin) {
            // restore state
            enablePinSitesMode()
        }
        setupMenuVisibility()
        setupSearchView()
        return true
    }

    private fun setupMenuVisibility() {
        if (mode == SitePickerMode.DEFAULT) {
            menuEditPin.isVisible = true
            // hide the FAB while editing pins, otherwise reveal it
            if (adapter.mode == ActionMode.Pin) {
                hideAddSiteFab()
            } else {
                showAddSiteFab()
            }
        } else {
            menuEditPin.isVisible = false
            hideAddSiteFab()
        }
    }

    /**
     * The FAB is the only way to dismiss its own menu apart from the scrim, so the menu has to come
     * down with it. An open menu left behind would keep the scrim up and, now that the views behind
     * it are marked inert, hide the toolbar and site list from screen readers with no way back.
     */
    private fun hideAddSiteFab() {
        closeAddSiteMenu()
        binding.fabAddSite.hide()
    }

    private fun showAddSiteFab() {
        val fab = binding.fabAddSite
        // FloatingActionButton.show() only plays its entrance animation once the view is laid out.
        // setupMenuVisibility() runs from onPrepareOptionsMenu during the first layout pass, before
        // the FAB is laid out, so post the initial reveal to guarantee the animation. Re-check state
        // when the posted reveal runs: a search may have been expanded in the meantime (e.g. a search
        // restored after rotation), in which case the FAB must stay hidden.
        if (fab.isLaidOut) fab.show() else fab.post { if (shouldShowAddSiteFab()) fab.show() }
    }

    private fun shouldShowAddSiteFab(): Boolean {
        val searchExpanded = ::menuSearch.isInitialized && menuSearch.isActionViewExpanded
        return mode == SitePickerMode.DEFAULT && adapter.mode != ActionMode.Pin && !searchExpanded
    }

    private fun setupSearchView() {
        val searchView = menuSearch.actionView as SearchView
        searchView.maxWidth = Integer.MAX_VALUE
        menuSearch.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                hideAddSiteFab()
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String): Boolean {
                        if (!DeviceUtils.getInstance().hasHardwareKeyboard(this@ChooseSiteActivity)) {
                            ActivityUtils.hideKeyboardForced(searchView)
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String): Boolean {
                        searchKeyword = newText
                        AnalyticsTracker.track(Stat.SITE_SWITCHER_SEARCH_PERFORMED)
                        viewModel.loadSites(mode, newText)
                        return true
                    }
                })
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                searchKeyword = null
                searchView.setOnQueryTextListener(null)
                viewModel.loadSites(mode)
                invalidateOptionsMenu()
                return true
            }
        })

        // Restore search keyword
        if (searchKeyword != null) {
            // this is a workaround to set the search keyword after the search view is expanded
            // due to searchKeyword will be cleared after the search view has been expanded first time
            val keyword = searchKeyword // copy the keyword
            menuSearch.expandActionView()
            searchView.post { searchView.setQuery(keyword, true) }
            searchView.clearFocus()
        }
    }

    @Suppress("ReturnCount")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_pin -> {
                if (adapter.mode is ActionMode.Pin) {
                    disablePinSitesMode()
                    AnalyticsTracker.track(
                        Stat.SITE_SWITCHER_TOGGLED_PIN_TAPPED,
                        mapOf(TRACK_PROPERTY_STATE to TRACK_PROPERTY_STATE_DONE)
                    )
                } else {
                    enablePinSitesMode()
                    AnalyticsTracker.track(
                        Stat.SITE_SWITCHER_TOGGLED_PIN_TAPPED,
                        mapOf(TRACK_PROPERTY_STATE to TRACK_PROPERTY_STATE_EDIT)
                    )
                }
                return true
            }

            R.id.menu_search -> return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Enable pin sites mode via menu items
     */
    private fun enablePinSitesMode() {
        menuEditPin.setIcon(null)
        menuEditPin.title = getString(R.string.label_done_button)
        adapter.setActionMode(ActionMode.Pin)
        hideAddSiteFab()
    }

    /**
     * Disable pin sites mode via menu items
     */
    private fun disablePinSitesMode() {
        menuSearch.isVisible = true
        menuEditPin.setIcon(R.drawable.pin_filled)
        menuEditPin.title = getString(R.string.site_picker_edit_pins)
        adapter.setActionMode(ActionMode.None)
        showAddSiteFab()
    }

    private fun setupRecycleView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter.apply {
            onReload = { viewModel.loadSites(this@ChooseSiteActivity.mode, searchKeyword) }
            onSiteClicked = { selectSite(it) }
            onSiteRemoveClicked = { onSiteRemoveClick(it) }
        }
        binding.recyclerView.scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        binding.recyclerView.setEmptyView(binding.actionableEmptyView)
    }

    private fun selectSite(siteRecord: SiteRecord) {
        AnalyticsTracker.track(
            Stat.SITE_SWITCHER_SITE_TAPPED,
            mapOf(TRACK_PROPERTY_SECTION to viewModel.getSection(siteRecord.localId))
        )
        appPrefsWrapper.addRecentSiteLocalId(siteRecord.localId)
        setResult(RESULT_OK, Intent().putExtra(KEY_SITE_LOCAL_ID, siteRecord.localId))
        finish()
    }

    private fun onSiteRemoveClick(siteRecord: SiteRecord) {
        val site: SiteModel = siteStore.getSiteByLocalId(siteRecord.localId) ?: return
        showRemoveSelfHostedSiteDialog(site)
    }

    private fun showRemoveSelfHostedSiteDialog(site: SiteModel) {
        val siteName = SiteUtils.getSiteNameOrHomeURL(site)
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getText(R.string.remove_account))
            .setMessage(getString(R.string.confirm_remove_site, siteName))
            .setPositiveButton(
                resources.getText(R.string.yes)
            ) { _: DialogInterface?, _: Int ->
                dispatcher.dispatch(
                    SiteActionBuilder.newRemoveSiteAction(site)
                )
            }
            .setNegativeButton(resources.getText(R.string.no), null)
            .setCancelable(false)
            .create()
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.ADD_ACCOUNT, RequestCodes.CREATE_SITE -> if (resultCode == RESULT_OK) {
                viewModel.loadSites(mode)
                if (data?.getBooleanExtra(KEY_SITE_CREATED_BUT_NOT_FETCHED, false) == true) {
                    showSiteCreatedButNotFetchedSnackbar()
                } else {
                    val intent = data ?: Intent()
                    intent.putExtra(WPMainActivity.ARG_CREATE_SITE, RequestCodes.CREATE_SITE)
                    setResult(resultCode, intent)
                    finish()
                }
            }
        }

        // Enable the block editor on sites created on mobile
        if (requestCode == RequestCodes.CREATE_SITE) {
            if (data != null) {
                val newSiteLocalID = data.getIntExtra(
                    KEY_SITE_LOCAL_ID,
                    SelectedSiteRepository.UNAVAILABLE
                )
                SiteUtils.enableBlockEditorOnSiteCreation(dispatcher, siteStore, newSiteLocalID)
            }
        }
    }

    private fun showSiteCreatedButNotFetchedSnackbar() {
        val duration = AccessibilityUtils
            .getSnackbarDuration(this, resources.getInteger(R.integer.site_creation_snackbar_duration))
        val message = getString(R.string.site_created_but_not_fetched_snackbar_message)
        WPDialogSnackbar.make(binding.coordinatorLayout, message, duration).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SEARCH_KEYWORD, searchKeyword)
        outState.putString(KEY_ACTION_MODE, adapter.mode.value)
        outState.putBoolean(KEY_ADD_SITE_MENU_OPEN, isAddSiteMenuOpen)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        searchKeyword = savedInstanceState.getString(KEY_SEARCH_KEYWORD)

        savedInstanceState.getString(KEY_ACTION_MODE)?.let { actionMode ->
            adapter.setActionMode(ActionMode.from(actionMode))
        }

        // restore the expanded add-site menu (the menu only exists in DEFAULT mode and can't be
        // open while pinning)
        if (savedInstanceState.getBoolean(KEY_ADD_SITE_MENU_OPEN) &&
            mode == SitePickerMode.DEFAULT && adapter.mode != ActionMode.Pin
        ) {
            expandAddSiteMenuInstantly()
        }
    }

    companion object {
        const val KEY_ARG_SITE_CREATION_SOURCE = "ARG_SITE_CREATION_SOURCE"
        const val KEY_SOURCE = "source"
        const val KEY_SITE_LOCAL_ID = "local_id"
        const val KEY_SITE_PICKER_MODE = "key_site_picker_mode"
        const val KEY_SITE_TITLE_TASK_COMPLETED = "key_site_title_task_completed"
        const val KEY_SITE_CREATED_BUT_NOT_FETCHED = "key_site_created_but_not_fetched"
        const val KEY_SEARCH_KEYWORD = "key_search_keyword"
        const val KEY_ACTION_MODE = "key_action_mode"
        const val KEY_ADD_SITE_MENU_OPEN = "key_add_site_menu_open"
        private const val TRACK_PROPERTY_STATE = "state"
        private const val TRACK_PROPERTY_STATE_EDIT = "edit"
        private const val TRACK_PROPERTY_STATE_DONE = "done"
        private const val TRACK_PROPERTY_SECTION = "section"
        private const val SCRIM_ALPHA = 0.4f
        private const val FAB_MENU_ANIM_DURATION = 200L
        private const val FAB_MENU_STAGGER = 40L
        // Items grow in from a little under full size rather than from nothing, so the menu reads as
        // expanding out of the FAB instead of popping into place.
        private const val FAB_MENU_ITEM_COLLAPSED_SCALE = 0.8f

        @JvmStatic
        var isRunning = false
    }
}


/**
 * Mode for the site picker
 */
enum class SitePickerMode {
    /**
     * Show everything
     */
    DEFAULT,

    /**
     * Show all sites, hide the "Add Site" button and hide the "Edit Pins" button
     */
    SIMPLE,

    /**
     * Hide self-hosted sites for purchasing a domain for a WPCOM site
     * Also hide the "Add Site" button and hide the "Edit Pins" button
     */
    WPCOM_SITES_ONLY
}
