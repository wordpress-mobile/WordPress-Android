package org.wordpress.android.ui.main.jetpack.move

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.main.jetpack.move.JetpackMoveViewModel.Event.Noop
import org.wordpress.android.ui.main.jetpack.move.JetpackMoveViewModel.UiState.Content
import org.wordpress.android.ui.main.jetpack.move.JetpackMoveViewModel.UiState.Loading

@AndroidEntryPoint
class JetpackMoveFragment : Fragment() {
    private val viewModel: JetpackMoveViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                JetpackMoveScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvents()
        viewModel.start(requireNotNull(requireArguments().getParcelable(ARG_PARCEL)))
    }

    private fun observeEvents() {
        viewModel.events.onEach(this::handleEvents).launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun handleEvents(event: JetpackMoveViewModel.Event) {
        when (event) {
            is Noop -> error("Unhandled event: $event")
        }
    }

    companion object {
        private const val ARG_PARCEL = "ARG_PARCEL"

        fun newInstance(parcel: JetpackMoveViewModel.Data) = JetpackMoveFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_PARCEL, parcel)
            }
        }
    }
}

@Composable
private fun JetpackMoveScreen(viewModel: JetpackMoveViewModel = viewModel()) {
    Box {
        val uiState by viewModel.uiState.collectAsState()

        when (val state = uiState) {
            is Content -> ContentScreen(state)
            is Loading -> LoadingScreen()
        }
    }
}

@Composable
fun ContentScreen(uiState: Content) = with(uiState) {
    Text(text = featureName)
}

@Composable
fun LoadingScreen() {
    CircularProgressIndicator()
}

@Preview()
@Composable
private fun JetpackMoveScreenPreview() {
    AppTheme {
        val vm = JetpackMoveViewModel()
        vm.start(JetpackMoveViewModel.StatsData)
        JetpackMoveScreen()
    }
}
