package org.wordpress.android.ui.main

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private val addSiteMenuItems by lazy {
        // ordered bottom-to-top so the stagger animates upward from the main FAB
        listOf(binding.fabMenuItemSelfHosted, binding.fabMenuItemWpcom)
    }
    private val fabMenuItemOffset by lazy { resources.getDimension(R.dimen.margin_extra_large) }

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
        binding.fabAddSite.setOnClickListener {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_ADD_SITE_TAPPED)
            // when the user is signed in and can add a self-hosted site there are two choices, so
            // expand the FAB menu; otherwise there's only one action, so trigger it directly
            if (accountStore.hasAccessToken() && BuildConfig.ENABLE_ADD_SELF_HOSTED_SITE) {
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
        binding.fabWpcom.setOnClickListener { createWpcomSite() }
        binding.fabMenuItemWpcom.setOnClickListener { createWpcomSite() }
        binding.fabSelfHosted.setOnClickListener { addSelfHostedSite() }
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

        binding.fabMenuScrim.animate().cancel()
        binding.fabMenuScrim.isVisible = true
        binding.fabMenuScrim.animate().alpha(SCRIM_ALPHA).setDuration(FAB_MENU_ANIM_DURATION).start()
        binding.fabAddSite.animate().rotation(FAB_ICON_ROTATION).setDuration(FAB_MENU_ANIM_DURATION).start()

        addSiteMenuItems.forEachIndexed { index, item ->
            item.animate().cancel()
            item.alpha = 0f
            item.translationY = fabMenuItemOffset
            item.isVisible = true
            item.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * FAB_MENU_STAGGER)
                .setDuration(FAB_MENU_ANIM_DURATION)
                .withEndAction(null)
                .start()
        }
    }

    private fun closeAddSiteMenu() {
        if (!isAddSiteMenuOpen) return
        isAddSiteMenuOpen = false

        binding.fabMenuScrim.animate().cancel()
        binding.fabMenuScrim.animate().alpha(0f).setDuration(FAB_MENU_ANIM_DURATION)
            .withEndAction { binding.fabMenuScrim.isVisible = false }.start()
        binding.fabAddSite.animate().rotation(0f).setDuration(FAB_MENU_ANIM_DURATION).start()

        addSiteMenuItems.forEachIndexed { index, item ->
            item.animate().cancel()
            item.animate()
                .alpha(0f)
                .translationY(fabMenuItemOffset)
                .setStartDelay(index * FAB_MENU_STAGGER)
                .setDuration(FAB_MENU_ANIM_DURATION)
                .withEndAction { item.isVisible = false }
                .start()
        }
    }

    /**
     * Restores the expanded menu after a configuration change by jumping straight to the open
     * end state — no entrance animation and no analytics, both of which belong to a user-initiated
     * open. Setting the FAB visible directly (rather than via show()) skips its scale animation.
     */
    private fun expandAddSiteMenuInstantly() {
        isAddSiteMenuOpen = true
        binding.fabAddSite.isVisible = true
        binding.fabAddSite.rotation = FAB_ICON_ROTATION
        binding.fabMenuScrim.isVisible = true
        binding.fabMenuScrim.alpha = SCRIM_ALPHA
        addSiteMenuItems.forEach { item ->
            item.isVisible = true
            item.alpha = 1f
            item.translationY = 0f
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
                binding.fabAddSite.hide()
            } else {
                showAddSiteFab()
            }
        } else {
            menuEditPin.isVisible = false
            binding.fabAddSite.hide()
        }
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
                closeAddSiteMenu()
                binding.fabAddSite.hide()
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
        closeAddSiteMenu()
        binding.fabAddSite.hide()
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
        private const val FAB_ICON_ROTATION = 45f
        private const val FAB_MENU_ANIM_DURATION = 200L
        private const val FAB_MENU_STAGGER = 40L

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
