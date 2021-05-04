package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.FragmentLoginNoSitesEmptyViewBinding
import org.wordpress.android.login.LoginListener
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.login.LoginNoSitesErrorViewModel.State.NoUser
import org.wordpress.android.ui.accounts.login.LoginNoSitesErrorViewModel.State.ShowUser
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageType.USER
import javax.inject.Inject

@Suppress("TooManyFunctions")
class LoginNoSitesErrorFragment : Fragment(R.layout.fragment_login_no_sites_empty_view) {
    companion object {
        const val TAG = "LoginNoSitesErrorFragment"

        fun newInstance(): LoginNoSitesErrorFragment {
            return LoginNoSitesErrorFragment()
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    @Inject lateinit var uiHelpers: UiHelpers
    private var loginListener: LoginListener? = null
    private lateinit var viewModel: LoginNoSitesErrorViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initBackPressHandler()
        with(FragmentLoginNoSitesEmptyViewBinding.bind(view)) {
            initContentViews()
            initViewModel()
        }
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun FragmentLoginNoSitesEmptyViewBinding.initContentViews() {
        buttonPrimary.setOnClickListener { viewModel.onSeeInstructionsClicked() }
        buttonSecondary.setOnClickListener { viewModel.onTryAnotherAccountClicked() }
    }

    private fun FragmentLoginNoSitesEmptyViewBinding.initViewModel() {
        viewModel = ViewModelProvider(this@LoginNoSitesErrorFragment, viewModelFactory)
                .get(LoginNoSitesErrorViewModel::class.java)

        initObservers()

        viewModel.start(requireActivity().application as WordPress)
    }

    private fun FragmentLoginNoSitesEmptyViewBinding.initObservers() {
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

        viewModel.uiModel.observe(viewLifecycleOwner, { uiModel ->
            when (val state = uiModel.state) {
                is ShowUser -> {
                    loadGravatar(state.accountAvatarUrl)
                    setUserName(state.userName)
                    setDisplayName(state.displayName)
                    loginEmptyViewUserSection.visibility = View.VISIBLE
                }
                is NoUser -> loginEmptyViewUserSection.visibility = View.GONE
            }
        })
    }

    private fun FragmentLoginNoSitesEmptyViewBinding.loadGravatar(avatarUrl: String) =
            meGravatarLoader.load(
                    false,
                    meGravatarLoader.constructGravatarUrl(avatarUrl),
                    null,
                    imageAvatar,
                    USER,
                    null
            )

    private fun FragmentLoginNoSitesEmptyViewBinding.setUserName(value: String) =
            uiHelpers.setTextOrHide(textUsername, value)

    private fun FragmentLoginNoSitesEmptyViewBinding.setDisplayName(value: String) =
            uiHelpers.setTextOrHide(textDisplayname, value)

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
