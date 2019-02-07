package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView.LayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_fragment.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.StatsAbstractFragment
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlockAdapter
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsNavigator
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.observeEvent
import javax.inject.Inject

class StatsViewAllFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var viewModel: StatsViewAllViewModel
    private lateinit var navigator: StatsNavigator
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private var layoutManager: LayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        fun newInstance(statsType: StatsViewType): StatsViewAllFragment {
            val fragment = StatsViewAllFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, statsType)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_view_all_fragment, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }

        val intent = activity?.intent
        if (intent != null) {
            if (intent.hasExtra(WordPress.SITE)) {
                outState.putSerializable(WordPress.SITE, intent.getSerializableExtra(WordPress.SITE))
            }
            if (intent.hasExtra(WordPress.SITE)) {
                outState.putSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE,
                        intent.getSerializableExtra(StatsAbstractFragment.ARGS_VIEW_TYPE))
            }
        }

        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        this.layoutManager = layoutManager
        recyclerView.layoutManager = this.layoutManager
        recyclerView.addItemDecoration(
                StatsListItemDecoration(
                        resources.getDimensionPixelSize(dimen.margin_small),
                        resources.getDimensionPixelSize(dimen.margin_small),
                        1
                )
        )

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, savedInstanceState)
    }

    private fun initializeViewModels(activity: FragmentActivity, savedInstanceState: Bundle?) {
        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        val type = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(StatsAbstractFragment.ARGS_VIEW_TYPE) as StatsViewType
        } else {
            savedInstanceState.getSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE) as StatsViewType
        }

        val clazz = when (type) {
            StatsViewType.FOLLOWERS -> StatsViewAllFollowersViewModel::class.java
            StatsViewType.COMMENTS -> StatsViewAllCommentsViewModel::class.java
            else -> StatsViewAllFollowersViewModel::class.java
        }

        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(clazz)

        this.navigator = StatsNavigator(site, activity, statsDateFormatter)

        setupObservers()

        viewModel.start(site)
    }

    private fun setupObservers() {
        viewModel.isRefreshing.observe(this, Observer {
            it?.let { isRefreshing ->
                swipeToRefreshHelper.isRefreshing = isRefreshing
            }
        })

        viewModel.data.observe(this, Observer {
            if (it != null) {
                updateInsights(it)
            }
        })

        viewModel.navigationTarget.observeEvent(this) { target ->
            navigator.navigate(target)
            return@observeEvent true
        }
    }

    private fun updateInsights(statsState: List<StatsBlock>) {
        val adapter: StatsBlockAdapter
        if (recyclerView.adapter == null) {
            adapter = StatsBlockAdapter(imageManager)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as StatsBlockAdapter
        }
        val layoutManager = recyclerView?.layoutManager
        val recyclerViewState = layoutManager?.onSaveInstanceState()
        adapter.update(statsState)
        layoutManager?.onRestoreInstanceState(recyclerViewState)
    }
}
