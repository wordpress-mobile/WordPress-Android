package org.wordpress.android.ui.main.jetpack.migration

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
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.StepUiState
import org.wordpress.android.ui.main.jetpack.migration.components.LoadingState
import org.wordpress.android.ui.main.jetpack.migration.components.UserAvatarImage
import org.wordpress.android.ui.main.jetpack.migration.components.WelcomeStep
import javax.inject.Inject

@AndroidEntryPoint
class JetpackMigrationFragment : Fragment() {
    @Inject lateinit var dispatcher: Dispatcher

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

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) return
        viewModel.onAccountInfoLoaded()
    }

    @Subscribe(threadMode = MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) return
        viewModel.onSiteListLoaded()
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
