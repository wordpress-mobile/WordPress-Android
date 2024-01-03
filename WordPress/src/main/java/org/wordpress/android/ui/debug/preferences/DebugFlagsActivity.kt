package org.wordpress.android.ui.debug.preferences

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.domains.management.M3Theme
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class DebugFlagsActivity : AppCompatActivity() {
    private val viewModel: DebugFlagsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3Theme {
                val uiState by viewModel.uiStateFlow.collectAsState()
                DebugFlagsScreen(
                    flags = uiState,
                    onFlagChanged = viewModel::setFlag,
                    onBackTapped = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }
}
