package org.wordpress.android.ui.jetpackoverlay.individualplugin

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginViewModel.ActionEvent
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginViewModel.UiState
import org.wordpress.android.ui.jetpackoverlay.individualplugin.compose.WPJetpackIndividualPluginOverlayScreen
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.extensions.fillScreen
import javax.inject.Inject

@AndroidEntryPoint
class WPJetpackIndividualPluginFragment : BottomSheetDialogFragment() {
    private val viewModel: WPJetpackIndividualPluginViewModel by viewModels()

    @Inject
    lateinit var activityLauncher: ActivityLauncherWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeM2 {
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
            (this as? BottomSheetDialog)?.fillScreen()
        }

    override fun onCancel(dialog: DialogInterface) {
        // called when user hits the back button
        viewModel.onDismissScreenClick()
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: ActionEvent) {
        when (actionEvent) {
            is ActionEvent.PrimaryButtonClick -> activityLauncher.openPlayStoreLink(
                requireActivity(),
                ActivityLauncherWrapper.JETPACK_PACKAGE_NAME,
                ActivityLauncherWrapper.CAMPAIGN_INDIVIDUAL_PLUGIN
            )

            is ActionEvent.Dismiss -> dismiss()
        }.exhaustive
    }

    companion object {
        const val TAG = "WP_JETPACK_INDIVIDUAL_PLUGIN_FRAGMENT"

        @JvmStatic
        fun newInstance(): WPJetpackIndividualPluginFragment = WPJetpackIndividualPluginFragment()

        @JvmStatic
        fun show(fm: FragmentManager): WPJetpackIndividualPluginFragment = newInstance().also {
            it.show(fm, TAG)
        }
    }
}
