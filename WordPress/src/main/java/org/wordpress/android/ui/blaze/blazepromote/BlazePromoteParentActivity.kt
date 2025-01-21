package org.wordpress.android.ui.blaze.blazepromote

import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.BlazeUIModel
import org.wordpress.android.ui.blaze.BlazeUiState
import org.wordpress.android.ui.blaze.blazeoverlay.BlazeOverlayFragment
import org.wordpress.android.ui.blaze.blazeoverlay.BlazeViewModel
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.extensions.getParcelableExtraCompat
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat

const val ARG_EXTRA_BLAZE_UI_MODEL = "blaze_ui_model"
const val ARG_BLAZE_FLOW_SOURCE = "blaze_flow_source"
const val ARG_BLAZE_SHOULD_SHOW_OVERLAY = "blaze_flow_should_show_overlay"

@AndroidEntryPoint
class BlazePromoteParentActivity : BaseAppCompatActivity() {
    private val viewModel: BlazeViewModel by viewModels()
    private var shouldShowOverlay = false
    private var source: BlazeFlowSource = BlazeFlowSource.DASHBOARD_CARD
    private var uiModel: BlazeUIModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blaze_parent)

        savedInstanceState?.let {
            shouldShowOverlay = it.getBoolean(ARG_BLAZE_SHOULD_SHOW_OVERLAY)
            source = requireNotNull(it.getSerializableCompat(ARG_BLAZE_FLOW_SOURCE))
            uiModel = it.getParcelableCompat(ARG_EXTRA_BLAZE_UI_MODEL)
        } ?: run {
            shouldShowOverlay = intent.getBooleanExtra(ARG_BLAZE_SHOULD_SHOW_OVERLAY, false)
            source = requireNotNull(intent.getSerializableExtraCompat(ARG_BLAZE_FLOW_SOURCE))
            uiModel = intent.getParcelableExtraCompat(ARG_EXTRA_BLAZE_UI_MODEL)
        }

        viewModel.start(source, uiModel, shouldShowOverlay)
        observe()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(ARG_BLAZE_FLOW_SOURCE, source)
        outState.putParcelable(ARG_EXTRA_BLAZE_UI_MODEL, uiModel)
        outState.putBoolean(ARG_BLAZE_SHOULD_SHOW_OVERLAY, shouldShowOverlay)
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
                    // without this, rotating the screen will show the overlay again
                    shouldShowOverlay = false
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
}
