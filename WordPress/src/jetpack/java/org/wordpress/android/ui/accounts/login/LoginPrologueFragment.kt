package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.JetpackLoginPrologueScreenBinding
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowEmailLoginScreen
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowLoginViaSiteAddressScreen
import org.wordpress.android.util.WPActivityUtils

@AndroidEntryPoint
class LoginPrologueFragment : Fragment(R.layout.jetpack_login_prologue_screen) {
    private val viewModel: LoginPrologueViewModel by viewModels()
    private lateinit var loginPrologueListener: LoginPrologueListener

    @Suppress("TooGenericExceptionThrown")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is LoginPrologueListener) {
            throw RuntimeException("$context must implement LoginPrologueListener")
        }
        loginPrologueListener = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setting up a full screen flags for the decor view of this fragment,
        // that will work with transparent status bar
        WPActivityUtils.showFullScreen(view)

        updateSystemBars(showDarkStatusAndNavBarInLightMode = true)

        with(JetpackLoginPrologueScreenBinding.bind(view)) { initViewModel() }
    }

    private fun JetpackLoginPrologueScreenBinding.initViewModel() {
        initObservers()
        viewModel.start()
    }

    private fun JetpackLoginPrologueScreenBinding.initObservers() {
        viewModel.navigationEvents.observe(viewLifecycleOwner, { events ->
            events.getContentIfNotHandled()?.let {
                when (it) {
                    is ShowEmailLoginScreen -> loginPrologueListener.showEmailLoginScreen()
                    is ShowLoginViaSiteAddressScreen -> loginPrologueListener.loginViaSiteAddress()
                    else -> Unit // Do nothing
                }
            }
        })

        viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
            updateButtonUiState(
                    bottomButtonsContainer.continueWithWpcomButton,
                    uiState.continueWithWpcomButtonState.title,
                    uiState.continueWithWpcomButtonState.onClick
            )
            updateButtonUiState(
                    bottomButtonsContainer.enterYourSiteAddressButton,
                    uiState.enterYourSiteAddressButtonState.title,
                    uiState.enterYourSiteAddressButtonState.onClick
            )
        })
    }

    private fun updateButtonUiState(button: MaterialButton, title: Int, onClick: () -> Unit) {
        button.setText(title)
        button.setOnClickListener { onClick.invoke() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onFragmentResume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // important for accessibility - talkback
        activity?.setTitle(R.string.login_prologue_screen_title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateSystemBars(showDarkStatusAndNavBarInLightMode = false)
    }

    private fun updateSystemBars(showDarkStatusAndNavBarInLightMode: Boolean) {
        activity?.let {
            WPActivityUtils.setLightStatusBar(it.window, !showDarkStatusAndNavBarInLightMode)
            WPActivityUtils.setLightNavigationBar(it.window, !showDarkStatusAndNavBarInLightMode)
        }
    }

    companion object {
        const val TAG = "jetpack_login_prologue_fragment_tag"
    }
}
