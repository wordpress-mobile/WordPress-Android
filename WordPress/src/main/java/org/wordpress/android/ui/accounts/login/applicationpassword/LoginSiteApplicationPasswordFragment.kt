package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.ui.accounts.login.LoginAnalyticsListener
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPUrlUtils
import javax.inject.Inject

@AndroidEntryPoint
class LoginSiteApplicationPasswordFragment : Fragment() {
    private var loginActivity: LoginActivity? = null

    private val viewModel: LoginSiteApplicationPasswordViewModel by viewModels()

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var analyticsListener: LoginAnalyticsListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is LoginActivity) { "$context must be LoginActivity" }
        loginActivity = context
    }

    override fun onDetach() {
        super.onDetach()
        loginActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val isLoading by viewModel.loadingStateFlow.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                AppThemeM3 {
                    LoginSiteApplicationPasswordScreen(
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onBackClick = {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        },
                        onHelpClick = { cleanedAddress ->
                            analyticsListener.trackShowHelpClick()
                            loginActivity?.helpSiteAddress(cleanedAddress)
                        },
                        onContinueClick = { cleanedAddress ->
                            discover(cleanedAddress)
                        },
                        onErrorDismissed = {
                            viewModel.clearError()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.setTitle(R.string.site_address_login_title)
        analyticsListener.trackUrlFormViewed()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.discoveryURL.collect { url ->
                    if (url.isEmpty()) {
                        viewModel.setError(getString(R.string.application_password_not_supported_error))
                    } else {
                        activityNavigator.openApplicationPasswordLogin(requireActivity(), url)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        analyticsListener.trackFailure(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analyticsListener.siteAddressFormScreenResumed()
    }

    private fun discover(cleanedUrl: String) {
        if (!NetworkUtils.checkConnection(activity)) {
            return
        }
        // WP.com sites should use the OAuth flow, not application passwords
        val urlWithScheme = UrlUtils.addUrlSchemeIfNeeded(cleanedUrl, true)
        if (WPUrlUtils.isWordPressCom(urlWithScheme)) {
            loginActivity?.showWPcomLoginScreen(requireContext())
            return
        }
        analyticsListener.trackSubmitClicked()
        analyticsListener.trackConnectedSiteInfoRequested(cleanedUrl)
        viewModel.runApiDiscovery(cleanedUrl)
    }

    companion object {
        const val TAG: String = "login_site_application_password_fragment_tag"
    }
}
