package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView.LayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.refresh.StatsListItemDecoration
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.DaysListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.MonthsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.WeeksListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.YearsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightsListViewModel
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsNavigator
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.observeEvent
import javax.inject.Inject

class StatsListFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var statsDateFormatter: StatsDateFormatter
    @Inject lateinit var navigator: StatsNavigator
    private lateinit var viewModel: StatsListViewModel

    private var layoutManager: LayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        fun newInstance(section: StatsSection): StatsListFragment {
            val fragment = StatsListFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, section)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stats_list_fragment, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }

        val intent = activity?.intent
        if (intent != null && intent.hasExtra(WordPress.SITE)) {
            outState.putSerializable(WordPress.SITE, intent.getSerializableExtra(WordPress.SITE))
        }

        super.onSaveInstanceState(outState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val columns = resources.getInteger(R.integer.stats_number_of_columns)
        val layoutManager: LayoutManager = if (columns == 1) {
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        } else {
            StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        }
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        this.layoutManager = layoutManager
        recyclerView.layoutManager = this.layoutManager
        recyclerView.addItemDecoration(
                StatsListItemDecoration(
                        resources.getDimensionPixelSize(dimen.margin_small),
                        resources.getDimensionPixelSize(dimen.margin_small),
                        columns
                )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, savedInstanceState)
    }

    private fun initializeViewModels(activity: FragmentActivity, savedInstanceState: Bundle?) {
        val statsSection = arguments?.getSerializable(typeKey) as StatsSection

        val viewModelClass = when (statsSection) {
            StatsSection.INSIGHTS -> InsightsListViewModel::class.java
            StatsSection.DAYS -> DaysListViewModel::class.java
            StatsSection.WEEKS -> WeeksListViewModel::class.java
            StatsSection.MONTHS -> MonthsListViewModel::class.java
            StatsSection.YEARS -> YearsListViewModel::class.java
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(statsSection.name, viewModelClass)

        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        setupObservers(site, activity)
    }

    private fun setupObservers(site: SiteModel, activity: FragmentActivity) {
        viewModel.data.observe(this, Observer {
            if (it != null) {
                updateInsights(it)
            }
        })

        viewModel.navigationTarget.observeEvent(this) { target ->
            navigator.navigate(site, activity, target)
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
