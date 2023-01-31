package org.wordpress.android.ui.stats.refresh.lists.detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.StatsDetailFragmentBinding
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class StatsDetailFragment : DaggerFragment(R.layout.stats_detail_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var statsSiteProvider: StatsSiteProvider

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Inject
    lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: StatsDetailViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        with(StatsDetailFragmentBinding.bind(view)) {
            with(nonNullActivity as AppCompatActivity) {
                setSupportActionBar(toolbar)
                supportActionBar?.let {
                    it.setHomeButtonEnabled(true)
                    it.setDisplayHomeAsUpEnabled(true)
                }
            }
            initializeViewModels(nonNullActivity, savedInstanceState == null)
            initializeViews()
            initJetpackBanner()
        }
    }

    private fun StatsDetailFragmentBinding.initJetpackBanner() {
        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen = JetpackPoweredScreen.WithDynamicText.STATS
            root.post {
                val jetpackBannerView = jetpackBanner.root
                val scrollableView = root.findViewById<View>(R.id.recyclerView) as? RecyclerView
                    ?: return@post

                jetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView)
                jetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView)
                jetpackBanner.jetpackBannerText.text = uiHelpers.getTextOfUiString(
                    requireContext(),
                    jetpackBrandingUtils.getBrandingTextForScreen(screen)
                )

                if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                    jetpackBanner.root.setOnClickListener {
                        jetpackBrandingUtils.trackBannerTapped(screen)
                        JetpackPoweredBottomSheetFragment
                            .newInstance()
                            .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
                    }
                }
            }
        }
    }
    private fun StatsDetailFragmentBinding.initializeViews() {
        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
    }

    private fun initializeViewModels(activity: FragmentActivity,  isFirstStart: Boolean) {
        val siteId = activity.intent?.getIntExtra(WordPress.LOCAL_SITE_ID, 0) ?: 0
        statsSiteProvider.start(siteId)

        val postId = activity.intent?.getLongExtra(POST_ID, 0L)
        val postType = activity.intent?.getSerializableExtra(POST_TYPE) as String?
        val postTitle = activity.intent?.getSerializableExtra(POST_TITLE) as String?
        val postUrl = activity.intent?.getSerializableExtra(POST_URL) as String?

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(StatsSection.DETAIL.name, StatsDetailViewModel::class.java)
        viewModel.init(
            checkNotNull(postId),
            checkNotNull(postType),
            checkNotNull(postTitle),
            postUrl
        )

        setupObservers(viewModel, isFirstStart)
    }

    private fun setupObservers(viewModel: StatsDetailViewModel, isFirstStart: Boolean) {
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        }

        viewModel.showJetpackOverlay.observeEvent(viewLifecycleOwner) {
            if (isFirstStart) {
                JetpackFeatureFullScreenOverlayFragment
                    .newInstance(JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.STATS)
                    .show(childFragmentManager, JetpackFeatureFullScreenOverlayFragment.TAG)
            }
        }
    }
}
