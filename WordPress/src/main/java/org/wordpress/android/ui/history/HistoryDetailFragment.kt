package org.wordpress.android.ui.history

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.widgets.DiffView

class HistoryDetailFragment : Fragment() {
    private var mRevision: Revision? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRevision = if (savedInstanceState != null) {
            savedInstanceState.getParcelable(KEY_REVISION)
        } else {
            arguments?.getParcelable(EXTRA_REVISION)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.history_detail_fragment, container, false) as ViewGroup
        (rootView.findViewById<View>(R.id.title) as DiffView).showDiffs(mRevision!!.titleDiffs, true)
        (rootView.findViewById<View>(R.id.content) as DiffView).showDiffs(mRevision!!.contentDiffs, false)
        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_REVISION, mRevision)
    }

    companion object {
        const val EXTRA_REVISION = "EXTRA_REVISION"
        const val KEY_REVISION = "KEY_REVISION"

        fun newInstance(revision: Revision): HistoryDetailFragment {
            val fragment = HistoryDetailFragment()
            val bundle = Bundle()
            bundle.putParcelable(EXTRA_REVISION, revision)
            fragment.arguments = bundle
            return fragment
        }
    }
}
