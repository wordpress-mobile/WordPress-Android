package org.wordpress.android.ui.activitylog.list.filter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_log_type_filter_fragment.*
import kotlinx.android.synthetic.main.progress_layout.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Error
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.FullscreenLoading
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.getColorResIdFromAttribute
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import javax.inject.Inject

private const val ARG_INITIAL_SELECTION = "arg_initial_selection"
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
        initToolbar()
        initRecyclerView()
        initViewModel()
    }

    private fun initToolbar() {
        toolbar_main.navigationIcon = ColorUtils.applyTintToDrawable(
                toolbar_main.context, R.drawable.ic_close_white_24dp,
                toolbar_main.context.getColorResIdFromAttribute(R.attr.colorOnSurface)
        )
        toolbar_main.setNavigationContentDescription(R.string.close_dialog_button_desc)
        toolbar_main.setNavigationOnClickListener { dismiss() }
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        initAdapter()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(ActivityLogTypeFilterViewModel::class.java)

        val parentViewModel = ViewModelProvider(requireParentFragment(), viewModelFactory)
                .get(ActivityLogViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            uiHelpers.updateVisibility(actionable_empty_view, uiState.errorVisibility)
            uiHelpers.updateVisibility(recycler_view, uiState.contentVisibility)
            uiHelpers.updateVisibility(progress_layout, uiState.loadingVisibility)
            refreshMenuItems(uiState)
            when (uiState) {
                is FullscreenLoading -> refreshLoadingScreen(uiState)
                is Error -> refreshErrorScreen(uiState)
                is Content -> refreshContentScreen(uiState)
            }
        })
        viewModel.dismissDialog.observe(viewLifecycleOwner, Observer {
            it.applyIfNotHandled { dismiss() }
        })
        viewModel.start(
                remoteSiteId = RemoteId(requireNotNull(arguments).getLong(WordPress.REMOTE_SITE_ID)),
                initialSelection = requireNotNull(arguments).getStringArray(ARG_INITIAL_SELECTION)?.toList() ?: listOf(),
                parentViewModel = parentViewModel
        )
    }

    private fun refreshLoadingScreen(uiState: FullscreenLoading) {
        uiHelpers.setTextOrHide(progress_text, uiState.loadingText)
    }

    private fun refreshErrorScreen(uiState: Error) {
        uiHelpers.setTextOrHide(actionable_empty_view.title, uiState.errorTitle)
        uiHelpers.setTextOrHide(actionable_empty_view.subtitle, uiState.errorSubtitle)
        uiHelpers.setTextOrHide(actionable_empty_view.button, uiState.errorButtonText)
        actionable_empty_view.button.setOnClickListener { uiState.retryAction.action.invoke() }
    }

    private fun refreshContentScreen(uiState: Content) {
        (recycler_view.adapter as ActivityLogTypeFilterAdapter).update(uiState.items)
    }

    private fun refreshMenuItems(uiState: ActivityLogTypeFilterViewModel.UiState) {
        val menu = toolbar_main.menu
        menu.removeGroup(ACTIONS_MENU_GROUP)

        if (uiState is Content) {
            addMenuItem(uiState.primaryAction, PRIMARY_ACTION_ORDER, showAlways = PRIMARY_ACTION_SHOW_ALWAYS)
            addMenuItem(uiState.secondaryAction, SECONDARY_ACTION_ORDER, showAlways = SECONDARY_ACTION_SHOW_ALWAYS)
        }
    }

    private fun addMenuItem(action: ActivityLogTypeFilterViewModel.Action, order: Int, showAlways: Boolean) {
        val actionLabel = uiHelpers.getTextOfUiString(requireContext(), action.label)
        toolbar_main.menu.add(ACTIONS_MENU_GROUP, Menu.NONE, order, actionLabel).let {
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

    private fun initAdapter() {
        recycler_view.adapter = ActivityLogTypeFilterAdapter(uiHelpers)
    }

    companion object {
        @JvmStatic
        fun newInstance(remoteSiteId: RemoteId, initialSelection: List<String>): ActivityLogTypeFilterFragment {
            val args = Bundle()
            args.putStringArray(ARG_INITIAL_SELECTION, initialSelection.toTypedArray())
            args.putLong(WordPress.REMOTE_SITE_ID, remoteSiteId.value)
            return ActivityLogTypeFilterFragment().apply { arguments = args }
        }
    }
}
