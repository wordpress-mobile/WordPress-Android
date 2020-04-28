package org.wordpress.android.ui.whatsnew

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.R
import org.wordpress.android.WordPress
import javax.inject.Inject

class FeatureAnnouncementDialogFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: FeatureAnnouncementViewModel

    companion object {
        const val TAG = "FEATURE_ANNOUNCEMENT_DIALOG_FRAGMENT"
    }

    override fun getTheme(): Int {
        return R.style.FeatureAnnouncementDialogFragment
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(FeatureAnnouncementViewModel::class.java)


        viewModel.start()
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.feature_announcement_dialog_fragment, container, false)

        val progressIndicator = view.findViewById<View>(R.id.feature_list_progress_container)
        val versionLabel = view.findViewById<TextView>(R.id.feature_announcement_version_label)

        val closeButton = view.findViewById<View>(R.id.close_feature_announcement_button)
        closeButton.setOnClickListener { viewModel.onCloseDialogButtonPressed() }

        viewModel.uiModel.observe(this, Observer {
            it?.let { uiModel ->
                progressIndicator.visibility = if (uiModel.isProgressVisible) View.VISIBLE else View.GONE
                versionLabel.text = getString(R.string.version_with_name_param, uiModel.appVersion)
            }
        })

        viewModel.onDialogClosed.observe(this, Observer {
            dismiss()
        })

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity!!.applicationContext as WordPress).component().inject(this)
    }
}
