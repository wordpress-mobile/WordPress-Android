package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import android.view.View
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ActivityLogListFragmentBinding
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents.DownloadBackupFile
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents.ShowBackupDownload
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents.ShowRestore
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterFragment
import org.wordpress.android.ui.prefs.EmptyViewRecyclerView
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_REWINDABLE_ONLY_KEY
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.FETCHING
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.LOADING_MORE
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersShown
import org.wordpress.android.viewmodel.activitylog.DateRange
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

private const val ACTIVITY_TYPE_FILTER_TAG = "activity_log_type_filter_tag"
private const val DATE_PICKER_TAG = "activity_log_date_picker_tag"
private const val ACTIVITY_LOG_TRACKING_SOURCE = "activity_log"
private const val BACKUP_TRACKING_SOURCE = "backup"

/**
 * It was decided to reuse the 'Activity Log' screen instead of creating a new 'Backup' screen. This was due to the
 * fact that there will be lots of code that would need to be duplicated for the new 'Backup' screen. On the other
 * hand, not much more complexity would be introduced if the 'Activity Log' screen is reused (mainly some 'if/else'
 * code branches here and there).
 *
 * However, should more 'Backup' related additions are added to the 'Activity Log' screen, then it should become a
 * necessity to split those features in separate screens in order not to increase further the complexity of this
 * screen's architecture.
 */
class ActivityLogListFragment : Fragment(R.layout.activity_log_list_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: ActivityLogViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var listView: EmptyViewRecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component().inject(this@ActivityLogListFragment)
        viewModel = ViewModelProvider(
            this@ActivityLogListFragment,
            viewModelFactory
        ).get(ActivityLogViewModel::class.java)

        with(ActivityLogListFragmentBinding.bind(view)) {
            listView = logListView
            logListView.layoutManager = LinearLayoutManager(nonNullActivity, RecyclerView.VERTICAL, false)

            swipeToRefreshHelper = buildSwipeToRefreshHelper(swipeRefreshLayout) {
                if (NetworkUtils.checkConnection(nonNullActivity)) {
                    viewModel.onPullToRefresh()
                } else {
                    swipeToRefreshHelper.isRefreshing = false
                }
            }

            val site = if (savedInstanceState == null) {
                val nonNullIntent = checkNotNull(nonNullActivity.intent)
                nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
            } else {
                savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
            }
            val rewindableOnly = nonNullActivity.intent.getBooleanExtra(ACTIVITY_LOG_REWINDABLE_ONLY_KEY, false)

            logListView.setEmptyView(actionableEmptyView)
            logListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!recyclerView.canScrollVertically(1) && dy != 0) {
                        viewModel.onScrolledToBottom()
                    }
                }
            })

            setupObservers()

            viewModel.start(site, rewindableOnly)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        with(requireActivity()) {
            findViewById<View>(R.id.activity_type_filter).setOnClickListener { viewModel.onActivityTypeFilterClicked() }
            findViewById<View>(R.id.date_range_picker).setOnClickListener { viewModel.dateRangePickerClicked() }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        restoreDateRangePickerListeners()
    }

    override fun onResume() {
        super.onResume()
        if (activity is ScrollableViewInitializedListener) {
            (activity as ScrollableViewInitializedListener).onScrollableViewInitialized(listView.id)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun restoreDateRangePickerListeners() {
        (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<Pair<Long, Long>>)
            ?.let { initDateRangePickerButtonClickListener(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    fun onQueryRestoreStatus(rewindId: String, restoreId: Long) {
        viewModel.onQueryRestoreStatus(rewindId, restoreId)
    }

    fun onQueryBackupDownloadStatus(rewindId: String, downloadId: Long, actionState: Int) {
        viewModel.onQueryBackupDownloadStatus(rewindId, downloadId, actionState)
    }

    private fun ActivityLogListFragmentBinding.setupObservers() {
        viewModel.events.observe(viewLifecycleOwner, {
            reloadEvents(it ?: emptyList())
        })

        viewModel.eventListStatus.observe(viewLifecycleOwner, { listStatus ->
            refreshProgressBars(listStatus)
        })

        viewModel.filtersUiState.observe(viewLifecycleOwner, { uiState ->
            with(requireActivity()) {
                uiHelpers.updateVisibility(findViewById(R.id.filters_bar), uiState.visibility)
                uiHelpers.updateVisibility(findViewById(R.id.filters_bar_divider), uiState.visibility)
            }
            if (uiState is FiltersShown) updateFilters(uiState)
        })

        viewModel.emptyUiState.observe(viewLifecycleOwner, { emptyState ->
            actionableEmptyView.title.text = uiHelpers.getTextOfUiString(
                requireContext(),
                emptyState.emptyScreenTitle
            )
            actionableEmptyView.subtitle.text = uiHelpers.getTextOfUiString(
                requireContext(),
                emptyState.emptyScreenSubtitle
            )
        })

        viewModel.showActivityTypeFilterDialog.observe(viewLifecycleOwner, { event ->
            showActivityTypeFilterDialog(event.siteId, event.initialSelection, event.dateRange)
        })

        viewModel.showDateRangePicker.observe(viewLifecycleOwner, { event ->
            showDateRangePicker(event.initialSelection)
        })

        viewModel.showItemDetail.observe(viewLifecycleOwner, {
            if (it is ActivityLogListItem.Event) {
                ActivityLauncher.viewActivityLogDetailForResult(
                    activity,
                    viewModel.site,
                    it.activityId,
                    it.isButtonVisible,
                    it.isRestoreHidden,
                    viewModel.rewindableOnly
                )
            }
        })

        viewModel.showSnackbarMessage.observe(viewLifecycleOwner, { message ->
            val parent: View? = activity?.findViewById(android.R.id.content)
            if (message != null && parent != null) {
                WPSnackbar.make(parent, message, Snackbar.LENGTH_LONG).show()
            }
        })

        viewModel.moveToTop.observe(viewLifecycleOwner, {
            logListView.scrollToPosition(0)
        })

        viewModel.navigationEvents.observeEvent(viewLifecycleOwner, {
            navigate(it)
        })
    }

    private fun navigate(events: ActivityLogNavigationEvents) {
        val trackingSource = when {
            requireNotNull(
                requireActivity().intent.extras?.containsKey(ACTIVITY_LOG_REWINDABLE_ONLY_KEY)
            ) ->
                BACKUP_TRACKING_SOURCE
            else -> {
                ACTIVITY_LOG_TRACKING_SOURCE
            }
        }

        when (events) {
            is ShowBackupDownload -> ActivityLauncher.showBackupDownloadForResult(
                requireActivity(),
                viewModel.site,
                events.event.activityId,
                RequestCodes.BACKUP_DOWNLOAD,
                trackingSource
            )
            is ShowRestore -> ActivityLauncher.showRestoreForResult(
                requireActivity(),
                viewModel.site,
                events.event.activityId,
                RequestCodes.RESTORE,
                trackingSource
            )
            is DownloadBackupFile -> ActivityLauncher.downloadBackupDownloadFile(requireActivity(), events.url)
        }
    }

    private fun showDateRangePicker(initialDateRange: DateRange?) {
        val picker = MaterialDatePicker.Builder
            .dateRangePicker()
            .setTheme(R.style.WordPress_MaterialCalendarFullscreenTheme)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                    .build()
            )
            .setSelection(initialDateRange)
            .build()
        initDateRangePickerButtonClickListener(picker)
        picker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    private fun initDateRangePickerButtonClickListener(picker: MaterialDatePicker<Pair<Long, Long>>) {
        picker.addOnPositiveButtonClickListener { viewModel.onDateRangeSelected(it) }
    }

    private fun showActivityTypeFilterDialog(
        remoteSiteId: RemoteId,
        initialSelection: List<String>,
        dateRange: DateRange?
    ) {
        ActivityLogTypeFilterFragment.newInstance(remoteSiteId, initialSelection, dateRange)
            .show(childFragmentManager, ACTIVITY_TYPE_FILTER_TAG)
    }

    private fun updateFilters(uiState: FiltersShown) {
        with(requireActivity().findViewById<Chip>(R.id.date_range_picker)) {
            text = uiHelpers.getTextOfUiString(requireContext(), uiState.dateRangeLabel)
            contentDescription = uiHelpers.getTextOfUiString(requireContext(), uiState.dateRangeLabelContentDescription)
            isCloseIconVisible = uiState.onClearDateRangeFilterClicked != null
            setOnCloseIconClickListener { uiState.onClearDateRangeFilterClicked?.invoke() }
        }

        with(requireActivity().findViewById<Chip>(R.id.activity_type_filter)) {
            text = uiHelpers.getTextOfUiString(requireContext(), uiState.activityTypeLabel)
            contentDescription = uiHelpers
                .getTextOfUiString(requireContext(), uiState.activityTypeLabelContentDescription)
            isCloseIconVisible = uiState.onClearActivityTypeFilterClicked != null
            setOnCloseIconClickListener { uiState.onClearActivityTypeFilterClicked?.invoke() }
        }
    }

    private fun refreshProgressBars(eventListStatus: ActivityLogViewModel.ActivityLogListStatus?) {
        if (!isAdded || view == null) {
            return
        }
        // We want to show the swipe refresher for the initial fetch but not while loading more
        swipeToRefreshHelper.isRefreshing = eventListStatus == FETCHING
        // We want to show the progress bar at the bottom while loading more but not for initial fetch
        val showLoadMore = eventListStatus == LOADING_MORE
        requireActivity().findViewById<View>(R.id.progress)?.visibility = if (showLoadMore) View.VISIBLE else View.GONE
    }

    private fun ActivityLogListFragmentBinding.reloadEvents(data: List<ActivityLogListItem>) {
        setEvents(data)
    }

    private fun onItemClicked(item: ActivityLogListItem) {
        viewModel.onItemClicked(item)
    }

    private fun onSecondaryActionClicked(
        secondaryAction: ActivityLogListItem.SecondaryAction,
        item: ActivityLogListItem
    ): Boolean {
        return viewModel.onSecondaryActionClicked(secondaryAction, item)
    }

    private fun ActivityLogListFragmentBinding.setEvents(events: List<ActivityLogListItem>) {
        val adapter: ActivityLogAdapter
        if (logListView.adapter == null) {
            adapter = ActivityLogAdapter(
                this@ActivityLogListFragment::onItemClicked,
                this@ActivityLogListFragment::onSecondaryActionClicked,
                uiHelpers
            )
            logListView.adapter = adapter
        } else {
            adapter = logListView.adapter as ActivityLogAdapter
        }
        adapter.updateList(events)
    }
}
