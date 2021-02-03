package org.wordpress.android.ui.jetpack.scan.history

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fullscreen_error_with_retry.*
import kotlinx.android.synthetic.main.scan_history_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.TabUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.LocaleManagerWrapper
import javax.inject.Inject

class ScanHistoryFragment : Fragment(R.layout.scan_history_fragment), ScrollableViewInitializedListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var localeManagerWrapper: LocaleManagerWrapper
    private lateinit var viewModel: ScanHistoryViewModel

    private val onTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabReselected(tab: Tab) {
        }

        override fun onTabUnselected(tab: Tab) {
        }

        override fun onTabSelected(tab: Tab) {
            viewModel.onTabSelected(tab.position)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initViewModel(getSite(savedInstanceState))
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun initViewModel(site: SiteModel) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ScanHistoryViewModel::class.java)
        setupObservers()
        viewModel.start(site)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
            uiHelpers.updateVisibility(tab_layout, uiState.contentVisible)
            uiHelpers.updateVisibility(view_pager, uiState.contentVisible)
            uiHelpers.updateVisibility(error_layout, uiState.errorVisible)
            when (uiState) {
                is ContentUiState -> {
                    updateTabs(uiState.tabs)
                }
                is ErrorUiState -> updateErrorLayout(uiState)
            }
        })
    }

    private fun updateErrorLayout(uiState: ErrorUiState) {
        uiHelpers.setTextOrHide(error_title, uiState.title)
        uiHelpers.updateVisibility(error_image, true)
        error_image.setImageResource(uiState.img)
        error_retry.setOnClickListener { uiState.retry.invoke() }
    }

    private fun updateTabs(list: List<TabUiState>) {
        val adapter = ScanHistoryTabAdapter(list, this)
        view_pager.adapter = adapter

        TabLayoutMediator(tab_layout, view_pager) { tab, position ->
            tab.text = uiHelpers.getTextOfUiString(requireContext(), list[position].label)
                    .toString()
                    .toUpperCase(localeManagerWrapper.getLocale())
        }.attach()
        tab_layout.addOnTabSelectedListener(onTabSelectedListener)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initToolbar()
    }

    private fun initToolbar() {
        setHasOptionsMenu(true)
        val activity = (requireActivity() as AppCompatActivity)
        toolbar_main.title = getString(R.string.scan_history)
        activity.setSupportActionBar(toolbar_main)
        activity.supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun getSite(savedInstanceState: Bundle?): SiteModel {
        return if (savedInstanceState == null) {
            requireActivity().intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            requireActivity().onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private class ScanHistoryTabAdapter(
        private val items: List<TabUiState>,
        parent: Fragment
    ) : FragmentStateAdapter(parent) {
        override fun getItemCount(): Int = items.count()

        override fun createFragment(position: Int): Fragment = ScanHistoryListFragment.newInstance(items[position].type)
    }

    override fun onScrollableViewInitialized(viewId: Int) {
        appbar_main.liftOnScrollTargetViewId = viewId
    }
}
