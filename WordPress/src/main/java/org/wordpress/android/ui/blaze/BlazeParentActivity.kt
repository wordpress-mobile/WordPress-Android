package org.wordpress.android.ui.blaze

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.ui.blazeoverlay.BlazeOverlayFragment
import org.wordpress.android.ui.blaze.ui.blazeoverlay.BlazeViewModel

const val ARG_EXTRA_POST_ID = "post_id"
const val ARG_BLAZE_FLOW_SOURCE = "blaze_flow_source"

@AndroidEntryPoint
class BlazeParentActivity : AppCompatActivity() {
    private val viewModel: BlazeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blaze_parent)
        viewModel.start(getSource(), getPostModel())
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
                is BlazeUiState.Done -> {
                    finish()
                }
                else -> {}
            }
        }
    }

    private fun getSource(): BlazeFlowSource {
        return intent.getSerializableExtra(ARG_BLAZE_FLOW_SOURCE) as BlazeFlowSource
    }

    private fun getPostModel(): PostUIModel? {
        return intent.getParcelableExtra<PostUIModel>(ARG_EXTRA_POST_ID)
    }
}
