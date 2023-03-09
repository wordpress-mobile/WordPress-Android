package org.wordpress.android.ui.blaze

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.ui.blazeoverlay.BlazeOverlayFragment
import org.wordpress.android.ui.blaze.ui.blazeoverlay.BlazeViewModel
import org.wordpress.android.ui.blaze.ui.blazewebview.BlazeWebViewFragment
import org.wordpress.android.util.extensions.getParcelableExtraCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat

const val ARG_EXTRA_BLAZE_UI_MODEL = "blaze_ui_model"
const val ARG_BLAZE_FLOW_SOURCE = "blaze_flow_source"

@AndroidEntryPoint
class BlazeParentActivity : AppCompatActivity() {
    private val viewModel: BlazeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blaze_parent)
        viewModel.start(getSource(), getBlazeUiModel())
        observe()
    }

    private fun observe() {
        viewModel.uiState.observe(this) { uiState ->
            when (uiState) {
                is BlazeUiState.PromoteScreen -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, BlazeOverlayFragment.newInstance())
                        .commitNow()
                }
                is BlazeUiState.WebViewScreen -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, BlazeWebViewFragment.newInstance())
                        .commitNow()
                }
                is BlazeUiState.Done -> {
                    finish()
                }
                else -> {}
            }
        }
    }

    private fun getSource(): BlazeFlowSource {
        return requireNotNull(intent.getSerializableExtraCompat(ARG_BLAZE_FLOW_SOURCE))
    }

    private fun getBlazeUiModel(): BlazeUIModel? {
        return intent.getParcelableExtraCompat(ARG_EXTRA_BLAZE_UI_MODEL)
    }
}
