package org.wordpress.android.ui.main.jetpack.staticposter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.main.jetpack.staticposter.compose.JetpackStaticPoster
import org.wordpress.android.util.AppLog

@AndroidEntryPoint
class JetpackStaticPosterFragment : Fragment() {
    private val viewModel: JetpackStaticPosterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val uiState by viewModel.uiState.collectAsState()
                when (val state = uiState) {
                    is UiState.Content -> JetpackStaticPoster(
                        uiState = state,
                        onPrimaryClick = viewModel::onPrimaryClick,
                        onSecondaryClick = viewModel::onSecondaryClick,
                        onBackClick = requireActivity()::onBackPressed,
                    )
                    is UiState.Loading -> CircularProgressIndicator()
                }
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

    private fun handleEvents(event: Event) {
        when (event) {
            is Event.PrimaryButtonClick -> AppLog.d(AppLog.T.MAIN, "Primary button clicked")
            is Event.SecondaryButtonClick -> AppLog.d(AppLog.T.MAIN, "Secondary button clicked")
        }
    }

    companion object {
        private const val ARG_PARCEL = "ARG_PARCEL"

        fun newInstance(parcel: UiData) = JetpackStaticPosterFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_PARCEL, parcel)
            }
        }
    }
}
