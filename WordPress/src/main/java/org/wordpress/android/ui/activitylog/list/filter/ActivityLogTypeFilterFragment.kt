package org.wordpress.android.ui.activitylog.list.filter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ActivityLogTypeFilterFragmentBinding
import org.wordpress.android.databinding.ProgressLayoutBinding
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Error
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.FullscreenLoading
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.extensions.getColorResIdFromAttribute
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import org.wordpress.android.viewmodel.activitylog.DateRange
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

private const val ARG_INITIAL_SELECTION = "arg_initial_selection"
private const val ARG_DATE_RANGE_AFTER = "arg_date_range_after"
private const val ARG_DATE_RANGE_BEFORE = "arg_date_range_before"
private const val ACTIONS_MENU_GROUP = 1

/**
 * Show the primary action closer to user's finger.
 */
private const val PRIMARY_ACTION_ORDER = 2
private const val SECONDARY_ACTION_ORDER = 1

/**
 * Always show the primary action no matter the screen size.
 */
private const val PRIMARY_ACTION_SHOW_ALWAYS = true
private const val SECONDARY_ACTION_SHOW_ALWAYS = false

class ActivityLogTypeFilterFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: ActivityLogTypeFilterViewModel

    override fun getTheme(): Int = R.style.WordPress_FullscreenDialog

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_log_type_filter_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(ActivityLogTypeFilterFragmentBinding.bind(view)) {
            initToolbar()
            initRecyclerView()
            initViewModel()
        }
    }

    private fun ActivityLogTypeFilterFragmentBinding.initToolbar() {
        toolbarMain.navigationIcon = ColorUtils.applyTintToDrawable(
                toolbarMain.context, R.drawable.ic_close_white_24dp,
                toolbarMain.context.getColorResIdFromAttribute(R.attr.colorOnSurface)
        )
        toolbarMain.setNavigationContentDescription(R.string.close_dialog_button_desc)
        toolbarMain.setNavigationOnClickListener { dismiss() }
    }

    private fun ActivityLogTypeFilterFragmentBinding.initRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        initAdapter()
    }

    @Suppress("UseCheckOrError")
    private fun ActivityLogTypeFilterFragmentBinding.initViewModel() {
        viewModel = ViewModelProvider(this@ActivityLogTypeFilterFragment, viewModelFactory)
                .get(ActivityLogTypeFilterViewModel::class.java)

        val parentViewModel = ViewModelProvider(requireParentFragment(), viewModelFactory)
                .get(ActivityLogViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
            uiHelpers.updateVisibility(actionableEmptyView, uiState.errorVisibility)
            uiHelpers.updateVisibility(recyclerView, uiState.contentVisibility)
            uiHelpers.updateVisibility(progress.root, uiState.loadingVisibility)
            refreshMenuItems(uiState)
            when (uiState) {
                is FullscreenLoading -> progress.refreshLoadingScreen(uiState)
                is Error -> refreshErrorScreen(uiState)
                is Content -> refreshContentScreen(uiState)
            }
        })
        viewModel.dismissDialog.observeEvent(viewLifecycleOwner, {
            dismiss()
        })

        val afterDateRangeAvailable = requireNotNull(arguments).containsKey(ARG_DATE_RANGE_AFTER)
        val beforeDateRangeAvailable = requireNotNull(arguments).containsKey(ARG_DATE_RANGE_BEFORE)
        val dateRange: DateRange? = if (afterDateRangeAvailable && beforeDateRangeAvailable) {
            val after = requireNotNull(arguments).getLong(ARG_DATE_RANGE_AFTER)
            val before = requireNotNull(arguments).getLong(ARG_DATE_RANGE_BEFORE)
            Pair(after, before)
        } else if (afterDateRangeAvailable || beforeDateRangeAvailable) {
            throw IllegalStateException("DateRange is missing after or before date")
        } else {
            null
        }
        viewModel.start(
                remoteSiteId = RemoteId(requireNotNull(arguments).getLong(WordPress.REMOTE_SITE_ID)),
                initialSelection = requireNotNull(arguments).getStringArray(ARG_INITIAL_SELECTION)?.toList()
                        ?: listOf(),
                dateRange = dateRange,
                parentViewModel = parentViewModel
        )
    }

    private fun ProgressLayoutBinding.refreshLoadingScreen(uiState: FullscreenLoading) {
        uiHelpers.setTextOrHide(progressText, uiState.loadingText)
    }

    private fun ActivityLogTypeFilterFragmentBinding.refreshErrorScreen(uiState: Error) {
        actionableEmptyView.image.setImageResource(uiState.image)
        uiHelpers.setTextOrHide(actionableEmptyView.title, uiState.title)
        uiHelpers.setTextOrHide(actionableEmptyView.subtitle, uiState.subtitle)
        uiHelpers.setTextOrHide(actionableEmptyView.button, uiState.buttonText)
        actionableEmptyView.button.setOnClickListener { uiState.retryAction?.action?.invoke() }
    }

    private fun ActivityLogTypeFilterFragmentBinding.refreshContentScreen(uiState: Content) {
        (recyclerView.adapter as ActivityLogTypeFilterAdapter).update(uiState.items)
    }

    private fun ActivityLogTypeFilterFragmentBinding.refreshMenuItems(uiState: ActivityLogTypeFilterViewModel.UiState) {
        val menu = toolbarMain.menu
        menu.removeGroup(ACTIONS_MENU_GROUP)

        if (uiState is Content) {
            addMenuItem(uiState.primaryAction, PRIMARY_ACTION_ORDER, showAlways = PRIMARY_ACTION_SHOW_ALWAYS)
            addMenuItem(uiState.secondaryAction, SECONDARY_ACTION_ORDER, showAlways = SECONDARY_ACTION_SHOW_ALWAYS)
        }
    }

    private fun ActivityLogTypeFilterFragmentBinding.addMenuItem(
        action: ActivityLogTypeFilterViewModel.Action,
        order: Int,
        showAlways: Boolean
    ) {
        val actionLabel = uiHelpers.getTextOfUiString(requireContext(), action.label)
        toolbarMain.menu.add(ACTIONS_MENU_GROUP, Menu.NONE, order, actionLabel).let {
            if (showAlways) {
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            } else {
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            it.setOnMenuItemClickListener {
                action.action.invoke()
                true
            }
        }
    }

    private fun ActivityLogTypeFilterFragmentBinding.initAdapter() {
        recyclerView.adapter = ActivityLogTypeFilterAdapter(uiHelpers)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            remoteSiteId: RemoteId,
            initialSelection: List<String>,
            dateRange: DateRange?
        ): ActivityLogTypeFilterFragment {
            val args = Bundle()
            args.putStringArray(ARG_INITIAL_SELECTION, initialSelection.toTypedArray())
            dateRange?.first?.let { args.putLong(ARG_DATE_RANGE_AFTER, it) }
            dateRange?.second?.let { args.putLong(ARG_DATE_RANGE_BEFORE, it) }
            args.putLong(WordPress.REMOTE_SITE_ID, remoteSiteId.value)
            return ActivityLogTypeFilterFragment().apply { arguments = args }
        }
    }
}
