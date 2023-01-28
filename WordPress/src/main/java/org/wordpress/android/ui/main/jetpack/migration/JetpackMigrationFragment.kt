package org.wordpress.android.ui.main.jetpack.migration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.HelpActivity.Origin.JETPACK_MIGRATION_HELP
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.LocaleAwareComposable
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.CompleteFlow
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.FallbackToLogin
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.FinishActivity
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.Logout
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.JetpackMigrationActionEvent.ShowHelp
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Error
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Loading
import org.wordpress.android.ui.main.jetpack.migration.compose.state.DeleteStep
import org.wordpress.android.ui.main.jetpack.migration.compose.state.DoneStep
import org.wordpress.android.ui.main.jetpack.migration.compose.state.ErrorStep
import org.wordpress.android.ui.main.jetpack.migration.compose.state.LoadingState
import org.wordpress.android.ui.main.jetpack.migration.compose.state.NotificationsStep
import org.wordpress.android.ui.main.jetpack.migration.compose.state.WelcomeStep
import org.wordpress.android.ui.utils.PreMigrationDeepLinkData
import org.wordpress.android.util.AppThemeUtils
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.extensions.getParcelableCompat
import javax.inject.Inject

@AndroidEntryPoint
class JetpackMigrationFragment : Fragment() {
    @Inject
    lateinit var dispatcher: Dispatcher

    private val viewModel: JetpackMigrationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val userLanguage by viewModel.refreshAppLanguage.observeAsState("")

                LocaleAwareComposable(
                    locale = LocaleManager.languageLocale(userLanguage),
                    onLocaleChange = viewModel::setAppLanguage
                ) {
                    JetpackMigrationScreen()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModelEvents()
        observeRefreshAppThemeEvents()
        val showDeleteWpState = arguments?.getBoolean(KEY_SHOW_DELETE_WP_STATE, false) ?: false
        val deepLinkData = arguments?.getParcelableCompat<PreMigrationDeepLinkData>(KEY_DEEP_LINK_DATA)
        initBackPressHandler(showDeleteWpState)
        viewModel.start(
            showDeleteWpState,
            requireActivity().application as WordPress,
            deepLinkData
        )
    }

    private fun observeViewModelEvents() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun observeRefreshAppThemeEvents() {
        viewModel.refreshAppTheme.observe(viewLifecycleOwner) {
            AppThemeUtils.setAppTheme(requireActivity())
        }
    }

    private fun handleActionEvents(actionEvent: JetpackMigrationActionEvent) {
        when (actionEvent) {
            is CompleteFlow -> {
                actionEvent.deepLinkData?.also {
                    ActivityLauncher.openDeepLinkAfterJPMigration(requireContext(), it.action, it.uri)
                } ?: ActivityLauncher.showMainActivity(requireContext())
            }
            is FallbackToLogin -> {
                actionEvent.deepLinkData?.let { (action, uri) ->
                    uri?.also {
                        ActivityLauncher.openJetpackForDeeplink(requireContext(), action, UriWrapper(it), true)
                    }
                } ?: ActivityLauncher.showMainActivity(requireContext(), true)
            }
            is Logout -> (requireActivity().application as? WordPress)?.let { viewModel.signOutWordPress(it) }
            is ShowHelp -> launchHelpScreen()
            is FinishActivity -> requireActivity().finish()
        }
    }

    private fun launchHelpScreen() {
        ActivityLauncher.viewHelpAndSupport(
            requireContext(),
            JETPACK_MIGRATION_HELP,
            null,
            null
        )
    }

    private fun initBackPressHandler(showDeleteWpState: Boolean) {
        if (showDeleteWpState) return
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(
                true
            ) {
                override fun handleOnBackPressed() {
                    viewModel.logoutAndFallbackToLogin()
                }
            })
    }

    companion object {
        private const val KEY_DEEP_LINK_DATA = "KEY_DEEP_LINK_DATA"
        private const val KEY_SHOW_DELETE_WP_STATE = "KEY_SHOW_DELETE_WP_STATE"

        fun newInstance(
            showDeleteWpState: Boolean = false,
            deepLinkData: PreMigrationDeepLinkData?
        ): JetpackMigrationFragment =
            JetpackMigrationFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(KEY_SHOW_DELETE_WP_STATE, showDeleteWpState)
                    if (deepLinkData != null) {
                        putParcelable(KEY_DEEP_LINK_DATA, deepLinkData)
                    }
                }
            }
    }
}

@Composable
private fun JetpackMigrationScreen(viewModel: JetpackMigrationViewModel = viewModel()) {
    Box {
        val uiState by viewModel.uiState.collectAsState(Loading)

        Crossfade(targetState = uiState) { state ->
            when (state) {
                is Content.Welcome -> WelcomeStep(state)
                is Content.Notifications -> NotificationsStep(state)
                is Content.Done -> DoneStep(state)
                is Content.Delete -> DeleteStep(state)
                is Error -> ErrorStep(state)
                is Loading -> LoadingState()
            }
        }
    }
}
