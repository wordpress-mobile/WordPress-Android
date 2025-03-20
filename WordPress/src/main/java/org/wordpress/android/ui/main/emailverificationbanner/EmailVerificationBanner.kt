package org.wordpress.android.ui.main.emailverificationbanner

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView

/**
 * Custom view which hosts the email verification banner
 */
class EmailVerificationBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val composeView: ComposeView = ComposeView(context)

    init {
        addView(composeView)
    }

    fun setViewModel(viewModel: EmailVerificationViewModel) {
        composeView.setContent {
            EmailVerificationBanner(
                verificationState = viewModel.verificationState.collectAsState(),
                emailAddress = viewModel.emailAddress.collectAsState(),
                errorMessage = viewModel.errorMessage.collectAsState(),
                onSendLinkClick = {
                    viewModel.onSendVerificationLinkClick()
                }
            )
        }
    }
}
