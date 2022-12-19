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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.util.extensions.setStatusBarAsSurfaceColor
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
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(FeatureAnnouncementViewModel::class.java)
        dialog.setStatusBarAsSurfaceColor()
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.feature_announcement_dialog_fragment, container, false)

        val progressIndicator = view.findViewById<View>(R.id.feature_list_progress_container)
        val versionLabel = view.findViewById<TextView>(R.id.feature_announcement_version_label)

        val closeButton = view.findViewById<View>(R.id.close_feature_announcement_button)
        closeButton.setOnClickListener { viewModel.onCloseDialogButtonPressed() }

        val recyclerView = view.findViewById<RecyclerView>(R.id.feature_list)
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        val featureAdapter = FeatureAnnouncementListAdapter(this)
        recyclerView.adapter = featureAdapter

        val titleTextView = view.findViewById<TextView>(R.id.feature_announcement_dialog_label)
        val appName = getString(R.string.app_name)
        val title = getString(R.string.feature_announcement_dialog_label, appName)
        titleTextView.text = title

        viewModel.uiModel.observe(this, Observer {
            it?.let { uiModel ->
                progressIndicator.visibility = if (uiModel.isProgressVisible) View.VISIBLE else View.GONE
                versionLabel.text = getString(R.string.version_with_name_param, uiModel.appVersion)
                featureAdapter.toggleFooterVisibility(uiModel.isFindOutMoreVisible)
            }
        })

        viewModel.onDialogClosed.observe(this, Observer {
            dismiss()
        })

        viewModel.onAnnouncementDetailsRequested.observe(this, Observer { detailsUrl ->
            WPWebViewActivity.openURL(context, detailsUrl)
        })

        viewModel.featureItems.observe(this, Observer { featureItems ->
            featureAdapter.updateList(featureItems)
        })

        viewModel.start()
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onSessionPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onSessionEnded()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onSessionStarted()
    }
}
