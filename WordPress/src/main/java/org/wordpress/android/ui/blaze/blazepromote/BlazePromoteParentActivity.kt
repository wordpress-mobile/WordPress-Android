package org.wordpress.android.ui.blaze.blazepromote

import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.BlazeUIModel
import org.wordpress.android.ui.blaze.BlazeUiState
import org.wordpress.android.ui.blaze.blazeoverlay.BlazeOverlayFragment
import org.wordpress.android.ui.blaze.blazeoverlay.BlazeViewModel
import org.wordpress.android.util.extensions.getParcelableExtraCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat

const val ARG_EXTRA_BLAZE_UI_MODEL = "blaze_ui_model"
const val ARG_BLAZE_FLOW_SOURCE = "blaze_flow_source"
const val ARG_BLAZE_SHOULD_SHOW_OVERLAY = "blaze_flow_should_show_overlay"

@AndroidEntryPoint
class BlazePromoteParentActivity : LocaleAwareActivity() {
    private val viewModel: BlazeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blaze_parent)
        viewModel.start(getSource(), getBlazeUiModel(), getShouldShowOverlay())
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
                        .replace(R.id.container, BlazePromoteWebViewFragment.newInstance())
                        .commitNow()
                }
                is BlazeUiState.Done -> {
                    finish()
                }
                else -> {}
            }
        }

        viewModel.onSelectedSiteMissing.observe(this) {
            finish()
        }
    }

    private fun getSource(): BlazeFlowSource {
        return requireNotNull(intent.getSerializableExtraCompat(ARG_BLAZE_FLOW_SOURCE))
    }

    private fun getBlazeUiModel(): BlazeUIModel? {
        return intent.getParcelableExtraCompat(ARG_EXTRA_BLAZE_UI_MODEL)
    }

    private fun getShouldShowOverlay(): Boolean = intent.getBooleanExtra(ARG_BLAZE_SHOULD_SHOW_OVERLAY, false)
}
