package org.wordpress.android.ui.main.jetpack.staticposter

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.JETPACK_PACKAGE_NAME
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.main.jetpack.staticposter.compose.JetpackStaticPoster
import org.wordpress.android.util.UrlUtils
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
            AppTheme {
                LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
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

    @Composable
    fun LockScreenOrientation(orientation: Int) {
        DisposableEffect(orientation) {
            requireActivity().requestedOrientation = orientation
            onDispose {
                // Although restore original orientation seems like the logical solution, it does not
                // work in this case because dispose runs after the new fragment is created, which
                // then just resets to the orientation that is started with. If we weren't using the
                // same activity (like we are for tabs), this would work very nicely by setting orientation
                // to user.
                // no op
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvents()
        viewModel.start(requireNotNull(requireArguments().getParcelable(ARG_PARCEL)))
    }

    private fun observeEvents() {
        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Event.PrimaryButtonClick -> activityLauncher.openPlayStoreLink(
                    requireActivity(),
                    JETPACK_PACKAGE_NAME
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
