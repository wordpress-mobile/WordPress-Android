package org.wordpress.android.ui.activitylog.list.filter

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_log_type_filter_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.getColorResIdFromAttribute
import javax.inject.Inject

class ActivityLogTypeFilterFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: ActivityLogTypeFilterViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(ActivityLogTypeFilterViewModel::class.java)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_log_type_filter_fragment, container, false)
    }

    override fun getTheme(): Int {
        return R.style.WordPress_FullscreenDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(view)
        initRecyclerView()
        viewModel.start(remoteSiteId = RemoteId(requireNotNull(arguments).getLong(WordPress.REMOTE_SITE_ID)))
    }

    private fun initToolbar(view: View) {
        val toolbar = view.findViewById(R.id.toolbar_main) as MaterialToolbar
        toolbar.navigationIcon = ColorUtils.applyTintToDrawable(
                toolbar.context, R.drawable.ic_close_white_24dp,
                toolbar.context.getColorResIdFromAttribute(R.attr.colorOnSurface)
        )
        toolbar.setNavigationContentDescription(R.string.close_dialog_button_desc)
        toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        initAdapter()
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
