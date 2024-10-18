package org.wordpress.android.ui.debug.preferences

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class DebugSharedPreferenceFlagsActivity : AppCompatActivity() {
    private val viewModel: DebugSharedPreferenceFlagsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiStateFlow.collectAsState()
                DebugSharedPreferenceFlagsScreen(
                    flags = uiState,
                    onFlagChanged = viewModel::setFlag,
                    onBackTapped = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }
}
