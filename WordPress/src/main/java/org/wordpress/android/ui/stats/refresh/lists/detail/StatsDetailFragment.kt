package org.wordpress.android.ui.stats.refresh.lists.detail

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_date_selector.*
import kotlinx.android.synthetic.main.stats_detail_fragment.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.observeEvent
import javax.inject.Inject

class StatsDetailFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsDetailViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_detail_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViewModels(nonNullActivity)
        initializeViews()
    }

    private fun initializeViews() {
        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
    }

    private fun initializeViewModels(activity: FragmentActivity) {
        val site = activity.intent?.getSerializableExtra(WordPress.SITE) as SiteModel?
        val postId = activity.intent?.getLongExtra(POST_ID, 0L)
        val postType = activity.intent?.getSerializableExtra(POST_TYPE) as String?
        val postTitle = activity.intent?.getSerializableExtra(POST_TITLE) as String?
        val postUrl = activity.intent?.getSerializableExtra(POST_URL) as String?

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(StatsSection.DETAIL.name, StatsDetailViewModel::class.java)
        viewModel.init(
                checkNotNull(site),
                checkNotNull(postId),
                checkNotNull(postType),
                checkNotNull(postTitle),
                postUrl
        )

        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: StatsDetailViewModel) {
        viewModel.isRefreshing.observe(this, Observer {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        })

        viewModel.selectedDateChanged.observeEvent(this) {
            viewModel.onDateChanged()
            true
        }

        viewModel.showDateSelector.observe(this, Observer { dateSelectorUiModel ->
            val dateSelectorVisibility = if (dateSelectorUiModel?.isVisible == true) View.VISIBLE else View.GONE
            if (date_selection_toolbar.visibility != dateSelectorVisibility) {
                date_selection_toolbar.visibility = dateSelectorVisibility
            }
            selected_date.text = dateSelectorUiModel?.date ?: ""
            val enablePreviousButton = dateSelectorUiModel?.enableSelectPrevious == true
            if (select_previous_date.isEnabled != enablePreviousButton) {
                select_previous_date.isEnabled = enablePreviousButton
            }
            val enableNextButton = dateSelectorUiModel?.enableSelectNext == true
            if (select_next_date.isEnabled != enableNextButton) {
                select_next_date.isEnabled = enableNextButton
            }
        })
    }
}
