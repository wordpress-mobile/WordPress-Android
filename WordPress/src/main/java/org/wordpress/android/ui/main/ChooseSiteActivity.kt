package org.wordpress.android.ui.main

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.SitePickerActivityBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.main.ChooseSiteActivity.Companion.ARG_SITE_CREATION_SOURCE
import org.wordpress.android.ui.main.ChooseSiteActivity.Companion.SOURCE
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource.Companion.fromString
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

@AndroidEntryPoint
class ChooseSiteActivity : LocaleAwareActivity() {
    private val viewModel: SiteViewModel by viewModels()
    private val adapter = ChooseSiteAdapter()
    private lateinit var binding: SitePickerActivityBinding
    private lateinit var menuSearch: MenuItem
    private lateinit var menuEditPin: MenuItem
    private lateinit var menuAdd: MenuItem
    private lateinit var refreshHelper: SwipeToRefreshHelper

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var dispatcher: Dispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SitePickerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        binding.toolbarMain.setNavigationOnClickListener { finish() }

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

        viewModel.loadSites()
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        super.onStop()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (refreshHelper.isRefreshing) {
            refreshHelper.isRefreshing = false
        }
        viewModel.loadSites(event.updatedSites)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteRemoved(event: OnSiteRemoved) {
        if (event.isError.not()) {
            viewModel.loadSites()
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
        menuAdd = menu.findItem(R.id.menu_add)
        menuEditPin = menu.findItem(R.id.menu_pin)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val searchView = menuSearch.actionView as SearchView
        setupSearchView(searchView)
        return true
    }

    private fun setupSearchView(searchView: SearchView) {
        menuSearch.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                menuAdd.isVisible = false
                menuEditPin.isVisible = false
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String): Boolean {
                        if (!DeviceUtils.getInstance().hasHardwareKeyboard(this@ChooseSiteActivity)) {
                            ActivityUtils.hideKeyboardForced(searchView)
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String): Boolean {
                        viewModel.searchSites(newText)
                        return true
                    }
                })
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                searchView.setOnQueryTextListener(null)
                viewModel.loadSites()
                menuAdd.isVisible = true
                menuEditPin.isVisible = true
                invalidateOptionsMenu()
                return true
            }
        })
    }

    @Suppress("ReturnCount")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_pin -> {
                if (adapter.mode is ActionMode.Pin) {
                    disablePinSitesMode()
                } else {
                    enablePinSitesMode()
                }
                return true
            }

            R.id.menu_add -> {
                addSite(this, accountStore.hasAccessToken(), SiteCreationSource.MY_SITE)
                return true
            }

            R.id.menu_search -> return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addSite(activity: FragmentActivity, hasAccessToken: Boolean, source: SiteCreationSource) {
        if (hasAccessToken) {
            if (!BuildConfig.ENABLE_ADD_SELF_HOSTED_SITE) {
                ActivityLauncher.newBlogForResult(activity, source)
            } else {
                // user is signed into wordpress app, so use the dialog to enable choosing whether to
                // create a new wp.com blog or add a self-hosted one
                showAddSiteDialog(activity, source)
            }
        } else {
            // user doesn't have an access token, so simply enable adding self-hosted
            ActivityLauncher.addSelfHostedSiteForResult(activity)
        }
    }

    private fun showAddSiteDialog(activity: FragmentActivity, source: SiteCreationSource) {
        val dialog: DialogFragment = AddSiteDialog()
        val args = Bundle()
        args.putString(ARG_SITE_CREATION_SOURCE, source.label)
        dialog.arguments = args
        dialog.show(activity.supportFragmentManager, AddSiteDialog.ADD_SITE_DIALOG_TAG)
    }

    /**
     * Enable pin sites mode via menu items
     */
    private fun enablePinSitesMode() {
        menuSearch.isVisible = false
        menuAdd.isVisible = false
        menuEditPin.setIcon(null)
        menuEditPin.title = "Done"
        adapter.setActionMode(ActionMode.Pin)
    }

    /**
     * Disable pin sites mode via menu items
     */
    private fun disablePinSitesMode() {
        menuSearch.isVisible = true
        menuAdd.isVisible = true
        menuEditPin.setIcon(R.drawable.pin_filled)
        menuEditPin.title = "Edit Pins"
        adapter.setActionMode(ActionMode.None)
    }

    private fun setupRecycleView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter.apply {
            onReload = { viewModel.loadSites() }
            onSiteClicked = { selectSite(it) }
        }
        binding.recyclerView.scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        binding.recyclerView.setEmptyView(binding.actionableEmptyView)
    }

    private fun selectSite(siteRecord: SiteRecord) {
        AppPrefs.addRecentlyPickedSiteId(siteRecord.localId)
        setResult(RESULT_OK, Intent().putExtra(KEY_SITE_LOCAL_ID, siteRecord.localId))
        finish()
    }

    companion object {
        const val ARG_SITE_CREATION_SOURCE = "ARG_SITE_CREATION_SOURCE"
        const val SOURCE = "source"
        const val KEY_SITE_LOCAL_ID = "local_id"

        @JvmStatic
        fun startForResult(activity: Activity) {
            Intent(activity, ChooseSiteActivity::class.java)
                .let { activity.startActivityForResult(it, RequestCodes.SITE_PICKER) }
        }
    }
}


class AddSiteDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val source = fromString(requireArguments().getString(ARG_SITE_CREATION_SOURCE))
        val items = arrayOf<CharSequence>(
            getString(R.string.site_picker_create_wpcom),
            getString(R.string.site_picker_add_self_hosted)
        )
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.site_picker_add_site)
        builder.setAdapter(
            ArrayAdapter(requireActivity(), R.layout.add_new_site_dialog_item, R.id.text, items)
        ) { _: DialogInterface?, which: Int ->
            if (which == 0) {
                ActivityLauncher.newBlogForResult(activity, source)
            } else {
                ActivityLauncher.addSelfHostedSiteForResult(activity)
            }
        }
        AnalyticsTracker.track(
            Stat.ADD_SITE_ALERT_DISPLAYED,
            mapOf(SOURCE to source.label)
        )
        return builder.create()
    }

    companion object {
        const val ADD_SITE_DIALOG_TAG = "add_site_dialog"
    }
}
