package org.wordpress.android.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.JetpackRemoteInstallFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.CONNECT
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.LOGIN
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.MANUAL_INSTALL
import org.wordpress.android.ui.RequestCodes.JETPACK_LOGIN
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class JetpackRemoteInstallFragment : Fragment(R.layout.jetpack_remote_install_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: JetpackRemoteInstallViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(JetpackRemoteInstallFragmentBinding.bind(view)) {
            initDagger()
            initViewModel(savedInstanceState)
        }
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()!!.inject(this)
    }

    private fun JetpackRemoteInstallFragmentBinding.initViewModel(savedInstanceState: Bundle?) {
        requireActivity().let { activity ->
            val intent = activity.intent
            val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
            val source = intent.getSerializableExtra(TRACKING_SOURCE_KEY) as JetpackConnectionSource
            val retrievedState = savedInstanceState?.getSerializable(VIEW_STATE) as? JetpackRemoteInstallViewState.Type
            viewModel = ViewModelProvider(
                    this@JetpackRemoteInstallFragment, viewModelFactory
            ).get(JetpackRemoteInstallViewModel::class.java)
            viewModel.start(site, retrievedState)

            initLiveViewStateObserver()

            viewModel.liveActionOnResult.observe(viewLifecycleOwner, Observer { result ->
                if (result != null) {
                    when (result.action) {
                        MANUAL_INSTALL -> onManualInstallResultAction(activity, source, result)
                        LOGIN -> onLoginResultAction(activity, source)
                        CONNECT -> onConnectResultAction(activity, source, result)
                    }
                }
            })
        }
    }

    private fun onManualInstallResultAction(
        activity: FragmentActivity,
        source: JetpackConnectionSource,
        result: JetpackResultActionData
    ) {
        JetpackConnectionWebViewActivity.startManualFlow(
                activity,
                source,
                result.site,
                result.loggedIn
        )
        activity.finish()
    }

    @Suppress("DEPRECATION")
    private fun onLoginResultAction(
        activity: FragmentActivity,
        source: JetpackConnectionSource
    ) {
        val loginIntent = Intent(activity, LoginActivity::class.java)
        LoginMode.JETPACK_STATS.putInto(loginIntent)
        loginIntent.putExtra(LoginActivity.ARG_JETPACK_CONNECT_SOURCE, source)
        startActivityForResult(loginIntent, JETPACK_LOGIN)
    }

    private fun onConnectResultAction(
        activity: FragmentActivity,
        source: JetpackConnectionSource,
        result: JetpackResultActionData
    ) {
        JetpackConnectionWebViewActivity.startJetpackConnectionFlow(
                activity,
                source,
                result.site,
                result.loggedIn
        )
        activity.finish()
    }

    private fun JetpackRemoteInstallFragmentBinding.initLiveViewStateObserver() {
        viewModel.liveViewState.observe(viewLifecycleOwner, Observer { viewState ->
            if (viewState != null) {
                if (viewState is JetpackRemoteInstallViewState.Error) {
                    AppLog.e(AppLog.T.JETPACK_REMOTE_INSTALL, "An error occurred while installing Jetpack")
                }
                jetpackInstallIcon.setImageResource(viewState.icon)
                if (viewState.iconTint != null) {
                    jetpackInstallIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(
                            jetpackInstallIcon.context, viewState.iconTint))
                } else {
                    jetpackInstallIcon.imageTintList = null
                }
                jetpackInstallTitle.setText(viewState.titleResource)
                jetpackInstallMessage.setText(viewState.messageResource)
                if (viewState.buttonResource != null) {
                    jetpackInstallButton.visibility = View.VISIBLE
                    jetpackInstallButton.setText(viewState.buttonResource)
                } else {
                    jetpackInstallButton.visibility = View.GONE
                }
                jetpackInstallButton.setOnClickListener { viewState.onClick() }
                jetpackInstallProgress.visibility = if (viewState.progressBarVisible) View.VISIBLE else View.GONE
            }
        })
    }

    @Suppress("DEPRECATION")
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

    companion object {
        const val TRACKING_SOURCE_KEY = "tracking_source_key"
        private const val VIEW_STATE = "view_state_key"
    }
}
