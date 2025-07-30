package org.wordpress.android.ui.jetpackconnection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class JetpackConnectionActivity : BaseAppCompatActivity() {
    private val viewModel: JetpackConnectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackConnectionScreen(
                currentStep = viewModel.currentStep.collectAsState(),
                stepStatuses = viewModel.stepStatuses.collectAsState(),
                onCloseClick = viewModel::onCloseClick,
                showDoneButton = viewModel.showDoneButton.collectAsState()
            )
        }

        lifecycleScope.launch {
            viewModel.uiEvent.filterNotNull().collect { event ->
                when (event) {
                    JetpackConnectionViewModel.UiEvent.Close -> {
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context) =
            Intent(context, JetpackConnectionActivity::class.java)
    }
}
