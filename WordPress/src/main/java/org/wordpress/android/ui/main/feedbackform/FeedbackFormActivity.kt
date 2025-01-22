package org.wordpress.android.ui.main.feedbackform

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.main.BaseAppCompatActivity

@AndroidEntryPoint
class FeedbackFormActivity : BaseAppCompatActivity() {
    private val viewModel by viewModels<FeedbackFormViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val feedbackPrefix = intent.getStringExtra(EXTRA_FEEDBACK_PREFIX)
        viewModel.feedbackPrefix = feedbackPrefix

        setContentView(
            ComposeView(this).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    FeedbackFormScreen(
                        messageText = viewModel.messageText.collectAsState(),
                        progressDialogState = viewModel.progressDialogState.collectAsState(),
                        attachments = viewModel.attachments.collectAsState(),
                        onMessageChanged = {
                            viewModel.updateMessageText(it)
                        },
                        onSubmitClick = {
                            viewModel.onSubmitClick(this@FeedbackFormActivity)
                        },
                        onCloseClick = {
                            viewModel.onCloseClick(this@FeedbackFormActivity)
                        },
                        onChooseMediaClick = {
                            viewModel.onChooseMediaClick(this@FeedbackFormActivity)
                        },
                        onRemoveMediaClick = {
                            viewModel.onRemoveMediaClick(it)
                        },
                        onSupportClick = {
                            navigateToHelpScreen()
                        },
                    )
                }
            }
        )
    }

    private fun navigateToHelpScreen() {
        val intent = Intent(this, HelpActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCodes.PHOTO_PICKER) {
            data?.let {
                viewModel.onPhotoPickerResult(this, it)
            }
        }
    }

    companion object {
        const val EXTRA_FEEDBACK_PREFIX = "extra_feedback_prefix"
    }
}
