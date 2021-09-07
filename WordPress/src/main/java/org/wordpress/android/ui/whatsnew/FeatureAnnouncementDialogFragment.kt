package org.wordpress.android.ui.whatsnew

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.WordPress
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.isDarkTheme
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

        val window: Window? = dialog.window
        window?.let {
            window.statusBarColor = dialog.context.getColorFromAttribute(attr.colorSurface)
            if (!resources.configuration.isDarkTheme()) {
                window.decorView.systemUiVisibility = window.decorView
                        .systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
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
