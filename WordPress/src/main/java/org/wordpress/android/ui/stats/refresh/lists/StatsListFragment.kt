package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.stats_date_selector.*
import kotlinx.android.synthetic.main.stats_error_view.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.StatsListItemDecoration
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.detail.DetailListViewModel
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
        const val LIST_TYPE = "type_key"

        fun newInstance(section: StatsSection): StatsListFragment {
            val fragment = StatsListFragment()
            val bundle = Bundle()
            bundle.putSerializable(LIST_TYPE, section)
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
        (activity?.intent?.getSerializableExtra(LIST_TYPE) as? StatsSection)?.let { sectionFromIntent ->
            outState.putSerializable(LIST_TYPE, sectionFromIntent)
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
                        resources.getDimensionPixelSize(R.dimen.stats_list_card_horizontal_spacing),
                        resources.getDimensionPixelSize(R.dimen.stats_list_card_top_spacing),
                        resources.getDimensionPixelSize(R.dimen.stats_list_card_bottom_spacing),
                        resources.getDimensionPixelSize(R.dimen.stats_list_card_first_spacing),
                        resources.getDimensionPixelSize(R.dimen.stats_list_card_last_spacing),
                        columns
                )
        )

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1) && dy != 0) {
                    viewModel.onScrolledToBottom()
                }
            }
        })

        select_next_date.setOnClickListener {
            viewModel.onNextDateSelected()
        }

        select_previous_date.setOnClickListener {
            viewModel.onPreviousDateSelected()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity)
    }

    private fun initializeViewModels(activity: FragmentActivity) {
        val statsSection = arguments?.getSerializable(LIST_TYPE) as? StatsSection
                ?: activity.intent?.getSerializableExtra(LIST_TYPE) as? StatsSection
                ?: StatsSection.INSIGHTS

        val viewModelClass = when (statsSection) {
            StatsSection.DETAIL -> DetailListViewModel::class.java
            StatsSection.INSIGHTS -> InsightsListViewModel::class.java
            StatsSection.DAYS -> DaysListViewModel::class.java
            StatsSection.WEEKS -> WeeksListViewModel::class.java
            StatsSection.MONTHS -> MonthsListViewModel::class.java
            StatsSection.YEARS -> YearsListViewModel::class.java
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(statsSection.name, viewModelClass)

        setupObservers(activity)
        viewModel.start()
    }

    private fun setupObservers(activity: FragmentActivity) {
        viewModel.uiModel.observe(this, Observer {
            if (it != null) {
                when (it) {
                    is UiModel.Success -> {
                        updateInsights(it.data)
                    }
                    is UiModel.Error -> {
                        recyclerView.visibility = View.GONE
                        actionable_error_view.visibility = View.VISIBLE
                        actionable_error_view.button.setOnClickListener {
                            viewModel.onRetryClick()
                        }
                    }
                }
            }
        })

        viewModel.dateSelectorData.observe(this, Observer { dateSelectorUiModel ->
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

        viewModel.navigationTarget.observeEvent(this) { target ->
            navigator.navigate(activity, target)
            return@observeEvent true
        }

        viewModel.selectedDate.observeEvent(this) {
            viewModel.onDateChanged()
            true
        }

        viewModel.listSelected.observe(this, Observer {
            viewModel.onListSelected()
        })
    }

    private fun updateInsights(statsState: List<StatsBlock>) {
        recyclerView.visibility = View.VISIBLE
        actionable_error_view.visibility = View.GONE
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
