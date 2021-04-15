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
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step
import javax.inject.Inject

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

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
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

        (requireActivity().application as WordPress).component().inject(this)

        val binding = FragmentLoginNoSitesErrorBinding.bind(view)

        binding.loginErrorMsg.text = errorMsg

        viewModel = ViewModelProvider(this@LoginNoSitesErrorFragment, viewModelFactory)
                .get(LoginNoSitesErrorViewModel::class.java)
        viewModel.signOutWordPress(requireActivity().application as WordPress)

        initBackPressHandler()
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

        unifiedLoginTracker.track(step = Step.NO_JETPACK_SITES)
    }

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(
                        true
                ) {
                    override fun handleOnBackPressed() {
                        ActivityLauncher.showSignInForResultJetpackOnly(requireActivity())
                    }
                })
    }
}
