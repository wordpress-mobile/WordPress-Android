package org.wordpress.android.ui.accounts.login.jetpack

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.JetpackLoginEmptyViewBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesViewModel.State.NoUser
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesViewModel.State.ShowUser
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

@AndroidEntryPoint
class LoginNoSitesFragment : Fragment(R.layout.jetpack_login_empty_view) {
    companion object {
        const val TAG = "LoginNoSitesFragment"

        fun newInstance(): LoginNoSitesFragment {
            return LoginNoSitesFragment()
        }
    }

    @Inject
    lateinit var meGravatarLoader: MeGravatarLoader

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var uiHelpers: UiHelpers
    private val viewModel: LoginNoSitesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.onBackPressed()
        }
        with(JetpackLoginEmptyViewBinding.bind(view)) {
            initContentViews()
            initClickListeners()
            initViewModel(savedInstanceState)
        }
    }

    private fun JetpackLoginEmptyViewBinding.initContentViews() {
        uiHelpers.setTextOrHide(loginErrorMessageTitle, R.string.login_no_jetpack_sites)
        uiHelpers.setTextOrHide(loginErrorMessageText, R.string.login_no_jetpack_sites_error_message)
    }

    private fun JetpackLoginEmptyViewBinding.initClickListeners() {
        bottomButtonsContainer.buttonPrimary.setOnClickListener { viewModel.onSeeInstructionsPressed() }
        bottomButtonsContainer.buttonSecondary.setOnClickListener { viewModel.onTryAnotherAccountPressed() }
    }

    private fun JetpackLoginEmptyViewBinding.initViewModel(savedInstanceState: Bundle?) {
        initObservers()

        viewModel.start(
            application = requireActivity().application as WordPress,
            savedInstanceState = savedInstanceState
        )
    }

    private fun JetpackLoginEmptyViewBinding.initObservers() {
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
                    userCardView.visibility = View.VISIBLE
                }
                is NoUser -> userCardView.visibility = View.GONE
            }
        })
    }

    private fun JetpackLoginEmptyViewBinding.loadGravatar(avatarUrl: String) {
        imageManager.loadIntoCircle(
            userContainer.imageAvatar,
            ImageType.AVATAR_WITHOUT_BACKGROUND,
            meGravatarLoader.constructGravatarUrl(avatarUrl)
        )
    }

    private fun JetpackLoginEmptyViewBinding.setUserName(value: String) =
        uiHelpers.setTextOrHide(userContainer.textUsername, value)

    private fun JetpackLoginEmptyViewBinding.setDisplayName(value: String) =
        uiHelpers.setTextOrHide(userContainer.textDisplayName, value)

    private fun showSignInForResultJetpackOnly() {
        ActivityLauncher.showSignInForResultJetpackOnly(requireActivity())
    }

    private fun showInstructions(url: String) {
        ActivityLauncher.openUrlExternal(requireContext(), url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onFragmentResume()
    }
}
