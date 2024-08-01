package org.wordpress.android.ui.main.feedbackform

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.LocaleAwareActivity

@AndroidEntryPoint
class FeedbackFormActivity : LocaleAwareActivity() {
    private val viewModel by viewModels<FeedbackFormViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            ComposeView(this).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    FeedbackFormScreen(
                        messageText = viewModel.messageText.collectAsState(),
                        isProgressShowing = viewModel.isProgressShowing.collectAsState(),
                        onMessageChanged = {
                            viewModel.updateMessageText(it)
                        },
                        onSubmitClick = {
                            viewModel.onSubmitClick(this@FeedbackFormActivity)
                        },
                        onCloseClick = {
                            viewModel.onCloseClick(this@FeedbackFormActivity)
                        },
                        attachments = viewModel.attachments.collectAsState(),
                        onChooseMediaClick = {
                            viewModel.onChooseMediaClick(this@FeedbackFormActivity)
                        },
                        onRemoveMediaClick = {
                            viewModel.onRemoveMediaClick(it)
                        }
                    )
                }
            }
        )
    }
}
