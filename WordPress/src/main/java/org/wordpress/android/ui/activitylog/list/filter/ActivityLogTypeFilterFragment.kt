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
import com.google.android.material.appbar.MaterialToolbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.getColorResIdFromAttribute
import javax.inject.Inject

class ActivityLogTypeFilterFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

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
        val view = inflater.inflate(R.layout.activity_log_type_filter_fragment, container, false)
        initToolbar(view)
        return view
    }

    override fun getTheme(): Int {
        return R.style.WordPress_FullscreenDialog
    }

    private fun initToolbar(view: View) {
        val toolbar = view.findViewById(R.id.toolbar_main) as MaterialToolbar
        toolbar.title = "Ahoj"
        toolbar.navigationIcon = ColorUtils.applyTintToDrawable(
                toolbar.context, R.drawable.ic_close_white_24dp,
                toolbar.context.getColorResIdFromAttribute(R.attr.colorOnSurface)
        )
        toolbar.setNavigationContentDescription(R.string.close_dialog_button_desc)
        toolbar.setNavigationOnClickListener { dismiss() }
    }
}
