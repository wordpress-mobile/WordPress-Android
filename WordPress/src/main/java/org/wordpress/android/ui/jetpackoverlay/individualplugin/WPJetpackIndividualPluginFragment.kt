package org.wordpress.android.ui.jetpackoverlay.individualplugin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginViewModel.ActionEvent
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginViewModel.UiState
import org.wordpress.android.ui.jetpackoverlay.individualplugin.compose.WPJetpackIndividualPluginOverlayScreen
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.util.extensions.setStatusBarAsSurfaceColor
import javax.inject.Inject

@AndroidEntryPoint
class WPJetpackIndividualPluginFragment : DialogFragment() {
    private val viewModel: WPJetpackIndividualPluginViewModel by viewModels()

    @Inject
    lateinit var activityLauncher: ActivityLauncherWrapper

    override fun getTheme(): Int {
        return R.style.WPJetpackIndividualPluginDialogFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val uiState by viewModel.uiState.collectAsState()
                when (val state = uiState) {
                    is UiState.Loaded -> {
                        WPJetpackIndividualPluginOverlayScreen(
                            state.sites,
                            onCloseClick = viewModel::onDismissScreenClick,
                            onPrimaryButtonClick = viewModel::onPrimaryButtonClick,
                            onSecondaryButtonClick = viewModel::onDismissScreenClick,
                        )
                    }

                    is UiState.None -> {}
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onScreenShown()
        observeActionEvents()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            (this as ComponentDialog).onBackPressedDispatcher
                .addCallback(this@WPJetpackIndividualPluginFragment) {
                    viewModel.onDismissScreenClick()
                    onBackPressedDispatcher.onBackPressedCompat(this)
                }
            setStatusBarAsSurfaceColor()
        }

    private fun observeActionEvents() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: ActionEvent) {
        when (actionEvent) {
            is ActionEvent.PrimaryButtonClick -> activityLauncher.openPlayStoreLink(
                requireActivity(),
                ActivityLauncherWrapper.JETPACK_PACKAGE_NAME
            )

            is ActionEvent.Dismiss -> dismiss()
        }.exhaustive
    }

    companion object {
        const val TAG = "WP_JETPACK_INDIVIDUAL_PLUGIN_FRAGMENT"

        @JvmStatic
        fun newInstance(): WPJetpackIndividualPluginFragment = WPJetpackIndividualPluginFragment()
    }
}
