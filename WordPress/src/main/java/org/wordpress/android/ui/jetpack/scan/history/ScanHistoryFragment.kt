package org.wordpress.android.ui.jetpack.scan.history

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.FullscreenErrorWithRetryBinding
import org.wordpress.android.databinding.ScanHistoryFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.TabUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import javax.inject.Inject

@AndroidEntryPoint
class ScanHistoryFragment : Fragment(R.layout.scan_history_fragment), MenuProvider, ScrollableViewInitializedListener {
    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var localeManagerWrapper: LocaleManagerWrapper

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils
    private val viewModel: ScanHistoryViewModel by activityViewModels()
    private var binding: ScanHistoryFragmentBinding? = null

    private val onTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabReselected(tab: Tab) {
            // Do nothing
        }

        override fun onTabUnselected(tab: Tab) {
            // Do nothing
        }

        override fun onTabSelected(tab: Tab) {
            viewModel.onTabSelected(tab.position)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        binding = ScanHistoryFragmentBinding.bind(view).apply {
            getSite(savedInstanceState)?.let { initViewModel(it) }
            initToolbar()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun ScanHistoryFragmentBinding.initViewModel(site: SiteModel) {
        setupObservers()
        viewModel.start(site)
    }

    private fun ScanHistoryFragmentBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiHelpers.updateVisibility(tabLayout, uiState.contentVisible)
            uiHelpers.updateVisibility(viewPager, uiState.contentVisible)
            uiHelpers.updateVisibility(fullscreenErrorWithRetry.errorLayout, uiState.errorVisible)
            when (uiState) {
                is ContentUiState -> {
                    updateTabs(uiState.tabs)
                }
                is ErrorUiState -> fullscreenErrorWithRetry.updateErrorLayout(uiState)
            }
        }
    }

    private fun FullscreenErrorWithRetryBinding.updateErrorLayout(uiState: ErrorUiState) {
        uiHelpers.setTextOrHide(errorTitle, uiState.title)
        uiHelpers.updateVisibility(errorImage, true)
        errorImage.setImageResource(uiState.img)
        errorRetry.setOnClickListener { uiState.retry.invoke() }
    }

    private fun ScanHistoryFragmentBinding.updateTabs(list: List<TabUiState>) {
        val adapter = ScanHistoryTabAdapter(list, this@ScanHistoryFragment)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = uiHelpers.getTextOfUiString(requireContext(), list[position].label)
                .toString()
                .uppercase(localeManagerWrapper.getLocale())
        }.attach()
        tabLayout.addOnTabSelectedListener(onTabSelectedListener)
    }

    private fun ScanHistoryFragmentBinding.initToolbar() {
        val activity = (requireActivity() as AppCompatActivity)
        toolbarMain.title = getString(R.string.scan_history)
        activity.setSupportActionBar(toolbarMain)
        activity.supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun getSite(savedInstanceState: Bundle?): SiteModel? {
        return if (savedInstanceState == null) {
            requireActivity().intent.getSerializableExtraCompat(WordPress.SITE)
        } else {
            savedInstanceState.getSerializableCompat(WordPress.SITE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // Do nothing
    }

    override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
        android.R.id.home -> {
            requireActivity().onBackPressedDispatcher.onBackPressed()
            true
        }
        else -> false
    }

    private class ScanHistoryTabAdapter(
        private val items: List<TabUiState>,
        parent: Fragment
    ) : FragmentStateAdapter(parent) {
        override fun getItemCount(): Int = items.count()

        override fun createFragment(position: Int): Fragment = ScanHistoryListFragment.newInstance(items[position].type)
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        binding?.appbarMain?.liftOnScrollTargetViewId = containerId
        initJetpackBanner(containerId)
    }

    private fun initJetpackBanner(scrollableContainerId: Int) {
        if (jetpackBrandingUtils.shouldShowJetpackBrandingForPhaseOne()) {
            val screen = JetpackPoweredScreen.WithDynamicText.SCAN
            binding?.root?.post {
                val jetpackBannerView = binding?.jetpackBanner?.root ?: return@post
                val scrollableView = binding?.root?.findViewById<View>(scrollableContainerId) as? RecyclerView
                    ?: return@post

                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)
                binding?.jetpackBanner?.jetpackBannerText?.text = uiHelpers.getTextOfUiString(
                    requireContext(),
                    jetpackBrandingUtils.getBrandingTextForScreen(screen)
                )

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    binding?.jetpackBanner?.root?.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(screen)
                        JetpackPoweredBottomSheetFragment
                            .newInstance()
                            .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                    }
                }
            }
        }
    }
}
