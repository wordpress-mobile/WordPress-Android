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
import org.wordpress.android.ui.accounts.login.LoginAnalyticsTracker
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
    lateinit var analyticsTracker: LoginAnalyticsTracker

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
                        errorMessage = errorMessage,
                        onBackClick = {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        },
                        onHelpClick = {
                            analyticsTracker.trackShowHelpClick()
                            loginActivity?.helpSiteAddress()
                        },
                        onContinueClick = { cleanedAddress ->
                            discover(cleanedAddress)
                        },
                        onErrorDismissed = {
                            viewModel.clearError()
                        },
                        onCancelLoading = if (isLoading) {
                            { viewModel.cancelDiscovery() }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.setTitle(R.string.site_address_login_title)
        analyticsTracker.trackUrlFormViewed()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Only ever a URL to navigate to; a failed discovery surfaces through errorMessage.
                viewModel.discoveryURL.collect { url ->
                    activityNavigator.openApplicationPasswordLogin(requireActivity(), url)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        analyticsTracker.trackFailure(it)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.wpComDetected.collect {
                    // WP.com sites can't use application passwords; send them to the OAuth flow.
                    // Discovery already ran here (unlike the up-front WPUrlUtils.isWordPressCom()
                    // check), so record that it resolved to a WordPress.com site.
                    analyticsTracker.trackConnectedSiteInfoSucceeded(mapOf("is_wpcom" to true))
                    loginActivity?.showWPcomLoginScreen(requireContext())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analyticsTracker.siteAddressFormScreenResumed()
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
        analyticsTracker.trackSubmitClicked()
        analyticsTracker.trackConnectedSiteInfoRequested(cleanedUrl)
        viewModel.runApiDiscovery(cleanedUrl)
    }

    companion object {
        const val TAG: String = "login_site_application_password_fragment_tag"
    }
}
