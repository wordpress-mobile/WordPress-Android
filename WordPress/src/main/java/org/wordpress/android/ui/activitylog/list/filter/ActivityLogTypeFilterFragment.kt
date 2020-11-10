package org.wordpress.android.ui.activitylog.list.filter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.wordpress.android.R.layout
import org.wordpress.android.WordPress

class ActivityLogTypeFilterFragment : DialogFragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout.activity_log_type_filter_fragment, container, false)
    }
}
