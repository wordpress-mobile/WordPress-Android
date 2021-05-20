package org.wordpress.android.ui.accounts.login.jetpack

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.JetpackLoginEmptyViewBinding
import org.wordpress.android.login.LoginListener
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step
import org.wordpress.android.ui.utils.HtmlMessageUtils
import javax.inject.Inject

@Suppress("TooManyFunctions")
class LoginSiteCheckErrorFragment : Fragment(R.layout.jetpack_login_empty_view) {
    companion object {
        const val TAG = "LoginSiteCheckErrorFragment"
        const val ARG_SITE_ADDRESS = "SITE-ADDRESS"

        fun newInstance(siteAddress: String): LoginSiteCheckErrorFragment {
            val fragment = LoginSiteCheckErrorFragment()
            val args = Bundle()
            args.putString(ARG_SITE_ADDRESS, siteAddress)
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    @Inject lateinit var htmlMessageUtils: HtmlMessageUtils
    private var loginListener: LoginListener? = null
    private var siteAddress: String? = null
    private lateinit var viewModel: LoginSiteCheckErrorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            siteAddress = it.getString(ARG_SITE_ADDRESS, null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDagger()
        initBackPressHandler()
        initViewModel()
        with(JetpackLoginEmptyViewBinding.bind(view)) {
            initErrorMessageView()
            initClickListeners()
        }
        initObservers()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this@LoginSiteCheckErrorFragment, viewModelFactory)
                .get(LoginSiteCheckErrorViewModel::class.java)
    }

    private fun JetpackLoginEmptyViewBinding.initErrorMessageView() {
        loginErrorMessageText.text = htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                R.string.login_not_a_jetpack_site,
                "<b>$siteAddress</b>"
        )
    }

    private fun JetpackLoginEmptyViewBinding.initClickListeners() {
        bottomButtonsContainer.buttonPrimary.setOnClickListener { viewModel.onSeeInstructionsPressed() }
        bottomButtonsContainer.buttonSecondary.setOnClickListener { viewModel.onTryAnotherAccountPressed() }
    }

    private fun initObservers() {
        viewModel.navigationEvents.observe(viewLifecycleOwner, { events ->
            events.getContentIfNotHandled()?.let {
                when (it) {
                    is ShowSignInForResultJetpackOnly -> showSignInForResultJetpackOnly()
                    is ShowInstructions -> showInstructions(it.url)
                    else -> { // no op
                    }
                }
            }
        })
    }

    private fun showSignInForResultJetpackOnly() {
        ActivityLauncher.showSignInForResultJetpackOnly(requireActivity())
    }

    private fun showInstructions(url: String) {
        ActivityLauncher.openUrlExternal(requireContext(), url)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // this will throw if parent activity doesn't implement the login listener interface
        loginListener = context as? LoginListener
    }

    override fun onDetach() {
        loginListener = null
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()

        unifiedLoginTracker.track(step = Step.NOT_A_JETPACK_SITE)
    }

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(
                        true
                ) {
                    override fun handleOnBackPressed() {
                        viewModel.onBackPressed()
                    }
                })
    }
}
