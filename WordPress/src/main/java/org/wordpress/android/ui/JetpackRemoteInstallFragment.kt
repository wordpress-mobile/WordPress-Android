package org.wordpress.android.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_remote_install_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState
import javax.inject.Inject

class JetpackRemoteInstallFragment : Fragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: JetpackRemoteInstallViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as WordPress).component()?.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.let { activity ->
            viewModel = ViewModelProviders.of(activity, viewModelFactory)
                    .get<JetpackRemoteInstallViewModel>(JetpackRemoteInstallViewModel::class.java)

            val intent = activity.intent
            val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
            val source = intent.getSerializableExtra(TRACKING_SOURCE_KEY) as JetpackConnectionSource
            val retrievedState = savedInstanceState?.getSerializable(VIEW_STATE) as? ViewState.Type
            viewModel.start(site, retrievedState)
            viewModel.liveViewState.observe(this, Observer { viewState ->
                if (viewState != null) {
                    jetpack_install_icon.setImageResource(viewState.icon)
                    jetpack_install_title.setText(viewState.titleResource)
                    jetpack_install_message.setText(viewState.messageResource)
                    if (viewState.buttonResource != null) {
                        jetpack_install_button.visibility = View.VISIBLE
                        jetpack_install_button.setText(viewState.buttonResource)
                    } else {
                        jetpack_install_button.visibility = View.GONE
                    }
                    jetpack_install_button.setOnClickListener { viewState.onClick() }
                    jetpack_install_progress.visibility = if (viewState.progressBarVisible) View.VISIBLE else View.GONE
                }
            })
            viewModel.liveJetpackConnectionFlow.observe(this, Observer { result ->
                if (result != null) {
                    JetpackConnectionWebViewActivity.startJetpackConnectionFlow(
                            activity,
                            source,
                            result.site,
                            result.hasAccessToken
                    )
                }
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.liveViewState.value?.type?.let {
            outState.putSerializable(VIEW_STATE, it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.jetpack_remote_install_fragment, container, false)
    }

    companion object {
        const val TRACKING_SOURCE_KEY = "tracking_source_key"
        private const val VIEW_STATE = "view_state_key"
    }
}
