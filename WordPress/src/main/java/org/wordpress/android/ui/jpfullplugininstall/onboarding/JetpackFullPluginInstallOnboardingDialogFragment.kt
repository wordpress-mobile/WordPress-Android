package org.wordpress.android.ui.jpfullplugininstall.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import org.wordpress.android.ui.compose.theme.AppTheme

class JetpackFullPluginInstallOnboardingDialogFragment : DialogFragment() {
//    private val viewModel: JetpackFullPluginInstallOnboardingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
//                JetpackFullPluginInstallOnboardingScreen()
            }
        }
    }
}
