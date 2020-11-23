package org.wordpress.android.ui.activitylog.list.filter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
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
import javax.inject.Inject

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
        initToolbar(view)
        initRecyclerView()
        initViewModel()
    }

    private fun initToolbar(view: View) {
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
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(ActivityLogTypeFilterViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            uiHelpers.updateVisibility(actionable_empty_view, uiState.errorVisibility)
            uiHelpers.updateVisibility(recycler_view, uiState.contentVisibility)
            uiHelpers.updateVisibility(progress_layout, uiState.loadingVisibility)
            when (uiState) {
                is FullscreenLoading -> refreshLoadingScreen(uiState)
                is Error -> refreshErrorScreen(uiState)
                is Content -> refreshContentScreen(uiState)
            }
        })
        viewModel.start(remoteSiteId = RemoteId(requireNotNull(arguments).getLong(WordPress.REMOTE_SITE_ID)))
    }

    private fun refreshLoadingScreen(uiState: FullscreenLoading) {
        uiHelpers.setTextOrHide(progress_text, uiState.loadingText)
    }

    private fun refreshErrorScreen(uiState: Error) {
        TODO("Not yet implemented")
    }

    private fun refreshContentScreen(uiState: Content) {
        TODO("Not yet implemented")
    }

    private fun initAdapter() {
        recycler_view.adapter = ActivityLogTypeFilterAdapter(uiHelpers)
    }

    companion object {
        @JvmStatic
        fun newInstance(remoteSiteId: RemoteId): ActivityLogTypeFilterFragment {
            val args = Bundle()
            args.putLong(WordPress.REMOTE_SITE_ID, remoteSiteId.value)
            return ActivityLogTypeFilterFragment().apply { arguments = args }
        }
    }
}
