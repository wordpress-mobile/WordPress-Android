package org.wordpress.android.ui.main

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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
import org.wordpress.android.ui.LocaleAwareActivity
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
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.widgets.WPDialogSnackbar
import javax.inject.Inject

@AndroidEntryPoint
class ChooseSiteActivity : LocaleAwareActivity() {
    private val viewModel: SiteViewModel by viewModels()
    private val adapter = ChooseSiteAdapter()
    private val mode by lazy { SitePickerMode.valueOf(intent.getStringExtra(KEY_SITE_PICKER_MODE)!!) }
    private val localId: Int? by lazy { intent.getIntExtra(KEY_SITE_LOCAL_ID, -1).takeIf { it != -1 } }
    private lateinit var binding: ChooseSiteActivityBinding
    private lateinit var menuSearch: MenuItem
    private lateinit var menuEditPin: MenuItem
    private lateinit var refreshHelper: SwipeToRefreshHelper
    private var searchKeyword: String? = null

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
        binding.buttonAddSite.setOnClickListener {
            AnalyticsTracker.track(Stat.SITE_SWITCHER_ADD_SITE_TAPPED)
            AddSiteHandler.addSite(this, accountStore.hasAccessToken(), SiteCreationSource.MY_SITE)
        }
        binding.progress.isVisible = true
        setupRecycleView()

        viewModel.sites.observe(this) {
            binding.progress.isVisible = false
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

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
        isRunning = true
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
            viewModel.loadSites(mode, searchKeyword)
        }
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
            binding.layoutAddSite.isVisible = true
        } else {
            menuEditPin.isVisible = false
            binding.layoutAddSite.isVisible = false
        }
    }

    private fun setupSearchView() {
        val searchView = menuSearch.actionView as SearchView
        searchView.maxWidth = Integer.MAX_VALUE
        menuSearch.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                binding.layoutAddSite.isVisible = false
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
    }

    /**
     * Disable pin sites mode via menu items
     */
    private fun disablePinSitesMode() {
        menuSearch.isVisible = true
        menuEditPin.setIcon(R.drawable.pin_filled)
        menuEditPin.title = getString(R.string.site_picker_edit_pins)
        adapter.setActionMode(ActionMode.None)
    }

    private fun setupRecycleView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter.apply {
            onReload = { viewModel.loadSites(this@ChooseSiteActivity.mode, searchKeyword) }
            onSiteClicked = { selectSite(it) }
            onSiteLongClicked = { onSiteLongClick(it) }
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

    private fun onSiteLongClick(siteRecord: SiteRecord) {
        val site: SiteModel = siteStore.getSiteByLocalId(siteRecord.localId) ?: return
        if (site.isUsingWpComRestApi.not()) {
            showRemoveSelfHostedSiteDialog(site)
        }
    }

    private fun showRemoveSelfHostedSiteDialog(site: SiteModel) {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getText(R.string.remove_account))
            .setMessage(resources.getText(R.string.sure_to_remove_account))
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
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        searchKeyword = savedInstanceState.getString(KEY_SEARCH_KEYWORD)

        savedInstanceState.getString(KEY_ACTION_MODE)?.let { actionMode ->
            adapter.setActionMode(ActionMode.from(actionMode))
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
        private const val TRACK_PROPERTY_STATE = "state"
        private const val TRACK_PROPERTY_STATE_EDIT = "edit"
        private const val TRACK_PROPERTY_STATE_DONE = "done"
        private const val TRACK_PROPERTY_SECTION = "section"

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
