package org.wordpress.android.ui.main.jetpack.staticposter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.CAMPAIGN_STATIC_POSTER
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.JETPACK_PACKAGE_NAME
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.jetpack.staticposter.compose.JetpackStaticPoster
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.extensions.getParcelableCompat
import javax.inject.Inject

@AndroidEntryPoint
class JetpackStaticPosterFragment : Fragment() {
    private val viewModel: JetpackStaticPosterViewModel by viewModels()

    @Inject
    lateinit var activityLauncher: ActivityLauncherWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState.collectAsState()
                when (val state = uiState) {
                    is UiState.Content -> JetpackStaticPoster(
                        uiState = state,
                        onPrimaryClick = viewModel::onPrimaryClick,
                        onSecondaryClick = viewModel::onSecondaryClick,
                        onBackClick = requireActivity().onBackPressedDispatcher::onBackPressed,
                    )
                    is UiState.Loading -> CircularProgressIndicator()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvents()
        viewModel.start(requireNotNull(requireArguments().getParcelableCompat(ARG_PARCEL)))
    }

    private fun observeEvents() {
        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Event.PrimaryButtonClick -> activityLauncher.openPlayStoreLink(
                    requireActivity(),
                    JETPACK_PACKAGE_NAME,
                    CAMPAIGN_STATIC_POSTER
                )
                is Event.SecondaryButtonClick -> event.url?.let {
                    WPWebViewActivity.openURL(requireContext(), UrlUtils.addUrlSchemeIfNeeded(it, true))
                }
            }
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
