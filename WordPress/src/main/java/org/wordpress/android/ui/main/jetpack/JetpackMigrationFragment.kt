package org.wordpress.android.ui.main.jetpack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.main.jetpack.JetpackMigrationViewModel.StepUiState
import org.wordpress.android.ui.main.jetpack.components.UserAvatarImage
import org.wordpress.android.ui.main.jetpack.components.WelcomeStep

@AndroidEntryPoint
class JetpackMigrationFragment : Fragment() {
    private val viewModel: JetpackMigrationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                JetpackMigrationScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.start()
    }
}

@Composable
private fun JetpackMigrationScreen(viewModel: JetpackMigrationViewModel = viewModel()) {
    Box {
        Column {
            val uiState by viewModel.uiState.collectAsState()

            UserAvatarImage(avatarUrl = uiState.userAvatarUrl)

            when (val step = uiState.stepState) {
                is StepUiState.Welcome -> WelcomeStep(step)
                else -> LoadingState()
            }
        }
    }
}
