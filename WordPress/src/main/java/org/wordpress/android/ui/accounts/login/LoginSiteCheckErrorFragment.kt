package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.FragmentLoginSiteCheckErrorBinding
import org.wordpress.android.login.LoginListener
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step
import javax.inject.Inject

class LoginSiteCheckErrorFragment : Fragment(R.layout.fragment_login_site_check_error) {
    companion object {
        const val TAG = "LoginSiteCheckErrorFragment"
        const val ARG_SITE_ADDRESS = "SITE-ADDRESS"
        const val ARG_ERROR_MESSAGE = "ERROR-MESSAGE"

        fun newInstance(siteAddress: String, errorMsg: String): LoginSiteCheckErrorFragment {
            val fragment = LoginSiteCheckErrorFragment()
            val args = Bundle()
            args.putString(ARG_SITE_ADDRESS, siteAddress)
            args.putString(ARG_ERROR_MESSAGE, errorMsg)
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    private var loginListener: LoginListener? = null
    private var siteAddress: String? = null
    private var errorMsg: String? = null
    private lateinit var viewModel: LoginSiteCheckErrorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            siteAddress = it.getString(ARG_SITE_ADDRESS, null)
            errorMsg = it.getString(ARG_ERROR_MESSAGE, null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDagger()
        initViewModel()
        with(FragmentLoginSiteCheckErrorBinding.bind(view)) {
            initErrorMessageView()
            initButtons()
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

    private fun FragmentLoginSiteCheckErrorBinding.initErrorMessageView() {
        loginEmptyViewMessage.text = errorMsg
    }

    private fun FragmentLoginSiteCheckErrorBinding.initButtons() {
        buttonPrimary.setOnClickListener { viewModel.onSeeInstructionsPressed() }
        buttonSecondary.setOnClickListener { viewModel.onTryAnotherAccountPressed() }
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
        ActivityLauncher.showSignInForResultJetpackOnly(requireActivity(), true)
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
}
