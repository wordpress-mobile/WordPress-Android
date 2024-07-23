package org.wordpress.android.ui.main.feedbackform

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.LocaleAwareActivity

@AndroidEntryPoint
class FeedbackFormActivity : LocaleAwareActivity() {
    private val viewModel by viewModels<FeedbackFormViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // TODO track screen shown?
        }

        setContentView(
            ComposeView(this).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    FeedbackFormScreen(
                        viewModel
                    )
                }
            }
        )
    }
}
