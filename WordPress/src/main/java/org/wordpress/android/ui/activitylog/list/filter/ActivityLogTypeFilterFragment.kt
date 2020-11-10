package org.wordpress.android.ui.activitylog.list.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.wordpress.android.R.layout

class ActivityLogTypeFilterFragment : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout.activity_log_type_filter_fragment, container, false)
    }
}
