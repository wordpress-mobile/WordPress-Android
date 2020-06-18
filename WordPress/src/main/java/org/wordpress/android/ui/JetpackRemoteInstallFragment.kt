package org.wordpress.android.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.jetpack_remote_install_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.CONNECT
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.LOGIN
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.MANUAL_INSTALL
import org.wordpress.android.ui.RequestCodes.JETPACK_LOGIN
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class JetpackRemoteInstallFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: JetpackRemoteInstallViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component()!!.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().let { activity ->
            viewModel = ViewModelProviders.of(this, viewModelFactory)
                    .get<JetpackRemoteInstallViewModel>(JetpackRemoteInstallViewModel::class.java)

            val intent = activity.intent
            val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
            val source = intent.getSerializableExtra(TRACKING_SOURCE_KEY) as JetpackConnectionSource
            val retrievedState = savedInstanceState?.getSerializable(VIEW_STATE) as? JetpackRemoteInstallViewState.Type
            viewModel.start(site, retrievedState)
            viewModel.liveViewState.observe(viewLifecycleOwner, Observer { viewState ->
                if (viewState != null) {
                    if (viewState is JetpackRemoteInstallViewState.Error) {
                        AppLog.e(AppLog.T.JETPACK_REMOTE_INSTALL, "An error occurred while installing Jetpack")
                    }
                    jetpack_install_icon.setImageResource(viewState.icon)
                    if (viewState.iconTint != null) {
                        jetpack_install_icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(
                                jetpack_install_icon.context, viewState.iconTint))
                    } else {
                        jetpack_install_icon.imageTintList = null
                    }
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
            viewModel.liveActionOnResult.observe(viewLifecycleOwner, Observer { result ->
                if (result != null) {
                    when (result.action) {
                        MANUAL_INSTALL -> {
                            JetpackConnectionWebViewActivity.startManualFlow(
                                    activity,
                                    source,
                                    result.site,
                                    result.loggedIn
                            )
                            activity.finish()
                        }
                        LOGIN -> {
                            val loginIntent = Intent(activity, LoginActivity::class.java)
                            LoginMode.JETPACK_STATS.putInto(loginIntent)
                            loginIntent.putExtra(LoginActivity.ARG_JETPACK_CONNECT_SOURCE, source)
                            startActivityForResult(loginIntent, JETPACK_LOGIN)
                        }
                        CONNECT -> {
                            JetpackConnectionWebViewActivity.startJetpackConnectionFlow(
                                    activity,
                                    source,
                                    result.site,
                                    result.loggedIn
                            )
                            activity.finish()
                        }
                    }
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JETPACK_LOGIN && resultCode == Activity.RESULT_OK) {
            val site = requireActivity().intent!!.getSerializableExtra(WordPress.SITE) as SiteModel
            viewModel.onLogin(site.id)
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
