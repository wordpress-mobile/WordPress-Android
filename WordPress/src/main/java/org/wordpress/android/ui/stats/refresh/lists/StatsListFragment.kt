package org.wordpress.android.ui.stats.refresh.lists

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.android.synthetic.main.stats_date_selector.*
import kotlinx.android.synthetic.main.stats_empty_view.*
import kotlinx.android.synthetic.main.stats_error_view.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.detail.DetailListViewModel
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsNavigator
import org.wordpress.android.ui.stats.refresh.utils.drawDateSelector
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.setVisible
import javax.inject.Inject

class StatsListFragment : ViewPagerFragment() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
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
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
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

        statsEmptyView.button.setOnClickListener {
            viewModel.onEmptyInsightsButtonClicked()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1) && dy != 0) {
                    viewModel.onScrolledToBottom()
                }
            }
        })

        nextDateButton.setOnClickListener {
            viewModel.onNextDateSelected()
        }

        previousDateButton.setOnClickListener {
            viewModel.onPreviousDateSelected()
        }

        statsErrorView.button.setOnClickListener {
            viewModel.onRetryClick()
        }
    }

    override fun getScrollableViewForUniqueIdProvision(): View? {
       return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity)
    }

    private fun initializeViewModels(activity: FragmentActivity) {
        val statsSection = arguments?.getSerializable(LIST_TYPE) as? StatsSection
                ?: activity.intent?.getSerializableExtra(LIST_TYPE) as? StatsSection
                ?: StatsSection.INSIGHTS

        val viewModelClass = when (statsSection) {
            StatsSection.DETAIL -> DetailListViewModel::class.java
            StatsSection.ANNUAL_STATS,
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
        viewModel.uiModel.observe(viewLifecycleOwner, Observer {
            when (it) {
                is UiModel.Success -> {
                    updateInsights(it.data)
                }
                is UiModel.Error, null -> {
                    recyclerView.visibility = View.GONE
                    statsErrorView.visibility = View.VISIBLE
                    statsEmptyView.visibility = View.GONE
                }
                is UiModel.Empty -> {
                    recyclerView.visibility = View.GONE
                    statsEmptyView.visibility = View.VISIBLE
                    statsErrorView.visibility = View.GONE
                    statsEmptyView.title.setText(it.title)
                    if (it.subtitle != null) {
                        statsEmptyView.subtitle.setText(it.subtitle)
                    } else {
                        statsEmptyView.subtitle.text = ""
                    }
                    if (it.image != null) {
                        statsEmptyView.image.setImageResource(it.image)
                    } else {
                        statsEmptyView.image.setImageDrawable(null)
                    }
                    statsEmptyView.button.setVisible(it.showButton)
                }
            }
        })

        viewModel.dateSelectorData.observe(viewLifecycleOwner, Observer { dateSelectorUiModel ->
            drawDateSelector(dateSelectorUiModel)
        })

        viewModel.navigationTarget.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let { target ->
                navigator.navigate(activity, target)
            }
        })

        viewModel.selectedDate.observe(viewLifecycleOwner, Observer { event ->
            if (event != null) {
                viewModel.onDateChanged(event.selectedSection)
            }
        })

        viewModel.listSelected.observe(viewLifecycleOwner, Observer {
            viewModel.onListSelected()
        })

        viewModel.typesChanged.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                viewModel.onTypesChanged()
            }
        })

        viewModel.scrollTo?.observe(viewLifecycleOwner, Observer { event ->
            if (event != null) {
                (recyclerView.adapter as? StatsBlockAdapter)?.let { adapter ->
                    event.getContentIfNotHandled()?.let { statsType ->
                        recyclerView.smoothScrollToPosition(adapter.positionOf(statsType))
                    }
                }
            }
        })
    }

    private fun updateInsights(statsState: List<StatsBlock>) {
        recyclerView.visibility = View.VISIBLE
        statsErrorView.visibility = View.GONE
        statsEmptyView.visibility = View.GONE

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
