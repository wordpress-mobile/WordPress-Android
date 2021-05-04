package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.FragmentLoginNoSitesErrorBinding
import org.wordpress.android.login.LoginListener
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import javax.inject.Inject

@Suppress("TooManyFunctions")
class LoginNoSitesErrorFragment : Fragment(R.layout.fragment_login_no_sites_error) {
    companion object {
        const val TAG = "LoginNoSitesErrorFragment"
        const val ARG_ERROR_MESSAGE = "ERROR-MESSAGE"

        fun newInstance(errorMsg: String): LoginNoSitesErrorFragment {
            val fragment = LoginNoSitesErrorFragment()
            val args = Bundle()
            args.putString(ARG_ERROR_MESSAGE, errorMsg)
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private var loginListener: LoginListener? = null
    private var errorMsg: String? = null
    private lateinit var viewModel: LoginNoSitesErrorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            errorMsg = it.getString(ARG_ERROR_MESSAGE, null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initBackPressHandler()
        with(FragmentLoginNoSitesErrorBinding.bind(view)) {
            initErrorMessageView()
            initViewModel()
        }
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun FragmentLoginNoSitesErrorBinding.initErrorMessageView() {
        loginErrorMessageText.text = errorMsg
    }

    private fun FragmentLoginNoSitesErrorBinding.initViewModel() {
        viewModel = ViewModelProvider(this@LoginNoSitesErrorFragment, viewModelFactory)
                .get(LoginNoSitesErrorViewModel::class.java)

        initObservers()

        viewModel.start(requireActivity().application as WordPress)
    }

    private fun FragmentLoginNoSitesErrorBinding.initObservers() {
        viewModel.navigationEvents.observe(viewLifecycleOwner, { events ->
            events.getContentIfNotHandled()?.let {
                when (it) {
                    is ShowSignInForResultJetpackOnly -> showSignInForResultJetpackOnly()
                    else -> { // no op
                    }
                }
            }
        })
    }

    private fun showSignInForResultJetpackOnly() {
        ActivityLauncher.showSignInForResultJetpackOnly(requireActivity(), true)
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
        viewModel.onFragmentResume()
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
