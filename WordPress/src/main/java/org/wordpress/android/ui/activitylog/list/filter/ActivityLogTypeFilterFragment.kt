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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(ActivityLogTypeFilterViewModel::class.java)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_log_type_filter_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(view)
        initRecyclerView()
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

    private fun initAdapter() {
        recycler_view.adapter = ActivityLogTypeFilterAdapter(uiHelpers)
    }
}
