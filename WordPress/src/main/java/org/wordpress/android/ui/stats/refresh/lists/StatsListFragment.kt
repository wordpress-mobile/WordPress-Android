package org.wordpress.android.ui.stats.refresh.lists

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.StatsListFragmentBinding
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel.Empty
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel.Success
import org.wordpress.android.ui.stats.refresh.lists.detail.DetailListViewModel
import org.wordpress.android.ui.stats.refresh.utils.SelectedTrafficGranularityManager
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsNavigator
import org.wordpress.android.ui.stats.refresh.utils.drawDateSelector
import org.wordpress.android.ui.stats.refresh.utils.toNameResource
import org.wordpress.android.util.config.StatsTrafficSubscribersTabsFeatureConfig
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
class StatsListFragment : ViewPagerFragment(R.layout.stats_list_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var statsDateFormatter: StatsDateFormatter

    @Inject
    lateinit var navigator: StatsNavigator

    @Inject
    lateinit var statsTrafficSubscribersTabsFeatureConfig: StatsTrafficSubscribersTabsFeatureConfig

    @Inject
    lateinit var selectedTrafficGranularityManager: SelectedTrafficGranularityManager

    private lateinit var viewModel: StatsListViewModel
    private lateinit var statsSection: StatsSection

    private var layoutManager: LayoutManager? = null
    private var binding: StatsListFragmentBinding? = null

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

        statsSection = arguments?.getSerializableCompat(LIST_TYPE)
            ?: activity?.intent?.getSerializableExtraCompat(LIST_TYPE)
                    ?: StatsSection.INSIGHTS
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }
        (activity?.intent?.getSerializableExtraCompat<StatsSection>(LIST_TYPE))?.let { sectionFromIntent ->
            outState.putSerializable(LIST_TYPE, sectionFromIntent)
        }
        super.onSaveInstanceState(outState)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.stats_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_new_stats_card -> {
                viewModel.onAddNewStatsButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun StatsListFragmentBinding.initializeViews(savedInstanceState: Bundle?) {
        val columns = resources.getInteger(R.integer.stats_number_of_columns)
        val layoutManager: LayoutManager = if (columns == 1) {
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        } else {
            StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        }
        savedInstanceState?.getParcelableCompat<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        this@StatsListFragment.layoutManager = layoutManager
        this.recyclerView.tag = statsSection.name
        recyclerView.layoutManager = this@StatsListFragment.layoutManager
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

        emptyView.statsEmptyView.button.setOnClickListener {
            viewModel.onEmptyInsightsButtonClicked()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1) && dy != 0) {
                    viewModel.onScrolledToBottom()
                }
            }
        })

        if (statsTrafficSubscribersTabsFeatureConfig.isEnabled()) {
            dateSelector.granularitySpinner.adapter = ArrayAdapter(
                requireContext(),
                R.layout.filter_spinner_item,
                StatsGranularity.entries.map { getString(it.toNameResource()) }
            ).apply { setDropDownViewResource(R.layout.toolbar_spinner_dropdown_item) }

            dateSelector.granularitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    with(StatsGranularity.entries[position]) {
                        selectedTrafficGranularityManager.setSelectedTrafficGranularity(this)
                    }
                }

                @Suppress("EmptyFunctionBlock")
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        dateSelector.nextDateButton.setOnClickListener {
            viewModel.onNextDateSelected()
        }

        dateSelector.previousDateButton.setOnClickListener {
            viewModel.onPreviousDateSelected()
        }

        errorView.statsErrorView.button.setOnClickListener {
            viewModel.onRetryClick()
        }
    }

    override fun getScrollableViewForUniqueIdProvision(): View {
        return binding!!.recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        with(StatsListFragmentBinding.bind(view)) {
            binding = this
            pageContainer.layoutTransition.setAnimateParentHierarchy(false)
            initializeViews(savedInstanceState)
            initializeViewModels(nonNullActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        setHasOptionsMenu(statsSection == StatsSection.INSIGHTS)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun StatsListFragmentBinding.initializeViewModels(activity: FragmentActivity) {
        val viewModelClass = when (statsSection) {
            StatsSection.DETAIL -> DetailListViewModel::class.java
            StatsSection.INSIGHT_DETAIL -> InsightsDetailListViewModel::class.java
            StatsSection.TOTAL_LIKES_DETAIL -> TotalLikesDetailListViewModel::class.java
            StatsSection.TOTAL_COMMENTS_DETAIL -> TotalCommentsDetailListViewModel::class.java
            StatsSection.TOTAL_FOLLOWERS_DETAIL -> TotalFollowersDetailListViewModel::class.java
            StatsSection.TRAFFIC -> TrafficListViewModel::class.java
            StatsSection.ANNUAL_STATS,
            StatsSection.INSIGHTS -> InsightsListViewModel::class.java
            StatsSection.SUBSCRIBERS -> SubscribersListViewModel::class.java
            StatsSection.DAYS -> DaysListViewModel::class.java
            StatsSection.WEEKS -> WeeksListViewModel::class.java
            StatsSection.MONTHS -> MonthsListViewModel::class.java
            StatsSection.YEARS -> YearsListViewModel::class.java
        }

        viewModel = ViewModelProvider(this@StatsListFragment, viewModelFactory)[statsSection.name, viewModelClass]

        setupObservers(activity)
        viewModel.start()
    }

    private fun StatsListFragmentBinding.setupObservers(activity: FragmentActivity) {
        viewModel.uiSourceRemoved.observe(viewLifecycleOwner) {
            viewModel.uiModel.removeObservers(viewLifecycleOwner)
            viewModel.navigationTarget.removeObservers(viewLifecycleOwner)
            viewModel.listSelected.removeObservers(viewLifecycleOwner)
        }

        viewModel.uiSourceAdded.observe(viewLifecycleOwner) {
            observeUiChanges(activity)
        }

        viewModel.dateSelectorData.observe(viewLifecycleOwner) { dateSelectorUiModel ->
            when (statsSection) {
                StatsSection.TOTAL_COMMENTS_DETAIL, StatsSection.TOTAL_FOLLOWERS_DETAIL -> {
                    drawDateSelector(DateSelectorUiModel(false))
                }
                else -> drawDateSelector(dateSelectorUiModel)
            }
        }

        viewModel.selectedDate?.observe(viewLifecycleOwner) { event ->
            if (event != null) {
                viewModel.onDateChanged(event.selectedGranularity)
            }
        }

        viewModel.typesChanged.observeEvent(viewLifecycleOwner) {
            viewModel.onTypesChanged()
        }

        viewModel.scrollTo?.observeEvent(viewLifecycleOwner) { statsType ->
            (recyclerView.adapter as? StatsBlockAdapter)?.let { adapter ->
                recyclerView.smoothScrollToPosition(adapter.positionOf(statsType))
            }
        }

        selectedTrafficGranularityManager.liveSelectedGranularity.observe(viewLifecycleOwner) {
            // Manage the logic of granularity selection in the viewmodel
            (viewModel as? TrafficListViewModel)?.onGranularitySelected(it)

            // Manage the UI update of the new granularity selection
            val selectedGranularityItemPos = StatsGranularity.entries.indexOf(
                selectedTrafficGranularityManager.getSelectedTrafficGranularity()
            )
            dateSelector.granularitySpinner.setSelection(selectedGranularityItemPos)

            recyclerView.scrollToPosition(0)
        }
    }

    private fun StatsListFragmentBinding.observeUiChanges(activity: FragmentActivity) {
        viewModel.uiModel.observe(viewLifecycleOwner) {
            showUiModel(it)
        }

        viewModel.navigationTarget.observeEvent(viewLifecycleOwner) { target -> navigator.navigate(activity, target) }

        viewModel.listSelected.observe(viewLifecycleOwner) { viewModel.onListSelected() }
    }

    private fun StatsListFragmentBinding.showUiModel(
        it: UiModel?
    ) {
        when (it) {
            is Success -> {
                updateInsights(it.data)
            }
            is Error, null -> {
                recyclerView.isGone = true
                emptyView.statsEmptyView.isGone = true
                errorView.statsErrorView.isVisible = true
            }
            is Empty -> {
                recyclerView.isInvisible = true
                errorView.statsErrorView.isGone = true
                emptyView.statsEmptyView.isVisible = true
                emptyView.statsEmptyView.title.setText(it.title)
                if (it.subtitle != null) {
                    emptyView.statsEmptyView.subtitle.setText(it.subtitle)
                } else {
                    emptyView.statsEmptyView.subtitle.text = ""
                }
                if (it.image != null) {
                    emptyView.statsEmptyView.image.setImageResource(it.image)
                } else {
                    emptyView.statsEmptyView.image.setImageDrawable(null)
                }
                emptyView.statsEmptyView.button.setVisible(it.showButton)
            }
        }
    }

    private fun StatsListFragmentBinding.updateInsights(statsState: List<StatsBlock>) {
        val adapter: StatsBlockAdapter
        if (recyclerView.adapter == null) {
            adapter = StatsBlockAdapter(imageManager)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as StatsBlockAdapter
        }

        val layoutManager = recyclerView.layoutManager
        val recyclerViewState = layoutManager?.onSaveInstanceState()
        adapter.update(statsState)

        errorView.statsErrorView.isGone = true
        emptyView.statsEmptyView.isGone = true
        recyclerView.isVisible = true

        layoutManager?.onRestoreInstanceState(recyclerViewState)
    }
}
